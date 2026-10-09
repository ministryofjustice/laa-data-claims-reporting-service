package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import uk.gov.justice.laa.dstew.claimsreports.service.CsvFileValidator;

@ExtendWith(MockitoExtension.class)
class S3ClientWrapperTest {

  @Mock private S3Client s3Client;

  @Mock private MetricsHandler metricsHandler;

  @Mock private CsvFileValidator csvFileValidator;

  private Path testFilePath;
  private File testReport;
  private S3ClientWrapper s3ClientWrapper;

  @SneakyThrows
  @BeforeEach
  void setUpS3ClientWrapper() {
    testFilePath = Path.of(getClass().getClassLoader().getResource("testReport.csv").toURI());
    testReport = testFilePath.toFile();
    reset(csvFileValidator, metricsHandler, s3Client);
    s3ClientWrapper =
        new S3ClientWrapper(s3Client, "bucket", metricsHandler, csvFileValidator, false);
  }

  @SneakyThrows
  @Test
  void uploadFile_shouldUploadSuppliedCsvFile() {

    var mockResponse = PutObjectResponse.builder().build();

    when(csvFileValidator.checkFileExtension("testReport.csv", "reports/filename.csv"))
        .thenReturn(true);
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);
    s3ClientWrapper.uploadFile(testReport, "reports/filename.csv");

    // Check wrapper builds up the correct request to S3
    var captorPutObjectRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
    var captorRequestBody = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3Client).putObject(captorPutObjectRequest.capture(), captorRequestBody.capture());

    var requestToS3 = captorPutObjectRequest.getValue();
    assertEquals("bucket", requestToS3.bucket());
    assertEquals("reports/filename.csv", requestToS3.key());

    // Check the expected contents was sent up to S3
    var requestBody = captorRequestBody.getValue();
    assertEquals(Files.readString(testFilePath), getRequestBodyContents(requestBody));

    // Check metrics logged
    verify(metricsHandler).setCustomMetric(eq(CustomMetricId.ENCODING_CHECK_TIME_MS), anyDouble());
    verify(metricsHandler).setCustomMetric(eq(CustomMetricId.UPLOAD_TIME_MS), anyDouble());
  }

  @Test
  void uploadFile_shouldLetAwsExceptionBeCaughtByExceptionHandler() {
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(NoSuchBucketException.builder().build());

    assertThrows(
        NoSuchBucketException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));
  }

  @Test
  void uploadFile_shouldErrorIfFileExtensionCheckReturnsFalse() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.exe")).thenReturn(false);
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.exe"));
  }

  @Test
  void uploadFile_shouldErrorIfFileExtensionThrowsException() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.exe"))
        .thenThrow(new CsvUploadException(":("));
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.exe"));
  }

  @Test
  void uploadFile_shouldErrorIfMimeTypeIsCsvReturnsFalse() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(false);
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));
  }

  @Test
  void uploadFile_shouldErrorIfMimeTypeIsCsvThrowsException() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenThrow(new CsvUploadException(":("));
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.exe"));
  }

  @Test
  void uploadFile_shouldErrorIfUtf8CheckReturnsFalse() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(false);
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));
  }

  @SneakyThrows
  @Test
  void uploadFile_shouldUploadErrorFileToS3IfUtf8CheckReturnsFalseAndFeatureFlagOn() {
    s3ClientWrapper =
        new S3ClientWrapper(s3Client, "bucket", metricsHandler, csvFileValidator, true);

    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(false);

    var mockResponse = PutObjectResponse.builder().build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);

    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));

    // Check wrapper builds up the correct request to S3
    var captorPutObjectRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
    var captorRequestBody = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3Client).putObject(captorPutObjectRequest.capture(), captorRequestBody.capture());

    var requestToS3 = captorPutObjectRequest.getValue();
    assertEquals("bucket", requestToS3.bucket());
    assertEquals("reports/errors/testReport.csv", requestToS3.key());

    // Check the expected contents was sent up to S3
    var requestBody = captorRequestBody.getValue();
    assertEquals(Files.readString(testFilePath), getRequestBodyContents(requestBody));
  }

  @Test
  void uploadFile_shouldErrorIfUtf8CheckThrowsException() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenThrow(new CsvUploadException(":("));
    assertThrows(
        CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));
  }

  @Test
  void uploadFile_shouldUploadWithoutCsvValidationWhenContentTypeProvided() {
    var mockResponse = PutObjectResponse.builder().build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);

    s3ClientWrapper.uploadFile(
        testReport,
        "reports/daily/report_012.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    var captorPutObjectRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captorPutObjectRequest.capture(), any(RequestBody.class));
    assertEquals(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        captorPutObjectRequest.getValue().contentType());
    verify(csvFileValidator, never()).checkMimeTypeIsCsv(any(File.class));
    verify(csvFileValidator, never()).checkUtf8Encoded(any(File.class));
  }

  @Test
  void uploadFile_shouldNotUploadWhenHeadersDoNotMatch() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);
    when(csvFileValidator.checkCsvHeaders(testReport, List.of("Expected header")))
        .thenReturn(false);

    assertThrows(
        CsvUploadException.class,
        () ->
            s3ClientWrapper.uploadFile(
                testReport, "filename.csv", List.of("Expected header"), null));
    assertThrows(
        CsvUploadException.class,
        () ->
            s3ClientWrapper.uploadFile(
                testReport, "filename.csv", List.of("Expected header"), null));

    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @SneakyThrows
  private String getRequestBodyContents(RequestBody requestBody) {
    var outputStream = new ByteArrayOutputStream();
    requestBody.contentStreamProvider().newStream().transferTo(outputStream);
    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
