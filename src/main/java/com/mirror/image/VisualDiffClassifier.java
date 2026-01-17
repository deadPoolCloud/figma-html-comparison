package com.mirror.image;

import com.mirror.model.DiffRegion;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Classifies visual differences into meaningful categories for QA teams
 */
public class VisualDiffClassifier {

    /**
     * Classifies diff regions into specific issue types and generates observations
     */
    public static void classifyRegions(List<DiffRegion> regions, 
                                       BufferedImage figma, 
                                       BufferedImage live) {
        for (DiffRegion region : regions) {
            String issueType = classifyIssueType(region, figma, live);
            String observation = generateObservation(region, issueType, figma, live);
            
            region.setIssueType(issueType);
            region.setObservation(observation);
        }
    }

    /**
     * Classifies the type of visual issue based on region characteristics
     */
    private static String classifyIssueType(DiffRegion region, BufferedImage figma, BufferedImage live) {
        int width = region.getWidth();
        int height = region.getHeight();
        double aspectRatio = (double) width / height;
        
        // Very small regions are likely spacing issues
        if (region.getArea() < 100) {
            return "spacing";
        }
        
        // Long horizontal regions suggest spacing or alignment
        if (aspectRatio > 3.0 && height < 10) {
            return "alignment";
        }
        
        // Long vertical regions suggest alignment
        if (aspectRatio < 0.3 && width < 10) {
            return "alignment";
        }
        
        // Large rectangular regions could be missing/extra elements
        if (region.getArea() > 5000) {
            return "missing";
        }
        
        // Medium-sized regions often indicate color or font issues
        if (region.getArea() > 500 && region.getArea() < 5000) {
            // Check if it's text-like (horizontal rectangles)
            if (aspectRatio > 2.0 && height < 50) {
                return "font";
            }
            return "color";
        }
        
        return "other";
    }

    /**
     * Generates human-readable observation for QA teams
     */
    private static String generateObservation(DiffRegion region, 
                                             String issueType,
                                             BufferedImage figma, 
                                             BufferedImage live) {
        int x = region.getX();
        int y = region.getY();
        int width = region.getWidth();
        int height = region.getHeight();
        
        switch (issueType) {
            case "alignment":
                if (width > height) {
                    return String.format("Horizontal alignment issue detected around X:%d-Y:%d (width: %dpx)", 
                            x, y, width);
                } else {
                    return String.format("Vertical alignment issue detected around X:%d-Y:%d (height: %dpx)", 
                            x, y, height);
                }
                
            case "spacing":
                return String.format("Spacing mismatch detected at X:%d-Y:%d (area: %.0fpx²)", 
                        x, y, region.getArea());
                
            case "font":
                return String.format("Font size or style mismatch detected in text region X:%d-Y:%d (%dx%d)", 
                        x, y, width, height);
                
            case "color":
                return String.format("Color mismatch detected at X:%d-Y:%d (region: %dx%d)", 
                        x, y, width, height);
                
            case "missing":
                return String.format("Missing or extra UI element detected at X:%d-Y:%d (%dx%d)", 
                        x, y, width, height);
                
            default:
                return String.format("Visual difference detected at X:%d-Y:%d (%dx%d)", 
                        x, y, width, height);
        }
    }

    /**
     * Analyzes pixel differences to detect specific spacing issues
     */
    public static List<String> analyzeSpacingIssues(BufferedImage figma, BufferedImage live, List<DiffRegion> regions) {
        List<String> spacingObservations = new ArrayList<>();
        
        for (DiffRegion region : regions) {
            if ("spacing".equals(region.getIssueType())) {
                // Estimate spacing difference based on region size
                double spacingDiff = Math.sqrt(region.getArea());
                if (spacingDiff > 5) {
                    spacingObservations.add(String.format(
                            "Spacing differs by approximately %.0fpx at position X:%d-Y:%d", 
                            spacingDiff, region.getX(), region.getY()));
                }
            }
        }
        
        return spacingObservations;
    }
}
