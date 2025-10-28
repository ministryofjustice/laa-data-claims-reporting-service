package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
   * todo.
   *
   * @param filePath - todo.
   * @param fileName - todo.
   */
  public void uploadFile(String filePath, String fileName) {
    var putRequest = PutObjectRequest.builder()
        .bucket(s3Bucket)
        .key("reports/" + fileName)
        .build();

    log.info("Uploading!");

    s3Client.putObject(putRequest, RequestBody.fromFile(Paths.get(filePath)));

    log.info("Uploaded!");

  }


}
