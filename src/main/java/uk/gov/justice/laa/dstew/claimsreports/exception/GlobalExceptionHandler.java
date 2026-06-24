package uk.gov.justice.laa.dstew.claimsreports.exception;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * The global exception handler for all exceptions.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * The handler for ItemNotFoundException.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(ItemNotFoundException.class)
  public ResponseEntity<String> handleItemNotFound(ItemNotFoundException exception) {
    return ResponseEntity.status(NOT_FOUND).body(exception.getMessage());
  }

  /**
   * The handler for Exception.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGenericException(Exception exception) {
    String logMessage = "An unexpected application error has occurred.";
    log.error(logMessage, exception);
    return ResponseEntity.internalServerError().body(logMessage);
  }

  /**
   * Handles {@link AwsServiceException} and its subtypes, and responds with an HTTP 500 Internal Server Error.
   *
   * @param e the exception thrown when there is an issue connecting to S3.
   * @return a {@link ResponseEntity} with error message.
   */
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(AwsServiceException.class)
  public ResponseEntity<String> handleAwsErrors(AwsServiceException e) {
    var message = "Failed to upload report.";

    // Ensure log has specific AWS exception class name in, such as NoSuchKeyException.
    log.atError()
        .addKeyValue("event.action", "s3.upload.failure")
        .addKeyValue("event.type", "storage")
        .addKeyValue("event.outcome", "failure")
        .log("AwsServiceException ({}) Thrown: {}", sanitise(e.getClass().getSimpleName()), sanitise(e.awsErrorDetails().toString()));

    return ResponseEntity.internalServerError().body(message);
  }

  /**
   * Handles {@link CsvUploadException} and responds with an HTTP 500 Internal Server Error.
   *
   * @param e the exception thrown when there is an issue uploading file after generation.
   * @return a {@link ResponseEntity} with error message.
   */
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(CsvUploadException.class)
  public ResponseEntity<String> handleCsvUploadException(CsvUploadException e) {
    var message = "Failed to upload report.";

    log.atError()
        .addKeyValue("event.action", "csv.validation.failure")
        .addKeyValue("event.type", "batch")
        .addKeyValue("event.outcome", "failure")
        .setCause(e)
        .log("CsvUploadException thrown");

    return ResponseEntity.internalServerError().body(message);
  }


}
