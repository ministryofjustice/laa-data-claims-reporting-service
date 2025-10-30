package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;

/**
 * todo.
 */
public class S3FileUploader implements FileUploader {

  private final String awsRegion;
  private final String bucketName;

  public S3FileUploader(String awsRegion, String bucketName) {
    this.bucketName = bucketName;
    this.awsRegion = awsRegion;
  }

  @Override
  public void uploadFile(File fileToUpload, String fileName) {
    var s3Client = new S3ClientWrapper(awsRegion, bucketName);
    s3Client.uploadFile(fileToUpload, fileName);
  }
}
