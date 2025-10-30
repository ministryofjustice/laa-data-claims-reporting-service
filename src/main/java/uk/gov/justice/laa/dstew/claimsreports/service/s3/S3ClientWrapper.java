package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
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

  // TODO note this method of upload has a 5gb size limit

  /**
   * Upload a generated file to S3
   *
   * @param tempFile - todo.
   * @param fileName - todo.
   */
  public void uploadFile(File tempFile, String fileName) {

    if (!tempFile.getPath().endsWith(".csv") || !fileName.endsWith(".csv")){
      throw new CsvUploadException("Attempting to upload file that is not a CSV file: " + tempFile.getPath());
    }

    var putRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key("reports/" + fileName)
        .build();

    log.info("Uploading {} to S3 bucket {} with filename {}", tempFile.getPath(), s3Bucket, fileName);

    long startTime = System.currentTimeMillis();
    // Response to this request is just metadata, if it errors it will throw an AwsServiceException
    s3Client.putObject(putRequest, RequestBody.fromFile(tempFile));
    long endTime = System.currentTimeMillis();
    long durationMilliseconds = endTime - startTime;

    log.info("Uploaded {} to S3 bucket {} with filename {} in {} ms", tempFile.getPath(), s3Bucket, fileName, durationMilliseconds);
  }

}

