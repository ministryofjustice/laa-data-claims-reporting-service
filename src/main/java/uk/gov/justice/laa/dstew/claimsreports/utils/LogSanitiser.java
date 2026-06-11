package uk.gov.justice.laa.dstew.claimsreports.utils;

/**
 * Utility class for sanitising log message content.
 *
 * <p>Replaces carriage return ({@code \r}), newline ({@code \n}) and tab
 * ({@code \t}) characters with underscores to help prevent CRLF log
 * injection and log formatting issues.
 */
public final class LogSanitiser {
  private LogSanitiser() {}

  /**
   * Sanitises the supplied string for safe inclusion in log messages.
   *
   * <p>If the input is {@code null}, the string {@code "null"} is returned.
   * Otherwise, any carriage return, newline or tab characters are replaced
   * with underscores.
   *
   * @param input the string to sanitise
   * @return the sanitised string, or {@code "null"} if the input is {@code null}
   */
  public static String sanitise(String input) {
    if (input == null) {
      return "null";
    }
    return input.replaceAll("[\r\n\t]", "_");
  }
}
