package uk.gov.justice.laa.dstew.claimreports.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import uk.gov.justice.laa.dstew.claimsreports.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.dstew.claimsreports.exception.ItemNotFoundException;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleItemNotFound_returnsNotFoundStatusAndErrorMessage() {
    ResponseEntity<String> result = globalExceptionHandler.handleItemNotFound(new ItemNotFoundException("Item not found"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(NOT_FOUND);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("Item not found");
  }

  @Test
  void handleGenericException_returnsInternalServerErrorStatusAndErrorMessage() {
    ResponseEntity<String> result = globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("An unexpected application error has occurred.");
  }

  @Test
  void handleAwsServiceException_returnsInternalServerErrorStatusAndErrorMessage() {
    var exception = NoSuchBucketException.builder().message("Bucket don't exist :(")
        .awsErrorDetails(AwsErrorDetails.builder().errorCode("312").errorMessage("uh oh").build())
        .build();

    ResponseEntity<String> result = globalExceptionHandler.handleAwsErrors(exception);

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("Failed to upload report.");
  }

  @Test
  void handleCsvUploadException_returnsInternalServerErrorStatusAndErrorMessage() {
    ResponseEntity<String> result = globalExceptionHandler.handleCsvUploadException(new CsvUploadException("File is wrong type"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("Failed to upload report.");
  }

}
