package com.mirror.model;

import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of visual comparison between Figma design and live HTML page
 */
public class DiffResult {

    private BufferedImage figmaImage;
    private BufferedImage liveImage;
    private BufferedImage diffImage; // Highlighted diff image with red overlay
    private Mat diffMask;
    private double mismatchPercent;
    private List<DiffRegion> regions;
    private List<String> observations; // Human-readable observations
    private IssueSeverity severity;

    public DiffResult(BufferedImage figmaImage,
                      BufferedImage liveImage,
                      Mat diffMask,
                      double mismatchPercent,
                      List<DiffRegion> regions) {
        this.figmaImage = figmaImage;
        this.liveImage = liveImage;
        this.diffMask = diffMask;
        this.mismatchPercent = mismatchPercent;
        this.regions = regions;
        this.observations = new ArrayList<>();
        this.severity = IssueSeverity.fromPercentage(mismatchPercent);
    }

    public BufferedImage getFigmaImage() { return figmaImage; }
    public BufferedImage getLiveImage() { return liveImage; }
    public BufferedImage getDiffImage() { return diffImage; }
    public void setDiffImage(BufferedImage diffImage) { this.diffImage = diffImage; }
    public Mat getDiffMask() { return diffMask; }
    public double getMismatchPercent() { return mismatchPercent; }
    public List<DiffRegion> getRegions() { return regions; }
    public List<String> getObservations() { return observations; }
    public void addObservation(String observation) { this.observations.add(observation); }
    public IssueSeverity getSeverity() { return severity; }
    public void setSeverity(IssueSeverity severity) { this.severity = severity; }
}
