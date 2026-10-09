package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WalValidationServiceTest {

  private final WalValidationService walValidationService = new WalValidationService();

  @ParameterizedTest(name = "{0}")
  @MethodSource("validWalCases")
  void parsesValidWal(String name, String walLsn, long expected) {
    assertTrue(walValidationService.isValidWalLsn(walLsn));
    assertTrue(walValidationService.parseWalLsn(walLsn).isPresent());
    assertEquals(expected, walValidationService.parseWalLsn(walLsn).get());
  }

  private static Stream<Arguments> validWalCases() {
    return Stream.of(
        Arguments.of("low half only", "0/16B6C50", 23817296L),
        Arguments.of("both halves set", "1/16B6C50", 4318784592L),
        Arguments.of(
            "Crosses the unsigned int boundary", "80000000/00000000", -9223372036854775808L));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("walInvalidCases")
  void parsesValidWal(String name, String walLsn) {
    assertFalse(walValidationService.isValidWalLsn(walLsn));
    assertFalse(walValidationService.parseWalLsn(walLsn).isPresent());
  }

  private static Stream<Arguments> walInvalidCases() {
    return Stream.of(
        Arguments.of("No slash", "1"),
        Arguments.of("Too many slashes", "1/2/3"),
        Arguments.of("Empty high half", "/00000000"),
        Arguments.of("Empty low half", "00000000/"),
        Arguments.of("Non-hex high half", "G/00000000"),
        Arguments.of("Non-hex low half", "00000000/G"),
        Arguments.of("Overlong high half", "100000000/00000000"),
        Arguments.of("Overlong low half", "00000000/100000000"),
        Arguments.of("16-digit unsigned high half", "FFFFFFFFFFFFFFFF/00000000"),
        Arguments.of("16-digit unsigned low half", "00000000/FFFFFFFFFFFFFFFF"),
        Arguments.of("null", null),
        Arguments.of("empty string", ""));
  }
}
