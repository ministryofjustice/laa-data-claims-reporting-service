package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import java.util.List;

/** Graph response containing document libraries for site. */
record SharePointDriveList(List<SharePointDrive> value) {}
