package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import uk.gov.justice.laa.dstew.claimsreports.exception.SharePointUploadException;

@Slf4j
@Component
@RequiredArgsConstructor
class SharePointGraphClient {

  private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
  private static final ParameterizedTypeReference<SharePointDriveList> DRIVE_LIST_TYPE =
      new ParameterizedTypeReference<>() {};

  private final RestTemplate restTemplate;
  private final SharePointProperties properties;

  SharePointUploadResult uploadFile(File fileToUpload, String uploadFileName) {
    try {
      String accessToken = acquireAccessToken();
      SharePointSite site = resolveSite(accessToken);
      SharePointDrive drive = resolveDrive(accessToken, site.id());
      return upload(accessToken, site, drive, fileToUpload, uploadFileName);
    } catch (IOException | RestClientException ex) {
      throw new SharePointUploadException("Failed to upload file to SharePoint", ex);
    }
  }

  private String acquireAccessToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("grant_type", "client_credentials");
    requestBody.add("client_id", properties.getClientId());
    requestBody.add("client_secret", properties.getClientSecret());
    requestBody.add("scope", GRAPH_SCOPE);

    String tokenUrl =
        "https://login.microsoftonline.com/" + properties.getTenantId() + "/oauth2/v2.0/token";
    ResponseEntity<SharePointTokenResponse> response =
        restTemplate.postForEntity(
            tokenUrl, new HttpEntity<>(requestBody, headers), SharePointTokenResponse.class);
    SharePointTokenResponse body = response.getBody();
    if (body == null || body.accessToken() == null || body.accessToken().isBlank()) {
      throw new SharePointUploadException(
          "SharePoint token response did not contain an access token");
    }
    return body.accessToken();
  }

  private SharePointSite resolveSite(String accessToken) {
    HttpEntity<Void> requestEntity = new HttpEntity<>(authorisationHeaders(accessToken));
    String siteLookupPath = UriUtils.encodePath(properties.getSitePath(), StandardCharsets.UTF_8);
    String siteUrl =
        "https://graph.microsoft.com/v1.0/sites/" + properties.getSiteHost() + ":" + siteLookupPath;
    ResponseEntity<SharePointSite> response =
        restTemplate.exchange(siteUrl, HttpMethod.GET, requestEntity, SharePointSite.class);
    SharePointSite site = response.getBody();
    if (site == null || site.id() == null || site.id().isBlank()) {
      throw new SharePointUploadException("Unable to resolve SharePoint site ID");
    }
    return site;
  }

  private SharePointDrive resolveDrive(String accessToken, String siteId) {
    HttpEntity<Void> requestEntity = new HttpEntity<>(authorisationHeaders(accessToken));
    String driveUrl = "https://graph.microsoft.com/v1.0/sites/" + siteId + "/drives";
    ResponseEntity<SharePointDriveList> response =
        restTemplate.exchange(driveUrl, HttpMethod.GET, requestEntity, DRIVE_LIST_TYPE);
    SharePointDriveList driveList = response.getBody();
    if (driveList == null || driveList.value() == null) {
      throw new SharePointUploadException("SharePoint drives response was empty");
    }

    return driveList.value().stream()
        .filter(Objects::nonNull)
        .filter(drive -> properties.getDriveName().equals(drive.name()))
        .findFirst()
        .orElseThrow(
            () ->
                new SharePointUploadException(
                    "Unable to resolve SharePoint drive " + properties.getDriveName()));
  }

  private SharePointUploadResult upload(
      String accessToken,
      SharePointSite site,
      SharePointDrive drive,
      File fileToUpload,
      String uploadFileName)
      throws IOException {
    String uploadUrl = buildUploadUrl(site.id(), drive.id(), uploadFileName);

    log.atInfo()
        .addKeyValue("event.action", "sharepoint.upload")
        .addKeyValue("event.type", "storage")
        .addKeyValue("sharepoint.site_id", site.id())
        .addKeyValue("sharepoint.drive", drive.name())
        .addKeyValue("sharepoint.folder", properties.getFolderPath())
        .log(
            "Uploading {} to SharePoint site {}",
            sanitise(fileToUpload.getPath()),
            sanitise(site.webUrl()));

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.parseMediaType(SharePointProperties.XLSX_MIME_TYPE));

    HttpEntity<byte[]> requestEntity =
        new HttpEntity<>(Files.readAllBytes(fileToUpload.toPath()), headers);
    ResponseEntity<SharePointFileUploadResponse> response =
        restTemplate.exchange(
            uploadUrl, HttpMethod.PUT, requestEntity, SharePointFileUploadResponse.class);
    SharePointFileUploadResponse body = response.getBody();
    if (body == null || body.webUrl() == null || body.webUrl().isBlank()) {
      throw new SharePointUploadException("SharePoint upload succeeded without a webUrl");
    }
    return new SharePointUploadResult(body.webUrl());
  }

  private String buildUploadUrl(String siteId, String driveId, String uploadFileName) {
    String encodedFolder = UriUtils.encodePath(properties.getFolderPath(), StandardCharsets.UTF_8);
    String encodedFileName = UriUtils.encodePathSegment(uploadFileName, StandardCharsets.UTF_8);
    return "https://graph.microsoft.com/v1.0/sites/"
        + siteId
        + "/drives/"
        + driveId
        + "/root:/"
        + encodedFolder
        + "/"
        + encodedFileName
        + ":/content";
  }

  private HttpHeaders authorisationHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    return headers;
  }
}
