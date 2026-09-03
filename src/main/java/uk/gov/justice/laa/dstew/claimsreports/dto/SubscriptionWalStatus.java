package uk.gov.justice.laa.dstew.claimsreports.dto;

import java.time.Instant;

/**
 * Represents the status of the Write Ahead Log (WAL) for a subscription. This record is used to
 * encapsulate the current WAL-related progress and metrics of a subscription stream, providing
 * insights into replication state.
 *
 * <p>receivedLsn The Log Sequence Number (LSN) of the last WAL record that has been received from
 * the publisher by this subscription. latestEndLsn The Log Sequence Number (LSN) of the last
 * transaction that has been successfully applied by this subscription on the subscriber.
 * latestEndTime The timestamp at which the transaction identified by latestEndLsn was committed on
 * the publisher and applied on the subscriber.
 */
public record SubscriptionWalStatus(
    String receivedLsn, String latestEndLsn, Instant latestEndTime) {}
