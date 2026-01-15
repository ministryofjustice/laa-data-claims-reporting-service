package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

/**
 * Class that wraps around the default {@link S3Client}, allowing us to set default behaviours.
 */
@Slf4j
public class S3ClientWrapper {

  private final S3Client s3Client;
  private final String s3Bucket;

  public S3ClientWrapper(String awsRegion, String s3Bucket) {
    this.s3Client = new S3ClientFactory().createS3Client(awsRegion);
    this.s3Bucket = s3Bucket;
  }

  public S3ClientWrapper(S3Client s3Client, String s3Bucket) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
  }

  /**
   * Upload a generated file to the S3 bucket.
   * NOTE: This has a file size limit of 5GB. Above this we'd need to write a multi-part upload.
   *
   * @param fileToUpload - the CSV file we have just generated
   * @param desiredFileKey - the file key (folder + name) to use on S3.
   */
  public void uploadFile(File fileToUpload, String desiredFileKey) {

    String mimeType;
    try {
      mimeType = Files.probeContentType(fileToUpload.toPath());
    } catch (IOException e) {
      throw new CsvUploadException("Unable to determine MIME type for file: " + fileToUpload.getName(), e);
    }

    if (mimeType == null || !mimeType.equalsIgnoreCase("text/csv")
            || !fileToUpload.getPath().endsWith(".csv") || !desiredFileKey.endsWith(".csv")) {
      throw new CsvUploadException("Attempting to upload file that is not a CSV file. Detected MIME type: " + mimeType);
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

    log.info("Uploaded {} to S3 bucket {} with filename {} in {} ms", fileToUpload.getPath(), s3Bucket, desiredFileKey, durationMilliseconds);
  }

}

