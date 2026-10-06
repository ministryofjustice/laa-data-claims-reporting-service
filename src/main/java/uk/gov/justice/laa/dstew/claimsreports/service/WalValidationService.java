package uk.gov.justice.laa.dstew.claimsreports.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Service for validating and parsing WAL LSN (Write-Ahead Log Log Sequence Number) strings. */
@Service
public class WalValidationService {

  /**
   * Validates whether the provided WAL LSN string is in a valid format.
   *
   * @param walLsn the WAL LSN string to validate
   * @return true if the WAL LSN is valid, false otherwise
   */
  public boolean isValidWalLsn(String walLsn) {
    return parseWalLsn(walLsn).isPresent();
  }

  /**
   * Parses the provided WAL LSN string into a combined long value.
   *
   * @param walLsn the WAL LSN string to parse
   * @return an Optional containing the combined long value if parsing is successful, or an empty
   *     Optional if parsing fails
   */
  public Optional<Long> parseWalLsn(String walLsn) {
    if (walLsn == null || walLsn.isBlank()) {
      return Optional.empty();
    }

    // Valid WAL LSN can only have one slash
    if (StringUtils.countOccurrencesOf(walLsn, "/") != 1) {
      return Optional.empty();
    }

    if (!walLsn.matches("[0-9A-Fa-f]+/[0-9A-Fa-f]+")) {
      // Needs to be valid Hex
      return Optional.empty();
    }

    String[] walParts = walLsn.split("/", 2);

    long highValue;
    long lowValue;
    try {
      highValue = Long.parseUnsignedLong(walParts[0], 16);
      lowValue = Long.parseUnsignedLong(walParts[1], 16);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }

    if (Long.compareUnsigned(highValue, 0xFFFFFFFFL) > 0
        || Long.compareUnsigned(lowValue, 0xFFFFFFFFL) > 0) {
      return Optional.empty();
    }

    long walCombined = (highValue << 32) | (lowValue & 0xFFFFFFFFL);

    return Optional.of(walCombined);
  }
}
