package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Explicit configuration for SharePoint/Graph integration. */
@Component
public class SharePointProperties {

  public static final String XLSX_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String DEFAULT_SITE_HOST = "justiceuk.sharepoint.com";
  public static final String DEFAULT_SITE_PATH = "/sites/msteams_b565eb";
  public static final String DEFAULT_DRIVE_NAME = "Documents";
  public static final String DEFAULT_FOLDER_PATH = "General/Security/test";
  public static final int DEFAULT_UPLOAD_RETRY_COUNT = 3;

  @Value("${sharepoint.tenant-id:}")
  private String tenantId;

  @Value("${sharepoint.client-id:}")
  private String clientId;

  @Value("${sharepoint.client-secret:}")
  private String clientSecret;

  @Value("${sharepoint.site-host:" + DEFAULT_SITE_HOST + "}")
  private String siteHost;

  @Value("${sharepoint.site-path:" + DEFAULT_SITE_PATH + "}")
  private String sitePath;

  @Value("${sharepoint.drive-name:" + DEFAULT_DRIVE_NAME + "}")
  private String driveName;

  @Value("${sharepoint.folder-path:" + DEFAULT_FOLDER_PATH + "}")
  private String folderPath;

  @Value("${sharepoint.upload.retry-count:" + DEFAULT_UPLOAD_RETRY_COUNT + "}")
  private int uploadRetryCount;

  public String getTenantId() {
    return tenantId;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getSiteHost() {
    return siteHost;
  }

  public String getSitePath() {
    return sitePath;
  }

  public String getDriveName() {
    return driveName;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public int getUploadRetryCount() {
    return uploadRetryCount;
  }
}
