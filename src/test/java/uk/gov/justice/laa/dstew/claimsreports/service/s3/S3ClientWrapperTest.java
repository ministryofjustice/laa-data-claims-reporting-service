package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ClientWrapperTest {

  @Mock
  private S3Client s3Client;

  private final String testFilePath = getClass().getClassLoader().getResource("testReport.csv").getPath();
  private final File testReport = new File(testFilePath);
  private S3ClientWrapper s3ClientWrapper;

  @BeforeEach
  void beforeEach() {
    s3ClientWrapper = new S3ClientWrapper(s3Client, "bucket");
  }

  @SneakyThrows
  @Test
  void uploadFile_shouldUploadSuppliedCsvFile() {

    var mockResponse = PutObjectResponse.builder().build();

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(mockResponse);

    s3ClientWrapper.uploadFile(testReport, "filename.csv");

    // Check wrapper builds up the correct request to S3
    var captorPutObjectRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
    var captorRequestBody = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3Client).putObject(captorPutObjectRequest.capture(), captorRequestBody.capture());

    var requestToS3 = captorPutObjectRequest.getValue();
    assertEquals("bucket", requestToS3.bucket());
    assertEquals("reports/filename.csv", requestToS3.key());

    // Check the expected contents was sent up to S3
    var requestBody = captorRequestBody.getValue();
    assertEquals(Files.readString(Path.of(testFilePath)), getRequestBodyContents(requestBody));

  }

  @Test
  void uploadFile_shouldLetAwsExceptionBeCaughtByExceptionHandler() {

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(NoSuchBucketException.builder().build());

    assertThrows(NoSuchBucketException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.csv"));

  }

  @Test
  void uploadFile_shouldErrorIfTryingToSetFileNameToNotBeACsvFile() {
    assertThrows(CsvUploadException.class, () -> s3ClientWrapper.uploadFile(testReport, "filename.exe"));
  }

  @Test
  void uploadFile_shouldErrorIfTryingToUploadFileThatIsNotCsv() {
    var badFile = mock(File.class);
    when(badFile.getPath()).thenReturn("filename.exe");

    assertThrows(CsvUploadException.class, () -> s3ClientWrapper.uploadFile(badFile, "filename.csv"));
  }

  @SneakyThrows
  private String getRequestBodyContents(RequestBody requestBody) {
    var outputStream = new ByteArrayOutputStream();
    requestBody.contentStreamProvider().newStream().transferTo(outputStream);
    return outputStream.toString();
  }

}