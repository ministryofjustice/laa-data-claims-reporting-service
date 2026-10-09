package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import uk.gov.justice.laa.dstew.claimsreports.service.CsvFileValidator;

/** Class that wraps around the default {@link S3Client}, allowing us to set default behaviours. */
@Slf4j
public class S3ClientWrapper {

  private final S3Client s3Client;
  private final String s3Bucket;
  private final MetricsHandler metricsHandler;
  private final CsvFileValidator csvFileValidator;
  private final Boolean uploadUtf8FailuresToS3;

  /**
   * Create S3ClientWrapper based on AWS region.
   *
   * @param awsRegion region the S3 is in
   * @param s3Bucket Bucket name
   * @param metricsHandler Prometheus metric handler
   * @param csvFileValidator CSV file validation service
   */
  public S3ClientWrapper(
      String awsRegion,
      String s3Bucket,
      MetricsHandler metricsHandler,
      CsvFileValidator csvFileValidator,
      Boolean uploadUtf8FailuresToS3) {
    this.s3Client = new S3ClientFactory().createS3Client(awsRegion);
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
    this.csvFileValidator = csvFileValidator;
    this.uploadUtf8FailuresToS3 = uploadUtf8FailuresToS3;
  }

  /**
   * Create S3ClientWrapper based on pre-provided S3Client.
   *
   * @param s3Client s3Client
   * @param s3Bucket Bucket name
   * @param metricsHandler Prometheus metric handler
   * @param csvFileValidator CSV file validation service
   */
  public S3ClientWrapper(
      S3Client s3Client,
      String s3Bucket,
      MetricsHandler metricsHandler,
      CsvFileValidator csvFileValidator,
      Boolean uploadUtf8FailuresToS3) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
    this.csvFileValidator = csvFileValidator;
    this.uploadUtf8FailuresToS3 = uploadUtf8FailuresToS3;
  }

  /**
   * Upload a generated file to the S3 bucket. NOTE: This has a file size limit of 5GB. Above this
   * we'd need to write a multi-part upload.
   *
   * @param fileToUpload - the CSV file we have just generated
   * @param desiredFileKey - the file key (folder + name) to use on S3.
   */
  public void uploadFile(File fileToUpload, String desiredFileKey) {
    uploadFile(fileToUpload, desiredFileKey, List.of(), null);
  }

  /**
   * Upload a generated CSV file after validating fixed headers and any additional patterned
   * headers.
   *
   * @param fileToUpload the CSV file we have just generated
   * @param desiredFileKey the file key to use on S3
   * @param expectedHeaders the expected fixed CSV headers in order
   * @param additionalHeaderPattern pattern that any additional CSV headers must match
   */
  public void uploadFile(
      File fileToUpload,
      String desiredFileKey,
      List<String> expectedHeaders,
      Pattern additionalHeaderPattern) {
    uploadFile(
        fileToUpload,
        desiredFileKey,
        expectedHeaders,
        additionalHeaderPattern,
        desiredFileKey.substring(desiredFileKey.lastIndexOf('/') + 1));
  }

  /** Uploads a validated CSV file, identifying its report in the S3 audit events. */
  public void uploadFile(
      File fileToUpload,
      String desiredFileKey,
      List<String> expectedHeaders,
      Pattern additionalHeaderPattern,
      String reportName) {
    String fileName = fileToUpload.getName();

    if (!csvFileValidator.checkMimeTypeIsCsv(fileToUpload)) {
      throw new CsvUploadException("Failed to check MIME type for file: " + fileName);
    }

    if (!csvFileValidator.checkFileExtension(fileName, desiredFileKey)) {
      throw new CsvUploadException(
          "Failed to check file extension is valid CSV for file "
              + fileName
              + " being uploaded to "
              + desiredFileKey);
    }

    log.atInfo()
        .addKeyValue("event.action", "csv.validation")
        .addKeyValue("event.type", "batch")
        .log("Checking {} is UTF-8 encoded", sanitise(fileName));
    long encodingCheckStart = System.currentTimeMillis();
    if (!csvFileValidator.checkUtf8Encoded(fileToUpload)) {
      log.atWarn()
          .addKeyValue("event.action", "csv.validation.failure")
          .addKeyValue("event.type", "end")
          .addKeyValue("event.outcome", "failure")
          .addKeyValue("report.name", sanitise(reportName))
          .addKeyValue("file.name", sanitise(fileName))
          .addKeyValue("error.code", "invalid_utf8")
          .log("UTF-8 validation failed for {}", sanitise(fileName));
      if (uploadUtf8FailuresToS3) {
        putFile(fileToUpload, "reports/errors/" + fileName, reportName);
      }
      throw new CsvUploadException("File '" + fileName + "' is not UTF-8 encoded");
    }

    if (expectedHeaders != null && !expectedHeaders.isEmpty()) {
      boolean headersValid =
          additionalHeaderPattern == null
              ? csvFileValidator.checkCsvHeaders(fileToUpload, expectedHeaders)
              : csvFileValidator.checkCsvHeaders(
                  fileToUpload, expectedHeaders, additionalHeaderPattern);
      if (!headersValid) {
        throw new CsvUploadException(
            "CSV headers do not match expected headers for file: " + fileName);
      }
    }
    long encodingDuration = System.currentTimeMillis() - encodingCheckStart;
    log.atInfo()
        .addKeyValue("event.action", "csv.validation")
        .addKeyValue("event.type", "batch")
        .addKeyValue("event.outcome", "success")
        .log("File {} is valid UTF-8. Check took {} ms", fileName, encodingDuration);
    metricsHandler.setCustomMetric(CustomMetricId.ENCODING_CHECK_TIME_MS, encodingDuration);

    long durationMilliseconds = putFile(fileToUpload, desiredFileKey, reportName);
    var fileSizeMib = fileToUpload.length() / 1024 / 1024;
    metricsHandler.setCustomMetric(CustomMetricId.UPLOAD_TIME_MS, durationMilliseconds);
    metricsHandler.setCustomMetric(CustomMetricId.REPORT_FILE_SIZE, fileSizeMib);
  }

  private long putFile(File fileToUpload, String key, String reportName) {
    long fileSize = fileToUpload.length();
    long startTime = System.nanoTime();
    log.atInfo()
        .addKeyValue("event.action", "s3.upload")
        .addKeyValue("event.type", "start")
        .addKeyValue("event.outcome", "unknown")
        .addKeyValue("s3.bucket", sanitise(s3Bucket))
        .addKeyValue("s3.key", sanitise(key))
        .addKeyValue("file.size", fileSize)
        .addKeyValue("report.name", sanitise(reportName))
        .log("S3 file upload started");

    try {
      var putRequest =
          PutObjectRequest.builder().bucket(s3Bucket).key(key).contentType("text/csv").build();
      s3Client.putObject(putRequest, RequestBody.fromFile(fileToUpload));
      long duration = System.nanoTime() - startTime;
      log.atInfo()
          .addKeyValue("event.action", "s3.upload")
          .addKeyValue("event.type", "creation")
          .addKeyValue("event.outcome", "success")
          .addKeyValue("s3.bucket", sanitise(s3Bucket))
          .addKeyValue("s3.key", sanitise(key))
          .addKeyValue("file.size", fileSize)
          .addKeyValue("file.size_mib", fileSize / 1024 / 1024)
          .addKeyValue("report.name", sanitise(reportName))
          .addKeyValue("event.duration", duration)
          .addKeyValue("upload.duration_ms", TimeUnit.NANOSECONDS.toMillis(duration))
          .log("S3 file upload succeeded");
      return TimeUnit.NANOSECONDS.toMillis(duration);
    } catch (RuntimeException exception) {
      long duration = System.nanoTime() - startTime;
      String reason = exception.getClass().getSimpleName();
      if (exception instanceof AwsServiceException awsException
          && awsException.awsErrorDetails() != null
          && awsException.awsErrorDetails().errorCode() != null) {
        reason = awsException.awsErrorDetails().errorCode();
      }
      log.atError()
          .addKeyValue("event.action", "s3.upload")
          .addKeyValue("event.type", "creation")
          .addKeyValue("event.outcome", "failure")
          .addKeyValue("s3.bucket", sanitise(s3Bucket))
          .addKeyValue("s3.key", sanitise(key))
          .addKeyValue("file.size", fileSize)
          .addKeyValue("report.name", sanitise(reportName))
          .addKeyValue("event.duration", duration)
          .addKeyValue("upload.duration_ms", TimeUnit.NANOSECONDS.toMillis(duration))
          .addKeyValue("error.type", exception.getClass().getName())
          .addKeyValue("error.code", sanitise(reason))
          .log("S3 file upload failed");
      throw new CsvUploadException("Failed to upload report.");
    }
  }
}
