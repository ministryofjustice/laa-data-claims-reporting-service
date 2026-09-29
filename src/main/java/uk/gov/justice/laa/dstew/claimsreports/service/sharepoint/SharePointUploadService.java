package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.exception.SharePointUploadException;

/** Retries and logs SharePoint uploads using Microsoft Graph. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharePointUploadService {

  private final SharePointGraphClient sharePointGraphClient;
  private final SharePointProperties sharePointProperties;

  public SharePointUploadResult uploadFile(File fileToUpload, String uploadFileName) {
    SharePointUploadException lastFailure = null;
    for (int attempt = 1; attempt <= sharePointProperties.getUploadRetryCount(); attempt++) {
      try {
        return sharePointGraphClient.uploadFile(fileToUpload, uploadFileName);
      } catch (SharePointUploadException ex) {
        lastFailure = ex;
        log.warn("SharePoint upload attempt {} failed for {}", attempt, uploadFileName, ex);
      }
    }

    throw new SharePointUploadException(
        "SharePoint upload failed after "
            + sharePointProperties.getUploadRetryCount()
            + " attempts for "
            + uploadFileName,
        lastFailure);
  }
}
