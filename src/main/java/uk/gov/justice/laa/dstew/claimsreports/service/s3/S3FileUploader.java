package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;

/**
 * Wrapper that tells systems with S3 enabled to use the S3 client to upload files.
 * c.f. to {@link LocalFileUploader}
 */
public class S3FileUploader implements FileUploader {

  private final S3ClientWrapper s3Client;

  public S3FileUploader(S3ClientWrapper s3ClientWrapper) {
    this.s3Client = s3ClientWrapper;
  }

  @Override
  public void uploadFile(File fileToUpload, String fileName) {
    s3Client.uploadFile(fileToUpload, fileName);
  }
}
