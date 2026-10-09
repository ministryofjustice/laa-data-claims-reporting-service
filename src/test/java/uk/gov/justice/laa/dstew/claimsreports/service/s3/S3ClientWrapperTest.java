package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
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
  private Logger logger;
  private ListAppender<ILoggingEvent> auditLogs;

  @SneakyThrows
  @BeforeEach
  void setUpS3ClientWrapper() {
    testFilePath = Path.of(getClass().getClassLoader().getResource("testReport.csv").toURI());
    testReport = testFilePath.toFile();
    reset(csvFileValidator, metricsHandler, s3Client);
    s3ClientWrapper =
        new S3ClientWrapper(s3Client, "bucket", metricsHandler, csvFileValidator, false);
    logger = (Logger) LoggerFactory.getLogger(S3ClientWrapper.class);
    auditLogs = new ListAppender<>();
    auditLogs.start();
    logger.addAppender(auditLogs);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(auditLogs);
    auditLogs.stop();
    MDC.clear();
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
  void uploadFile_shouldReturnGenericErrorForAwsFailure() {
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(NoSuchBucketException.builder().build());

    var exception =
        assertThrows(
            CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));
    assertEquals("Failed to upload report.", exception.getMessage());
    assertAuditEvents("filename.csv", "failure");
  }

  @Test
  void uploadFile_shouldAuditSuccessWithReportAndCorrelationMetadata() {
    MDC.put("run_id", "test-run");
    MDC.put("traceId", "test-trace");
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenAnswer(
            invocation -> {
              assertEquals(1, uploadEvents().size());
              assertEquals("start", fields(uploadEvents().getFirst()).get("event.type"));
              return PutObjectResponse.builder().build();
            });

    s3ClientWrapper.uploadFile(testReport, "filename.csv", List.of(), null, "REPORT000");

    assertAuditEvents("filename.csv", "success");
    for (var event : uploadEvents()) {
      assertEquals("REPORT000", fields(event).get("report.name"));
      assertEquals("test-run", event.getMDCPropertyMap().get("run_id"));
      assertEquals("test-trace", event.getMDCPropertyMap().get("traceId"));
    }
  }

  @Test
  void uploadFile_shouldAuditClientFailureWithoutSensitiveExceptionMessage() {
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(true);
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkClientException.create("sensitive-report-content secret-credential"));

    var exception =
        assertThrows(
            CsvUploadException.class,
            () ->
                s3ClientWrapper.uploadFile(
                    testReport, "filename.csv", List.of(), null, "REPORT000"));

    assertEquals("Failed to upload report.", exception.getMessage());
    assertEquals(null, exception.getCause());
    assertAuditEvents("filename.csv", "failure");
    assertEquals(
        SdkClientException.class.getName(), fields(uploadEvents().getLast()).get("error.type"));
    for (var event : uploadEvents()) {
      assertEquals(null, event.getThrowableProxy());
      assertTrue(!fields(event).toString().contains("secret-credential"));
      assertTrue(!event.getFormattedMessage().contains("sensitive-report-content"));
    }
  }

  @Test
  void uploadFile_shouldAuditFailedValidationFileWrite() {
    s3ClientWrapper =
        new S3ClientWrapper(s3Client, "bucket", metricsHandler, csvFileValidator, true);
    when(csvFileValidator.checkMimeTypeIsCsv(testReport)).thenReturn(true);
    when(csvFileValidator.checkFileExtension("testReport.csv", "filename.csv")).thenReturn(true);
    when(csvFileValidator.checkUtf8Encoded(testReport)).thenReturn(false);
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(
            NoSuchBucketException.builder()
                .statusCode(404)
                .requestId("aws-request")
                .awsErrorDetails(
                    AwsErrorDetails.builder()
                        .errorCode("NoSuchBucket")
                        .errorMessage("sensitive-report-content")
                        .build())
                .build());

    var exception =
        assertThrows(
            CsvUploadException.class,
            () ->
                s3ClientWrapper.uploadFile(
                    testReport, "filename.csv", List.of(), null, "REPORT000"));

    assertEquals("Failed to upload report.", exception.getMessage());
    assertAuditEvents("reports/errors/testReport.csv", "failure");
    var failure = fields(uploadEvents().getLast());
    assertEquals("NoSuchBucket", failure.get("error.code"));
    for (var event : uploadEvents()) {
      assertEquals("REPORT000", fields(event).get("report.name"));
    }
    assertTrue(!failure.toString().contains("sensitive-report-content"));
    verify(metricsHandler, never()).setCustomMetric(eq(CustomMetricId.UPLOAD_TIME_MS), anyDouble());
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
        CsvUploadException.class,
        () -> s3ClientWrapper.uploadFile(testReport, "filename.csv", List.of(), null, "REPORT000"));
    var failure = fields(auditLogs.list.getLast());
    assertEquals("csv.validation.failure", failure.get("event.action"));
    assertEquals("failure", failure.get("event.outcome"));
    assertEquals("invalid_utf8", failure.get("error.code"));
    assertEquals("REPORT000", failure.get("report.name"));
    assertEquals("testReport.csv", failure.get("file.name"));
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
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
        CsvUploadException.class,
        () -> s3ClientWrapper.uploadFile(testReport, "filename.csv", List.of(), null, "REPORT000"));

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
    assertAuditEvents("reports/errors/testReport.csv", "success");
    var validationFailure =
        auditLogs.list.stream()
            .filter(event -> "csv.validation.failure".equals(fields(event).get("event.action")))
            .findFirst()
            .orElseThrow();
    assertEquals("invalid_utf8", fields(validationFailure).get("error.code"));
    assertEquals("failure", fields(validationFailure).get("event.outcome"));
    assertEquals("REPORT000", fields(validationFailure).get("report.name"));
    assertTrue(
        auditLogs.list.indexOf(validationFailure)
            < auditLogs.list.indexOf(uploadEvents().getFirst()));
    for (var event : uploadEvents()) {
      assertEquals("REPORT000", fields(event).get("report.name"));
    }
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

    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @SneakyThrows
  private String getRequestBodyContents(RequestBody requestBody) {
    var outputStream = new ByteArrayOutputStream();
    requestBody.contentStreamProvider().newStream().transferTo(outputStream);
    return outputStream.toString(StandardCharsets.UTF_8);
  }

  private List<ILoggingEvent> uploadEvents() {
    return auditLogs.list.stream()
        .filter(event -> "s3.upload".equals(fields(event).get("event.action")))
        .toList();
  }

  private Map<String, Object> fields(ILoggingEvent event) {
    return event.getKeyValuePairs().stream()
        .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
  }

  private void assertAuditEvents(String key, String outcome) {
    var events = uploadEvents();
    assertEquals(2, events.size());
    var start = fields(events.getFirst());
    var end = fields(events.getLast());
    assertEquals("start", start.get("event.type"));
    assertEquals("unknown", start.get("event.outcome"));
    assertEquals("creation", end.get("event.type"));
    assertEquals(outcome, end.get("event.outcome"));
    for (var event : events) {
      var metadata = fields(event);
      assertEquals("bucket", metadata.get("s3.bucket"));
      assertEquals(key, metadata.get("s3.key"));
      assertEquals(testReport.length(), metadata.get("file.size"));
      assertTrue(metadata.containsKey("report.name"));
    }
    assertTrue((long) end.get("event.duration") >= 0);
    assertTrue((long) end.get("upload.duration_ms") >= 0);
  }
}
