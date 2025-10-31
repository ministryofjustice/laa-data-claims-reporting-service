package uk.gov.justice.laa.dstew.claimsreports.exception;

/**
 * The exception thrown when failure during upload of CSV file.
 */
public class CsvUploadException extends RuntimeException {

  /**
   * Constructor for CsvUploadException Exception.
   *
   * @param message the error message
   */
  public CsvUploadException(String message) {
    super(message);
  }
}
