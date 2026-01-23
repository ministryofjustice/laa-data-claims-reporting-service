package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.CustomReportMetric;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

/**
 * Class that wraps around the default {@link S3Client}, allowing us to set default behaviours.
 */
@Slf4j
public class S3ClientWrapper {

  private final S3Client s3Client;
  private final String s3Bucket;
  private final MetricsHandler metricsHandler;

  /**
   * Create S3ClientWrapper based on AWS region.
   *
   * @param awsRegion      region the S3 is in
   * @param s3Bucket       Bucket name
   * @param metricsHandler Prometheus metric handler
   */
  public S3ClientWrapper(String awsRegion, String s3Bucket, MetricsHandler metricsHandler) {
    this.s3Client = new S3ClientFactory().createS3Client(awsRegion);
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
  }

  /**
   * Create S3ClientWrapper based on pre-provided S3Client.
   *
   * @param s3Client       s3Client
   * @param s3Bucket       Bucket name
   * @param metricsHandler Prometheus metric handler
   */
  public S3ClientWrapper(S3Client s3Client, String s3Bucket, MetricsHandler metricsHandler) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
    this.metricsHandler = metricsHandler;
  }

  /**
   * Upload a generated file to the S3 bucket.
   * NOTE: This has a file size limit of 5GB. Above this we'd need to write a multi-part upload.
   *
   * @param fileToUpload   - the CSV file we have just generated
   * @param desiredFileKey - the file key (folder + name) to use on S3.
   */
  public void uploadFile(File fileToUpload, String desiredFileKey) {

    String fileName = fileToUpload.getName();
    String mimeType;
    try {
      mimeType = Files.probeContentType(fileToUpload.toPath());
    } catch (IOException e) {
      throw new CsvUploadException("Unable to determine MIME type for file: " + fileName, e);
    }

    // 1. MIME type check
    if (mimeType == null) {
      throw new CsvUploadException("Could not detect MIME type for file: " + fileName);
    }

    if (!mimeType.equalsIgnoreCase("text/csv")) {
      throw new CsvUploadException("File '" + fileName + "' has invalid MIME type: " + mimeType + ". Expected 'text/csv'.");
    }

    // 2. File extension check
    if (!fileName.toLowerCase().endsWith(".csv")) {
      throw new CsvUploadException("File '" + fileName + "' does not have a .csv extension.");
    }

    // 3. Desired key extension check
    if (!desiredFileKey.toLowerCase().endsWith(".csv")) {
      throw new CsvUploadException("Target key '" + desiredFileKey + "' must end with .csv.");
    }

    var putRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key(desiredFileKey)
        .contentType("text/csv")
        .build();

    log.info("Uploading {} to S3 bucket {} with filename {}", fileToUpload.getPath(), s3Bucket, desiredFileKey);

    long startTime = System.currentTimeMillis();
    // Response to this request is just metadata, if it errors it will throw an AwsServiceException
    s3Client.putObject(putRequest, RequestBody.fromFile(fileToUpload));
    long endTime = System.currentTimeMillis();
    long durationMilliseconds = endTime - startTime;

    // Using MiB as that is what system storage use and isn't user-facing.
    var fileSizeMib = fileToUpload.length() / 1024 / 1024;
    metricsHandler.setCustomMetric(CustomReportMetric.UPLOAD_TIME_MS, durationMilliseconds);
    metricsHandler.setCustomMetric(CustomReportMetric.REPORT_FILE_SIZE, fileSizeMib);
    log.info("Uploaded {} to S3 bucket {} with filename {} and size {} MiB in {} ms",
        fileToUpload.getPath(), s3Bucket, desiredFileKey, fileSizeMib, durationMilliseconds);
  }

}

