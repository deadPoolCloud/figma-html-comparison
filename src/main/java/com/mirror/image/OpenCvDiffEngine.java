package com.mirror.image;

import com.mirror.model.DiffRegion;
import com.mirror.model.DiffResult;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV-based image comparison engine for pixel-to-pixel analysis
 */
public class OpenCvDiffEngine implements VisualDiffEngine {

    private static final int PIXEL_DIFF_THRESHOLD = 30; // Threshold for pixel color difference

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    @Override
    public DiffResult compare(BufferedImage figma, BufferedImage live) {

        System.out.println("=== OPENCV COMPARISON ===");
        System.out.println("Input images:");
        System.out.println("  Figma: " + figma.getWidth() + " x " + figma.getHeight());
        System.out.println("  Live:  " + live.getWidth() + " x " + live.getHeight());

        // Align images to same dimensions first
        BufferedImage[] aligned = ImageAligner.alignBoth(figma, live);
        BufferedImage alignedFigma = aligned[0];
        BufferedImage alignedLive = aligned[1];

        // Convert to OpenCV Mat
        Mat img1 = ImageUtil.toMat(alignedFigma);
        Mat img2 = ImageUtil.toMat(alignedLive);

        System.out.println("After conversion to Mat:");
        System.out.println("  Mat1: " + img1.width() + " x " + img1.height() + " channels=" + img1.channels() + " type=" + img1.type());
        System.out.println("  Mat2: " + img2.width() + " x " + img2.height() + " channels=" + img2.channels() + " type=" + img2.type());

        // Ensure both images have the same type and channels
        if (img1.channels() != img2.channels()) {
            System.out.println("Converting to same number of channels...");
            if (img1.channels() == 4) {
                Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR);
            }
            if (img2.channels() == 4) {
                Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGRA2BGR);
            }
        }

        if (img1.type() != img2.type()) {
            System.out.println("Converting to same type...");
            img2.convertTo(img2, img1.type());
        }

        // Absolute difference
        Mat diff = new Mat();
        Core.absdiff(img1, img2, diff);

        // Convert to gray
        Mat gray = new Mat();
        Imgproc.cvtColor(diff, gray, Imgproc.COLOR_BGR2GRAY);

        // Threshold
        Mat thresh = new Mat();
        Imgproc.threshold(gray, thresh, PIXEL_DIFF_THRESHOLD, 255, Imgproc.THRESH_BINARY);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresh, contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double mismatchPixels = Core.countNonZero(thresh);
        double totalPixels = thresh.rows() * thresh.cols();
        double mismatchPercent = (mismatchPixels * 100.0) / totalPixels;

        System.out.println("Comparison results:");
        System.out.println("  Diff regions found: " + contours.size());
        System.out.println("  Mismatch percentage: " + String.format("%.2f%%", mismatchPercent));
        System.out.println("=========================");

        List<DiffRegion> regions = new ArrayList<>();

        for (MatOfPoint cnt : contours) {
            Rect r = Imgproc.boundingRect(cnt);
            double area = Imgproc.contourArea(cnt);
            double impact = mismatchPixels > 0 ? (area / mismatchPixels) * 100.0 : 0;

            regions.add(new DiffRegion(r.x, r.y, r.width, r.height, area, impact));
        }

        // Use aligned images for the result
        DiffResult result = new DiffResult(alignedFigma, alignedLive, thresh, mismatchPercent, regions);

        // Classify regions and generate observations
        VisualDiffClassifier.classifyRegions(regions, alignedFigma, alignedLive);

        // Generate highlighted diff image with red overlay
        BufferedImage diffImage = createHighlightedDiffImage(alignedLive, thresh);
        result.setDiffImage(diffImage);

        // Generate observations
        generateObservations(result, regions);

        // Clean up
        img1.release();
        img2.release();
        diff.release();
        gray.release();
        thresh.release();

        return result;
    }

    /**
     * Creates a highlighted diff image with red overlay on mismatched pixels
     */
    private BufferedImage createHighlightedDiffImage(BufferedImage baseImage, Mat diffMask) {
        BufferedImage highlighted = new BufferedImage(
                baseImage.getWidth(),
                baseImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = highlighted.createGraphics();
        g2d.drawImage(baseImage, 0, 0, null);

        // Draw red overlay on diff regions
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.RED);

        // Convert diff mask to BufferedImage and draw red overlay
        BufferedImage maskImage = ImageUtil.toBufferedImage(diffMask);
        for (int y = 0; y < maskImage.getHeight(); y++) {
            for (int x = 0; x < maskImage.getWidth(); x++) {
                int rgb = maskImage.getRGB(x, y);
                // If pixel is white in mask (difference detected), overlay red
                if ((rgb & 0xFFFFFF) > 0) {
                    g2d.fillRect(x, y, 1, 1);
                }
            }
        }

        g2d.dispose();
        return highlighted;
    }

    /**
     * Generates human-readable observations from diff regions
     */
    private void generateObservations(DiffResult result, List<DiffRegion> regions) {
        // Add spacing observations
        List<String> spacingIssues = VisualDiffClassifier.analyzeSpacingIssues(
                result.getFigmaImage(),
                result.getLiveImage(),
                regions
        );
        result.getObservations().addAll(spacingIssues);

        // Add top issues by impact
        regions.stream()
                .sorted((r1, r2) -> Double.compare(r2.getImpactPercent(), r1.getImpactPercent()))
                .limit(5)
                .forEach(region -> {
                    if (region.getObservation() != null) {
                        result.addObservation(region.getObservation());
                    }
                });
    }
}