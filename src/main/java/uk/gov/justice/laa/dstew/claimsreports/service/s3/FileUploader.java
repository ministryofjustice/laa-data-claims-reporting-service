package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;

/**
 * todo.
 */
public interface FileUploader {
  void uploadFile(File fileToUpload, String filename);
}

