package net.caramel.ayd.config;

public record ConfigIssue(String key, String value, Kind kind, String message) {
    public enum Kind { MALFORMED_IDENTIFIER, NOT_A_NUMBER, NON_FINITE, BELOW_MINIMUM, ABOVE_MAXIMUM }
}
