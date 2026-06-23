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
    return input == null
            ? null
            : input.replace('\r', '_')
            .replace('\n', '_');
  }

  /**
   * Sanitises a string for safe use as a filename component.
   *
   * <p>This method removes all characters except:
   * letters (A–Z, a–z), digits (0–9), dot (.), underscore (_), and hyphen (-).
   *
   * <p>If the input is {@code null}, an {@link IllegalArgumentException} is thrown.
   * If the resulting string is empty after sanitisation, an {@link IllegalArgumentException}
   * is also thrown, as it would not form a valid filename component.
   *
   * @param input the raw filename component to sanitise; must not be null
   * @return a sanitised string containing only safe filename characters
   * @throws IllegalArgumentException if the input is null or results in an empty string
   */
  public static String sanitiseForFilename(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Filename component cannot be null");
    }
    String cleaned = input.replaceAll("[^A-Za-z0-9._-]", "");
    if (cleaned.isEmpty()) {
      throw new IllegalArgumentException("Filename component invalid after sanitisation");
    }
    return cleaned;
  }
}
