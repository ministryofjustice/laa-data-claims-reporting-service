package uk.gov.justice.laa.dstew.claimsreports.utils;

/**
 * Utility class for sanitising strings for safe logging and filename usage.
 *
 * <p>Designed to reduce risks of log forging (CRLF injection) and ensure filenames contain only
 * safe, predictable characters.
 */
public final class LogSanitiser {

  private LogSanitiser() {}

  /**
   * Sanitises a string for safe inclusion in log messages.
   *
   * <p>Only carriage return ({@code \r}) and newline ({@code \n}) characters are replaced with
   * underscores to prevent log forging / fake log entries.
   *
   * <p>Tabs are intentionally preserved.
   *
   * @param input the string to sanitise
   * @return sanitised string, or {@code null} if input is null
   */
  public static String sanitise(String input) {
    if (input == null) {
      return null;
    }

    return input.replace('\r', '_').replace('\n', '_');
  }

  /**
   * Sanitises a string for safe use as a filename component.
   *
   * <p>Only the following characters are allowed:
   *
   * <ul>
   *   <li>A–Z
   *   <li>a–z
   *   <li>0–9
   *   <li>.
   *   <li>_
   *   <li>-
   * </ul>
   *
   * <p>All other characters are removed.
   *
   * @param input the raw filename component
   * @return sanitised filename-safe string
   * @throws IllegalArgumentException if input is null or sanitised result is empty
   */
  public static String sanitiseForFilename(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Filename component cannot be null");
    }

    StringBuilder sb = new StringBuilder(input.length());

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (isAllowedFilenameChar(c)) {
        sb.append(c);
      }
    }

    String cleaned = sb.toString();

    if (cleaned.isEmpty()) {
      throw new IllegalArgumentException("Filename component invalid after sanitisation");
    }

    return cleaned;
  }

  /** Checks whether a character is safe for filename usage. */
  private static boolean isAllowedFilenameChar(char c) {
    return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
  }
}
