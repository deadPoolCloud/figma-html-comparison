package com.mirror.model;

/**
 * Represents a region where visual differences are detected
 */
public class DiffRegion {

    private int x;
    private int y;
    private int width;
    private int height;
    private double area;
    private double impactPercent;
    private String issueType; // "alignment", "spacing", "font", "color", "missing", "extra"
    private String observation; // Human-readable description

    public DiffRegion(int x, int y, int width, int height,
                      double area, double impactPercent) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.area = area;
        this.impactPercent = impactPercent;
    }

    public DiffRegion(int x, int y, int width, int height,
                      double area, double impactPercent,
                      String issueType, String observation) {
        this(x, y, width, height, area, impactPercent);
        this.issueType = issueType;
        this.observation = observation;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getArea() { return area; }
    public double getImpactPercent() { return impactPercent; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
}
