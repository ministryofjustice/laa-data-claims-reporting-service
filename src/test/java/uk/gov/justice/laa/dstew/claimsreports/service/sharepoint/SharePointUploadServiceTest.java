package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.exception.SharePointUploadException;

class SharePointUploadServiceTest {

  private SharePointGraphClient sharePointGraphClient;
  private SharePointProperties sharePointProperties;
  private SharePointUploadService sharePointUploadService;

  @BeforeEach
  void setUp() {
    sharePointGraphClient = mock(SharePointGraphClient.class);
    sharePointProperties = mock(SharePointProperties.class);
    when(sharePointProperties.getUploadRetryCount()).thenReturn(3);
    sharePointUploadService =
        new SharePointUploadService(sharePointGraphClient, sharePointProperties);
  }

  @Test
  void shouldRetryAndSucceed() {
    when(sharePointGraphClient.uploadFile(any(File.class), eq("report.xlsx")))
        .thenThrow(new SharePointUploadException("first"))
        .thenThrow(new SharePointUploadException("second"))
        .thenReturn(new SharePointUploadResult("https://example"));

    sharePointUploadService.uploadFile(new File("report.xlsx"), "report.xlsx");

    verify(sharePointGraphClient, times(3)).uploadFile(any(File.class), eq("report.xlsx"));
  }

  @Test
  void shouldThrowAfterRetryLimitExceeded() {
    when(sharePointGraphClient.uploadFile(any(File.class), eq("report.xlsx")))
        .thenThrow(new SharePointUploadException("always fails"));

    assertThrows(
        SharePointUploadException.class,
        () -> sharePointUploadService.uploadFile(new File("report.xlsx"), "report.xlsx"));
  }
}
