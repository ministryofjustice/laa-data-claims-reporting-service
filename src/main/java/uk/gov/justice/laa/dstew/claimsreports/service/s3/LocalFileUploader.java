package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
import lombok.extern.slf4j.Slf4j;

/**
 * Locally we currently have no S3 mock setup etc.
 * For now, we just don't try and upload anywhere.
 * If we set up a mocked S3 for local, we can collapse these into one class instead of an interface + 2 classes
 */
@Slf4j
public class LocalFileUploader implements FileUploader {

  @Override
  public void uploadFile(File fileToUpload, String filename) {
    log.info("Not using an S3 enabled system");
  }
}
