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
}
