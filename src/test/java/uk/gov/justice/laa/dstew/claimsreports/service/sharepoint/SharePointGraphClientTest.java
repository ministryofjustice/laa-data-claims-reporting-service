package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.dstew.claimsreports.exception.SharePointUploadException;

class SharePointGraphClientTest {

  private RestTemplate restTemplate;
  private SharePointProperties properties;
  private SharePointGraphClient sharePointGraphClient;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    properties = mock(SharePointProperties.class);
    when(properties.getTenantId()).thenReturn("tenant-id");
    when(properties.getClientId()).thenReturn("client-id");
    when(properties.getClientSecret()).thenReturn("client-secret");
    when(properties.getSiteHost()).thenReturn("justiceuk.sharepoint.com");
    when(properties.getSitePath()).thenReturn("/sites/msteams_b565eb");
    when(properties.getDriveName()).thenReturn("Documents");
    when(properties.getFolderPath()).thenReturn("General/Security/test");
    sharePointGraphClient = new SharePointGraphClient(restTemplate, properties);
  }

  @Test
  void shouldResolveSiteDriveAndUploadFile() throws Exception {
    File tempFile = Files.createTempFile("sharepoint-client", ".xlsx").toFile();
    Files.writeString(tempFile.toPath(), "test");

    when(restTemplate.postForEntity(
            any(String.class), any(HttpEntity.class), eq(SharePointTokenResponse.class)))
        .thenReturn(ResponseEntity.ok(new SharePointTokenResponse("token")));
    when(restTemplate.exchange(
            eq(
                "https://graph.microsoft.com/v1.0/sites/justiceuk.sharepoint.com:/sites/msteams_b565eb"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(SharePointSite.class)))
        .thenReturn(
            ResponseEntity.ok(
                new SharePointSite(
                    "site-id",
                    "Test Site",
                    "https://justiceuk.sharepoint.com/sites/msteams_b565eb")));
    when(restTemplate.exchange(
            eq("https://graph.microsoft.com/v1.0/sites/site-id/drives"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(
            new ResponseEntity<>(
                new SharePointDriveList(
                    java.util.List.of(new SharePointDrive("drive-id", "Documents"))),
                HttpStatus.OK));
    when(restTemplate.exchange(
            eq(
                "https://graph.microsoft.com/v1.0/sites/site-id/drives/drive-id/root:/General/Security/test/report.xlsx:/content"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(SharePointFileUploadResponse.class)))
        .thenReturn(ResponseEntity.ok(new SharePointFileUploadResponse("https://example")));

    SharePointUploadResult result = sharePointGraphClient.uploadFile(tempFile, "report.xlsx");

    assertEquals("https://example", result.webUrl());
  }

  @Test
  void shouldThrowWhenDriveCannotBeResolved() {
    when(restTemplate.postForEntity(
            any(String.class), any(HttpEntity.class), eq(SharePointTokenResponse.class)))
        .thenReturn(ResponseEntity.ok(new SharePointTokenResponse("token")));
    when(restTemplate.exchange(
            eq(
                "https://graph.microsoft.com/v1.0/sites/justiceuk.sharepoint.com:/sites/msteams_b565eb"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(SharePointSite.class)))
        .thenReturn(
            ResponseEntity.ok(
                new SharePointSite(
                    "site-id",
                    "Test Site",
                    "https://justiceuk.sharepoint.com/sites/msteams_b565eb")));
    when(restTemplate.exchange(
            eq("https://graph.microsoft.com/v1.0/sites/site-id/drives"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(
            new ResponseEntity<>(
                new SharePointDriveList(java.util.List.of(new SharePointDrive("other", "Other"))),
                HttpStatus.OK));

    assertThrows(
        SharePointUploadException.class,
        () -> sharePointGraphClient.uploadFile(new File("report.xlsx"), "report.xlsx"));
  }
}
