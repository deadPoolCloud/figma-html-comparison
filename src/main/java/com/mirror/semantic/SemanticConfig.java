package com.mirror.semantic;

/**
 * Configurable thresholds for semantic comparison.
 */
public class SemanticConfig {

    // Pixel-level noise threshold (ignore differences below this)
    public int pixelNoiseThreshold = 3;

    // Breakpoint tolerance ratio (e.g. 0.05 = 5% difference in viewport dimensions)
    public double breakpointToleranceRatio = 0.05;

    // Typography tolerances
    public double fontSizeTolerance = 1.0;
    public double lineHeightTolerance = 2.0;
    public double letterSpacingTolerance = 0.2;

    // Spacing tolerances
    public double spacingTolerance = 8.0;

    // Color tolerances (Delta-E)
    public double colorDeltaEWarn = 2.0;
    public double colorDeltaEFail = 6.0;

    // Matching heuristics weights
    public double matchTextWeight = 1.0;
    public double matchTypeWeight = 0.5;
    public double matchSpatialWeight = 0.3;
    public double matchHierarchyWeight = 0.2;

    public static SemanticConfig DEFAULT = new SemanticConfig();
}
