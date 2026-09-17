package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

/** SharePoint site resolved from Microsoft Graph. */
record SharePointSite(String id, String displayName, String webUrl) {}
