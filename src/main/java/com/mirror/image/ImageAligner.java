package com.mirror.image;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageAligner {

    public static BufferedImage align(BufferedImage figma, BufferedImage live) {
        try {
            System.out.println("=== IMAGE ALIGNMENT ===");
            System.out.println("Before alignment:");
            System.out.println("  Figma: " + figma.getWidth() + " x " + figma.getHeight());
            System.out.println("  Live:  " + live.getWidth() + " x " + live.getHeight());

            // Get maximum dimensions
            int maxWidth = Math.max(figma.getWidth(), live.getWidth());
            int maxHeight = Math.max(figma.getHeight(), live.getHeight());

            System.out.println("Target dimensions: " + maxWidth + " x " + maxHeight);

            // Create aligned images with white background
            BufferedImage alignedFigma = createAlignedImage(figma, maxWidth, maxHeight);
            BufferedImage alignedLive = createAlignedImage(live, maxWidth, maxHeight);

            System.out.println("After alignment:");
            System.out.println("  Aligned Figma: " + alignedFigma.getWidth() + " x " + alignedFigma.getHeight());
            System.out.println("  Aligned Live:  " + alignedLive.getWidth() + " x " + alignedLive.getHeight());
            System.out.println("=======================");

            // Store both aligned images for comparison
            // Return the aligned live image (the compare method expects this)
            return alignedLive;

        } catch (Exception e) {
            throw new RuntimeException("Failed to align images", e);
        }
    }

    /**
     * Creates a new image with white background and centers the original image
     */
    private static BufferedImage createAlignedImage(BufferedImage original, int targetWidth, int targetHeight) {
        // Create new image with white background
        BufferedImage aligned = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = aligned.createGraphics();

        // Fill with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, targetWidth, targetHeight);

        // Draw original image at top-left corner
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return aligned;
    }

    /**
     * Align both images to the same dimensions
     * Returns array: [alignedFigma, alignedLive]
     */
    public static BufferedImage[] alignBoth(BufferedImage figma, BufferedImage live) {
        System.out.println("=== IMAGE ALIGNMENT ===");
        System.out.println("Before alignment:");
        System.out.println("  Figma: " + figma.getWidth() + " x " + figma.getHeight());
        System.out.println("  Live:  " + live.getWidth() + " x " + live.getHeight());

        // Get maximum dimensions
        int maxWidth = Math.max(figma.getWidth(), live.getWidth());
        int maxHeight = Math.max(figma.getHeight(), live.getHeight());

        System.out.println("Target dimensions: " + maxWidth + " x " + maxHeight);

        // Create aligned images with white background
        BufferedImage alignedFigma = createAlignedImage(figma, maxWidth, maxHeight);
        BufferedImage alignedLive = createAlignedImage(live, maxWidth, maxHeight);

        System.out.println("After alignment:");
        System.out.println("  Aligned Figma: " + alignedFigma.getWidth() + " x " + alignedFigma.getHeight());
        System.out.println("  Aligned Live:  " + alignedLive.getWidth() + " x " + alignedLive.getHeight());
        System.out.println("=======================");

        return new BufferedImage[]{alignedFigma, alignedLive};
    }
}