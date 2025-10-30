package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;

/**
 * Interface for uploading a file to S3.
 * This is an interface right now to handle local separately since we aren't mocking S3 buckets locally.
 */
public interface FileUploader {
  void uploadFile(File fileToUpload, String filename);
}

