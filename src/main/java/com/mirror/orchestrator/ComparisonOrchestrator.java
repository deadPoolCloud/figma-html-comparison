package com.mirror.orchestrator;

import com.mirror.capture.SeleniumCaptureService;
import com.mirror.capture.WebCaptureService;
import com.mirror.figma.FigmaService;
import com.mirror.figma.FigmaServiceMock;
import com.mirror.image.ImageAligner;
import com.mirror.image.OpenCvDiffEngine;
import com.mirror.image.VisualDiffEngine;
import com.mirror.model.DiffResult;
import com.mirror.model.Viewport;
import com.mirror.report.HtmlReportService;
import com.mirror.report.ReportService;

import java.awt.image.BufferedImage;

/**
 * Orchestrates the entire visual comparison workflow
 */
public class ComparisonOrchestrator {

    private final WebCaptureService webCapture = new SeleniumCaptureService();
    private final FigmaService figmaService = new FigmaServiceMock(); // CHANGED: Using mock to avoid API rate limit
    private final VisualDiffEngine diffEngine = new OpenCvDiffEngine();
    private final ReportService reportService = new HtmlReportService(); // Use HTML report

    /**
     * Compares Figma design with live HTML page
     * @param url Web URL to compare
     * @param figmaFile Figma File ID
     * @param figmaFrame Figma Node ID
     * @return DiffResult with comparison results
     */
    public DiffResult compare(String url, String figmaFile, String figmaFrame) {
        return compare(url, figmaFile, figmaFrame, Viewport.DESKTOP);
    }

    /**
     * Compares Figma design with live HTML page for a specific viewport
     * @param url Web URL to compare
     * @param figmaFile Figma File ID
     * @param figmaFrame Figma Node ID
     * @param viewport Viewport size (Desktop/Tablet/Mobile)
     * @return DiffResult with comparison results
     */
    public DiffResult compare(String url, String figmaFile, String figmaFrame, Viewport viewport) {

        System.out.println("Starting visual comparison...");
        System.out.println("  Web URL: " + url);
        System.out.println("  Figma File: " + figmaFile);
        System.out.println("  Figma Frame: " + figmaFrame);
        System.out.println("  Viewport: " + viewport.getName() + " (" + viewport.getDimensionString() + ")");

        // 1. Capture live website with specified viewport
        System.out.println("Capturing live website...");
        BufferedImage live = webCapture.capture(url, viewport);

        // 2. Fetch Figma frame
        System.out.println("Fetching Figma design...");
        BufferedImage figma = figmaService.getFrame(figmaFile, figmaFrame);

        // 3. Align sizes
        System.out.println("Aligning image sizes...");
        BufferedImage aligned = ImageAligner.align(figma, live);

        // 4. Compare images
        System.out.println("Comparing images pixel-to-pixel...");
        DiffResult result = diffEngine.compare(figma, aligned);

        // 5. Generate HTML report
        System.out.println("Generating HTML report...");
        reportService.generate(result);

        // Print summary
        System.out.println("\n=== Comparison Complete ===");
        System.out.println("Mismatch Percentage: " + String.format("%.2f%%", result.getMismatchPercent()));
        System.out.println("Severity: " + result.getSeverity().getLabel());
        System.out.println("Issue Regions: " + result.getRegions().size());
        System.out.println("Observations: " + result.getObservations().size());

        return result;
    }
}