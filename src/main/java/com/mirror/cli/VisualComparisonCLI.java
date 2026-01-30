package com.mirror.cli;

import com.mirror.model.SemanticComparisonResult;
import com.mirror.model.Viewport;
import com.mirror.orchestrator.ComparisonOrchestrator;

import java.util.Scanner;

/**
 * Command-Line Interface for Visual Regression Testing Tool
 * 
 * Usage:
 * java -cp ... com.mirror.cli.VisualComparisonCLI
 * 
 * Or provide arguments:
 * java -cp ... com.mirror.cli.VisualComparisonCLI <url> <figmaFileId>
 * <figmaNodeId> [viewport]
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
            Viewport viewport = args.length >= 4 ? Viewport.valueOf(args[3].toUpperCase()) : Viewport.DESKTOP;
            String mode = args.length >= 5 ? args[4] : "ALL";

            runComparison(url, figmaFile, figmaNode, viewport, mode);
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
            case "2":
                viewport = Viewport.TABLET;
                break;
            case "3":
                viewport = Viewport.MOBILE;
                break;
            default:
                viewport = Viewport.DESKTOP;
                break;
        }

        System.out.println("\nComparison Mode:");
        System.out.println("  1. Combined (Pixel + Semantic)");
        System.out.println("  2. Semantic Only (Fast, no Figma API)");
        System.out.print("Select mode [1-2, default: 1]: ");
        String modeChoice = scanner.nextLine().trim();
        String mode = "2".equals(modeChoice) ? "SEMANTIC" : "ALL";

        System.out.println();
        runComparison(url, figmaFile, figmaNode, viewport, mode);
    }

    private static void runComparison(String url, String figmaFile, String figmaNode, Viewport viewport) {
        runComparison(url, figmaFile, figmaNode, viewport, "ALL");
    }

    private static void runComparison(String url, String figmaFile, String figmaNode, Viewport viewport, String mode) {
        try {
            boolean includePixel = !"SEMANTIC".equalsIgnoreCase(mode);
            SemanticComparisonResult result = orchestrator.compareSemantic(url, figmaFile, figmaNode, viewport,
                    includePixel);

            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║                  📊 COMPARISON RESULTS                   ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println();
            if (includePixel) {
                System.out.println("✅ HTML Report generated successfully!");
                System.out.println("📁 Check the 'reports/' directory for the full report");
            } else {
                System.out.println("ℹ️  Pixel comparison skipped (Semantic Only)");
                printSemanticDetails(result);
            }
            System.out.println();
            System.out.println("Summary (semantic):");
            System.out.println("  Elements Audited:  " + result.getSummary().getTotalElementsAudited());
            System.out.println("  Detected Issues:   " + result.getSummary().getTotalIssues());
            System.out.println("  Overall Status:    " + result.getSummary().getSeverity());

        } catch (Exception e) {
            System.err.println("\n❌ Error during comparison: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printSemanticDetails(SemanticComparisonResult result) {
        if (result.getSummary().getTotalIssues() == 0) {
            System.out.println("\n✅ No semantic issues found! Design matches implementation.");
            return;
        }

        System.out.println("\n📝 Detailed Semantic Issues:");

        if (!result.getTextTypography().isEmpty()) {
            boolean hasIssues = false;
            for (SemanticComparisonResult.TypographyIssue issue : result.getTextTypography()) {
                if (issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.FAIL ||
                        issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.WARN) {
                    if (!hasIssues) {
                        System.out.println("\n🔤 Typography Issues:");
                        hasIssues = true;
                    }
                    System.out.printf("  - [%s] %s%n", issue.getSeverity(), issue.getElementId());
                    if (issue.getStatus() != null && issue.getStatus().equals("MISMATCH")) {
                        System.out.printf("    Expected: '%s'%n    Actual:   '%s'%n",
                                issue.getFigma().getText(), issue.getHtml().getText());
                    } else if (issue.getNotes() != null) {
                        System.out.printf("    %s%n", issue.getNotes());
                    }
                }
            }
        }

        if (!result.getElementSizes().isEmpty()) {
            boolean hasIssues = false;
            for (SemanticComparisonResult.ElementSizeIssue issue : result.getElementSizes()) {
                if (issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.FAIL ||
                        issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.WARN) {
                    if (!hasIssues) {
                        System.out.println("\n📏 Size Issues:");
                        hasIssues = true;
                    }
                    System.out.printf("  - [%s] %s (%s)%n", issue.getSeverity(), issue.getElementId(), issue.getRole());
                    System.out.printf("    Figma: %.0fx%.0f | HTML: %.0fx%.0f%n",
                            issue.getFigmaWidth(), issue.getFigmaHeight(),
                            issue.getHtmlWidth(), issue.getHtmlHeight());
                }
            }
        }

        if (!result.getColors().isEmpty()) {
            boolean hasIssues = false;
            for (SemanticComparisonResult.ColorIssue issue : result.getColors()) {
                if (issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.FAIL ||
                        issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.WARN) {
                    if (!hasIssues) {
                        System.out.println("\n🎨 Color Issues:");
                        hasIssues = true;
                    }
                    System.out.printf("  - [%s] %s: %s%n", issue.getSeverity(), issue.getElementId(), issue.getNotes());
                }
            }
        }

        if (!result.getAlignment().isEmpty()) {
            boolean hasIssues = false;
            for (SemanticComparisonResult.AlignmentIssue issue : result.getAlignment()) {
                if (issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.FAIL ||
                        issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.WARN) {
                    if (!hasIssues) {
                        System.out.println("\n📐 Alignment Issues:");
                        hasIssues = true;
                    }
                    System.out.printf("  - [%s] %s: %s (Offset: %.1fpx)%n",
                            issue.getSeverity(), issue.getElementId(), issue.getDescription(), issue.getOffsetPx());
                }
            }
        }

        if (!result.getSpacing().isEmpty()) {
            boolean hasIssues = false;
            for (SemanticComparisonResult.SpacingIssue issue : result.getSpacing()) {
                if (issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.FAIL ||
                        issue.getSeverity() == SemanticComparisonResult.SemanticSeverity.WARN) {
                    if (!hasIssues) {
                        System.out.println("\n↔ Spacing Issues:");
                        hasIssues = true;
                    }
                    System.out.printf("  - [%s] %s: %s (Diff: %.1fpx)%n",
                            issue.getSeverity(), issue.getBetween(), issue.getNotes(), issue.getDiffPx());
                }
            }
        }

        if (!result.getMissingElements().isEmpty()) {
            System.out.println("\n🚫 Missing Elements (In Figma but NOT in HTML):");
            for (SemanticComparisonResult.MissingElementIssue issue : result.getMissingElements()) {
                System.out.printf("  - [%s] %s (%s): '%s'%n",
                        issue.getSeverity(), issue.getElementId(), issue.getType(), issue.getText());
            }
        }

        if (!result.getExtraElements().isEmpty()) {
            System.out.println("\n➕ Extra Elements (In HTML but NOT in Figma):");
            for (SemanticComparisonResult.ExtraElementIssue issue : result.getExtraElements()) {
                System.out.printf("  - [%s] %s (%s): '%s'%n",
                        issue.getSeverity(), issue.getElementId(), issue.getTag(), issue.getText());
            }
        }

        System.out.println("\n---------------------------------------------------------");
    }
}
