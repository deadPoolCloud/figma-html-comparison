package com.mirror.model;

/**
 * Severity levels for visual regression issues
 */
public enum IssueSeverity {
    MINOR("Minor", "Small differences that may not affect user experience"),
    MAJOR("Major", "Noticeable differences that impact visual consistency"),
    CRITICAL("Critical", "Significant deviations that break design integrity");

    private final String label;
    private final String description;

    IssueSeverity(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines severity based on mismatch percentage
     */
    public static IssueSeverity fromPercentage(double mismatchPercent) {
        if (mismatchPercent < 1.0) {
            return MINOR;
        } else if (mismatchPercent < 5.0) {
            return MAJOR;
        } else {
            return CRITICAL;
        }
    }
}
