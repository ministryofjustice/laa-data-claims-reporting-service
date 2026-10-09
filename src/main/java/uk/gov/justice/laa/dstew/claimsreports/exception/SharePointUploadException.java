package uk.gov.justice.laa.dstew.claimsreports.exception;

/** Exception thrown when a SharePoint upload cannot be completed. */
public class SharePointUploadException extends RuntimeException {

  public SharePointUploadException(String message) {
    super(message);
  }

  public SharePointUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
