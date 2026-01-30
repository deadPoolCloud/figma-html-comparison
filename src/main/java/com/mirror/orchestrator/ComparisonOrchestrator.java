package com.mirror.orchestrator;

import com.mirror.capture.PlaywrightCaptureService;
import com.mirror.capture.WebCaptureService;
import com.mirror.figma.FigmaService;
import com.mirror.figma.FigmaServiceImpl;
import com.mirror.image.ImageAligner;
import com.mirror.image.OpenCvDiffEngine;
import com.mirror.image.VisualDiffEngine;
import com.mirror.model.DiffResult;
import com.mirror.model.SemanticComparisonResult;
import com.mirror.model.Viewport;
import com.mirror.report.HtmlReportService;
import com.mirror.report.JsonReportService;
import com.mirror.report.ReportService;
import com.mirror.semantic.FigmaSemanticExtractor;
import com.mirror.semantic.FigmaSemanticSnapshot;
import com.mirror.semantic.HtmlSemanticSnapshot;
import com.mirror.semantic.SemanticAnalyzer;

import java.awt.image.BufferedImage;

/**
 * Orchestrates the entire visual comparison workflow
 */
public class ComparisonOrchestrator {

    private final WebCaptureService webCapture = new PlaywrightCaptureService();
    private final FigmaService figmaService = new FigmaServiceImpl(); // CHANGED: Using real API to support dynamic file
                                                                      // IDs
    private final VisualDiffEngine diffEngine = new OpenCvDiffEngine();
    private final ReportService htmlReport = new HtmlReportService();
    private final ReportService jsonReport = new JsonReportService();
    private final FigmaSemanticExtractor figmaSemanticExtractor = new FigmaSemanticExtractor();
    private final SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();

    /**
     * Compares Figma design with live HTML page
     * 
     * @param url        Web URL to compare
     * @param figmaFile  Figma File ID
     * @param figmaFrame Figma Node ID
     * @return DiffResult with comparison results
     */
    public DiffResult compare(String url, String figmaFile, String figmaFrame) {
        return compare(url, figmaFile, figmaFrame, Viewport.DESKTOP);
    }

    /**
     * Compares Figma design with live HTML page for a specific viewport
     * 
     * @param url        Web URL to compare
     * @param figmaFile  Figma File ID
     * @param figmaFrame Figma Node ID
     * @param viewport   Viewport size (Desktop/Tablet/Mobile)
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

        // 5. Generate reports
        System.out.println("Generating reports...");
        htmlReport.generate(result);
        jsonReport.generate(result);

        // Print summary
        System.out.println("\n=== Comparison Complete ===");
        System.out.println("Mismatch Percentage: " + String.format("%.2f%%", result.getMismatchPercent()));
        System.out.println("Severity: " + result.getSeverity().getLabel());
        System.out.println("Issue Regions: " + result.getRegions().size());
        System.out.println("Observations: " + result.getObservations().size());

        return result;
    }

    /**
     * Performs semantic (non pixel-based) comparison and returns a JSON-ready
     * semantic result.
     * 
     * @param includePixelComparison If true, also runs visual pixel comparison.
     */
    public SemanticComparisonResult compareSemantic(String url, String figmaFile, String figmaFrame,
            Viewport viewport, boolean includePixelComparison) {

        if (includePixelComparison) {
            // Preserve existing behaviour: still run pixel comparison and HTML report
            compare(url, figmaFile, figmaFrame, viewport);
        } else {
            System.out.println("Skipping pixel-to-pixel comparison (Semantic Mode only).");
        }

        System.out.println("\nStarting semantic comparison (layout, typography, spacing)...");

        // 1. Capture semantic HTML snapshot
        HtmlSemanticSnapshot htmlSnapshot = webCapture.captureSemantic(url, viewport);

        // 2. Fetch semantic structure from Figma API
        // File figmaStructure = new File("figma_structure.json"); // REMOVED
        com.fasterxml.jackson.databind.JsonNode figmaJson = figmaService.getStructure(figmaFile, figmaFrame);
        FigmaSemanticSnapshot figmaSnapshot = figmaSemanticExtractor.extract(figmaJson);

        // 3. Analyze semantically
        SemanticComparisonResult semanticResult = semanticAnalyzer.analyze(figmaSnapshot, htmlSnapshot);

        System.out.println("Semantic comparison complete.");
        System.out.println("Total semantic issues: " + semanticResult.getSummary().getTotalIssues());
        System.out.println("Semantic severity: " + semanticResult.getSummary().getSeverity());

        // Generate reports
        htmlReport.generate(semanticResult);
        jsonReport.generate(semanticResult);

        return semanticResult;
    }

    /**
     * Legacy overload for backward compatibility (defaults to including pixel
     * comparison)
     */
    public SemanticComparisonResult compareSemantic(String url, String figmaFile, String figmaFrame,
            Viewport viewport) {
        return compareSemantic(url, figmaFile, figmaFrame, viewport, true);
    }
}