package uk.gov.justice.laa.dstew.claimsreports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3.timeout")
public record S3Timeouts(int connection, int socket, int apiCallAttempt, int totalApiCall) {}
