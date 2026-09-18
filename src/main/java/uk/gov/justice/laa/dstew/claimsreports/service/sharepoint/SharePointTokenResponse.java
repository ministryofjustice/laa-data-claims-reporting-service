package uk.gov.justice.laa.dstew.claimsreports.service.sharepoint;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OAuth token response payload returned by Microsoft identity platform. */
record SharePointTokenResponse(@JsonProperty("access_token") String accessToken) {}
