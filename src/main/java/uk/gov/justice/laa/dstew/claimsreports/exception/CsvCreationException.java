package uk.gov.justice.laa.dstew.claimsreports.exception;

/**
 * The exception thrown when failure during data processing for CSV file.
 */
public class CsvCreationException extends RuntimeException {

  /**
   * Constructor for CsvCreation Exception.
   *
   * @param message the error message
   */
  public CsvCreationException(String message) {
    super(message);
  }

  /**
   * Constructor for CsV Creation Exception that provides information on root cause of error.
   *
   * @param message new error message
   * @param cause original source of error
   */
  public CsvCreationException(String message, Throwable cause) {
    super(message, cause);
  }

}
