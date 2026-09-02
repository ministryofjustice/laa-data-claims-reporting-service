package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import uk.gov.justice.laa.dstew.claimsreports.service.CsvFileValidator;
import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

/**
 * Class that wraps around the default {@link S3Client}, allowing us to set default behaviours.
 */
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
   * @param awsRegion      region the S3 is in
   * @param s3Bucket       Bucket name
   * @param metricsHandler Prometheus metric handler
   * @param csvFileValidator CSV file validation service
   */
  public S3ClientWrapper(String awsRegion, String s3Bucket, MetricsHandler metricsHandler,
                         CsvFileValidator csvFileValidator, Boolean uploadUtf8FailuresToS3) {
    this.s3Client = new S3ClientFactory().createS3Client(awsRegion);
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
    this.csvFileValidator = csvFileValidator;
    this.uploadUtf8FailuresToS3 = uploadUtf8FailuresToS3;
  }

  /**
   * Create S3ClientWrapper based on pre-provided S3Client.
   *
   * @param s3Client       s3Client
   * @param s3Bucket       Bucket name
   * @param metricsHandler Prometheus metric handler
   * @param csvFileValidator CSV file validation service
   */
  public S3ClientWrapper(S3Client s3Client, String s3Bucket, MetricsHandler metricsHandler,
                         CsvFileValidator csvFileValidator, Boolean uploadUtf8FailuresToS3) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
    this.csvFileValidator = csvFileValidator;
    this.uploadUtf8FailuresToS3 = uploadUtf8FailuresToS3;
  }

  /**
   * Upload a generated file to the S3 bucket.
   * NOTE: This has a file size limit of 5GB. Above this we'd need to write a multi-part upload.
   *
   * @param fileToUpload   - the CSV file we have just generated
   * @param desiredFileKey - the file key (folder + name) to use on S3.
   */
  public void uploadFile(File fileToUpload, String desiredFileKey) {
    uploadFile(fileToUpload, desiredFileKey, List.of(), null);
  }

  public void uploadFile(File fileToUpload, String desiredFileKey, String contentType) {
    uploadFileToS3(fileToUpload, desiredFileKey, contentType);
  }

  /**
   * Upload a generated CSV file after validating fixed headers and any additional patterned headers.
   *
   * @param fileToUpload the CSV file we have just generated
   * @param desiredFileKey the file key to use on S3
   * @param expectedHeaders the expected fixed CSV headers in order
   * @param additionalHeaderPattern pattern that any additional CSV headers must match
   */
  public void uploadFile(File fileToUpload, String desiredFileKey, List<String> expectedHeaders,
                         Pattern additionalHeaderPattern) {
    String fileName = fileToUpload.getName();

    if (!csvFileValidator.checkMimeTypeIsCsv(fileToUpload)) {
      throw new CsvUploadException("Failed to check MIME type for file: " + fileName);
    }

    if (!csvFileValidator.checkFileExtension(fileName, desiredFileKey)) {
      throw new CsvUploadException("Failed to check file extension is valid CSV for file " + fileName + " being uploaded to " + desiredFileKey);
    }

    log.atInfo()
        .addKeyValue("event.action", "csv.validation")
        .addKeyValue("event.type", "batch")
        .log("Checking {} is UTF-8 encoded", sanitise(fileName));
    long encodingCheckStart = System.currentTimeMillis();
    if (!csvFileValidator.checkUtf8Encoded(fileToUpload)) {
      if (uploadUtf8FailuresToS3) {
        uploadErroredFile(fileToUpload, fileName);
      }
      throw new CsvUploadException("File '" + fileName + "' is not UTF-8 encoded");
    }

    if (expectedHeaders != null && !expectedHeaders.isEmpty()) {
      boolean headersValid = additionalHeaderPattern == null
          ? csvFileValidator.checkCsvHeaders(fileToUpload, expectedHeaders)
          : csvFileValidator.checkCsvHeaders(fileToUpload, expectedHeaders, additionalHeaderPattern);
      if (!headersValid) {
        throw new CsvUploadException("CSV headers do not match expected headers for file: " + fileName);
      }
    }
    long encodingDuration = System.currentTimeMillis() - encodingCheckStart;
    log.atInfo()
        .addKeyValue("event.action", "csv.validation")
        .addKeyValue("event.type", "batch")
        .addKeyValue("event.outcome", "success")
        .log("File {} is valid UTF-8. Check took {} ms", fileName, encodingDuration);
    metricsHandler.setCustomMetric(CustomMetricId.ENCODING_CHECK_TIME_MS, encodingDuration);

    uploadFileToS3(fileToUpload, desiredFileKey, "text/csv");
  }

  private void uploadErroredFile(File fileToUpload, String fileName) {
    log.atInfo()
        .addKeyValue("event.action", "s3.upload.error_file")
        .addKeyValue("event.type", "storage")
        .log("UTF-8 check failed and uploadUtf8Errors is enabled, attempting to upload to errors folder");
    var errorFileName = "reports/errors/" + fileName;

    var errorUpload = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key(errorFileName)
        .contentType("text/csv")
        .build();
    s3Client.putObject(errorUpload, RequestBody.fromFile(fileToUpload));
    log.atInfo()
        .addKeyValue("event.action", "s3.upload.error_file")
        .addKeyValue("event.type", "storage")
        .addKeyValue("s3.bucket", s3Bucket)
        .addKeyValue("s3.key", errorFileName)
        .log("Uploaded non-UTF-8 file {} to S3 bucket {} with filename {}",
            sanitise(fileToUpload.getPath()), sanitise(s3Bucket), sanitise(errorFileName));

  }

  private void uploadFileToS3(File fileToUpload, String desiredFileKey, String contentType) {
    var putRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key(desiredFileKey)
        .contentType(contentType)
        .build();

    log.atInfo()
        .addKeyValue("event.action", "s3.upload")
        .addKeyValue("event.type", "storage")
        .addKeyValue("s3.bucket", s3Bucket)
        .addKeyValue("s3.key", desiredFileKey)
        .log("Uploading {} to S3 bucket {} with filename {}", sanitise(fileToUpload.getPath()), sanitise(s3Bucket), sanitise(desiredFileKey));

    long startTime = System.currentTimeMillis();
    // Response to this request is just metadata, if it errors it will throw an AwsServiceException
    s3Client.putObject(putRequest, RequestBody.fromFile(fileToUpload));
    long endTime = System.currentTimeMillis();
    long durationMilliseconds = endTime - startTime;

    // Using MiB as that is what system storage use and isn't user-facing.
    var fileSizeMib = fileToUpload.length() / 1024 / 1024;
    metricsHandler.setCustomMetric(CustomMetricId.UPLOAD_TIME_MS, durationMilliseconds);
    metricsHandler.setCustomMetric(CustomMetricId.REPORT_FILE_SIZE, fileSizeMib);
    log.atInfo()
        .addKeyValue("event.action", "s3.upload")
        .addKeyValue("event.type", "storage")
        .addKeyValue("event.outcome", "success")
        .addKeyValue("s3.bucket", s3Bucket)
        .addKeyValue("s3.key", desiredFileKey)
        .addKeyValue("file.size_mib", fileSizeMib)
        .addKeyValue("upload.duration_ms", durationMilliseconds)
        .log("Uploaded {} to S3 bucket {} with filename {} and size {} MiB in {} ms",
            sanitise(fileToUpload.getPath()), sanitise(s3Bucket), sanitise(desiredFileKey), fileSizeMib, durationMilliseconds);
  }

}
