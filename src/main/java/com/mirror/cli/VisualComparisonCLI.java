package com.mirror.cli;

import com.mirror.model.SemanticComparisonResult;
import com.mirror.model.Viewport;
import com.mirror.orchestrator.ComparisonOrchestrator;

import java.util.Scanner;

/**
 * Command-Line Interface for Visual Regression Testing Tool
 * 
 * Usage:
 *   java -cp ... com.mirror.cli.VisualComparisonCLI
 * 
 * Or provide arguments:
 *   java -cp ... com.mirror.cli.VisualComparisonCLI <url> <figmaFileId> <figmaNodeId> [viewport]
 */
public class VisualComparisonCLI {

    private static final Scanner scanner = new Scanner(System.in);
    private static final ComparisonOrchestrator orchestrator = new ComparisonOrchestrator();

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   🎨 Figma HTML Visual Regression Testing Tool         ║");
        System.out.println("║   QA-Friendly Pixel-to-Pixel Comparison               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();

        if (args.length >= 3) {
            // Non-interactive mode with command-line arguments
            String url = args[0];
            String figmaFile = args[1];
            String figmaNode = args[2];
            Viewport viewport = args.length >= 4 ? 
                    Viewport.valueOf(args[3].toUpperCase()) : Viewport.DESKTOP;
            
            runComparison(url, figmaFile, figmaNode, viewport);
        } else {
            // Interactive mode
            runInteractiveMode();
        }
    }

    private static void runInteractiveMode() {
        System.out.println("Enter the following information:");
        System.out.println();

        // Get Web URL
        System.out.print("🌐 Web URL to compare: ");
        String url = scanner.nextLine().trim();
        if (url.isEmpty()) {
            System.err.println("Error: Web URL is required");
            return;
        }

        // Get Figma File ID
        System.out.print("📄 Figma File ID: ");
        String figmaFile = scanner.nextLine().trim();
        if (figmaFile.isEmpty()) {
            System.err.println("Error: Figma File ID is required");
            return;
        }

        // Get Figma Node ID
        System.out.print("🎯 Figma Node ID (Frame ID): ");
        String figmaNode = scanner.nextLine().trim();
        if (figmaNode.isEmpty()) {
            System.err.println("Error: Figma Node ID is required");
            return;
        }

        // Get Viewport
        System.out.println("\nViewport options:");
        System.out.println("  1. Desktop (1440x900)");
        System.out.println("  2. Tablet (768x1024)");
        System.out.println("  3. Mobile (375x667)");
        System.out.print("Select viewport [1-3, default: 1]: ");
        String viewportChoice = scanner.nextLine().trim();
        
        Viewport viewport;
        switch (viewportChoice) {
            case "2": viewport = Viewport.TABLET; break;
            case "3": viewport = Viewport.MOBILE; break;
            default: viewport = Viewport.DESKTOP; break;
        }

        System.out.println();
        runComparison(url, figmaFile, figmaNode, viewport);
    }

    private static void runComparison(String url, String figmaFile, String figmaNode, Viewport viewport) {
        try {
            SemanticComparisonResult result = orchestrator.compareSemantic(url, figmaFile, figmaNode, viewport);

            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║                  📊 COMPARISON RESULTS                   ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("✅ HTML Report generated successfully!");
            System.out.println("📁 Check the 'reports/' directory for the full report");
            System.out.println();
            System.out.println("Summary (semantic):");
            System.out.println("  Total issues: " + result.getSummary().getTotalIssues());
            System.out.println("  Severity: " + result.getSummary().getSeverity());
            
        } catch (Exception e) {
            System.err.println("\n❌ Error during comparison: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
