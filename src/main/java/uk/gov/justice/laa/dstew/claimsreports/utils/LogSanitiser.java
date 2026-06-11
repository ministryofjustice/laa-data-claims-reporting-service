package uk.gov.justice.laa.dstew.claimsreports.utils;

public final class LogSanitiser {
    private LogSanitiser() {}

    public static String sanitise(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\r\n\t]", "_");
    }
}
