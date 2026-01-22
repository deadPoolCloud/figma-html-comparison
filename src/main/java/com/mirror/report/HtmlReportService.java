package com.mirror.report;

import com.mirror.model.DiffRegion;
import com.mirror.model.DiffResult;
import com.mirror.model.IssueSeverity;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates QA-friendly HTML reports with visual comparisons and observations
 */
public class HtmlReportService implements ReportService {

    private static final String OUTPUT_DIR = "reports/";
    private static final String IMAGES_DIR = OUTPUT_DIR + "images/";

    @Override
    public void generate(DiffResult result) {
        try {
            // Create directories
            Files.createDirectories(Paths.get(IMAGES_DIR));

            // Save images
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String figmaPath = IMAGES_DIR + "figma_" + timestamp + ".png";
            String livePath = IMAGES_DIR + "live_" + timestamp + ".png";
            String diffPath = IMAGES_DIR + "diff_" + timestamp + ".png";

            ImageIO.write(result.getFigmaImage(), "PNG", new File(figmaPath));
            ImageIO.write(result.getLiveImage(), "PNG", new File(livePath));
            ImageIO.write(result.getDiffImage(), "PNG", new File(diffPath));

            // Generate HTML
            String htmlPath = OUTPUT_DIR + "report_" + timestamp + ".html";
            generateHtmlReport(result, htmlPath, 
                    "images/figma_" + timestamp + ".png",
                    "images/live_" + timestamp + ".png",
                    "images/diff_" + timestamp + ".png");

            System.out.println("HTML Report generated: " + htmlPath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }

    private void generateHtmlReport(DiffResult result, String filePath,
                                   String figmaImg, String liveImg, String diffImg) throws IOException {
        
        FileWriter writer = new FileWriter(filePath);
        writer.write(generateHtmlContent(result, figmaImg, liveImg, diffImg));
        writer.close();
    }

    private String generateHtmlContent(DiffResult result, String figmaImg, String liveImg, String diffImg) {
        double mismatchPercent = result.getMismatchPercent();
        IssueSeverity severity = result.getSeverity();
        String severityColor = getSeverityColor(severity);
        
        Map<String, Long> issueTypeCounts = countIssueTypes(result.getRegions());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Visual Regression Test Report</title>\n");
        html.append("    <style>\n");
        html.append(getCssStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        
        // Header
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>🎨 Visual Regression Test Report</h1>\n");
        html.append("            <div class=\"timestamp\">Generated: ").append(new Date()).append("</div>\n");
        html.append("        </div>\n");

        // Summary Card
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Mismatch Percentage</div>\n");
        html.append(String.format("                <div class=\"metric-value\" style=\"color: %s;\">%.2f%%</div>\n", 
                severityColor, mismatchPercent));
        html.append("            </div>\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Severity</div>\n");
        html.append(String.format("                <div class=\"metric-value\" style=\"color: %s;\">%s</div>\n", 
                severityColor, severity.getLabel()));
        html.append("            </div>\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Issue Regions</div>\n");
        html.append("                <div class=\"metric-value\">").append(result.getRegions().size()).append("</div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Issue Type Breakdown
        html.append("        <div class=\"section\">\n");
        html.append("            <h2>Issue Type Breakdown</h2>\n");
        html.append("            <div class=\"issue-types\">\n");
        for (Map.Entry<String, Long> entry : issueTypeCounts.entrySet()) {
            html.append(String.format("                <div class=\"issue-type-item\">\n"));
            html.append(String.format("                    <span class=\"issue-type-name\">%s</span>\n", 
                    capitalize(entry.getKey())));
            html.append(String.format("                    <span class=\"issue-type-count\">%d</span>\n", 
                    entry.getValue()));
            html.append("                </div>\n");
        }
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Images Comparison
        html.append("        <div class=\"section\">\n");
        html.append("            <h2>Visual Comparison</h2>\n");
        html.append("            <div class=\"image-comparison\">\n");
        html.append("                <div class=\"image-container\">\n");
        html.append("                    <h3>Figma Design (Expected)</h3>\n");
        html.append("                    <img src=\"").append(figmaImg).append("\" alt=\"Figma Design\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("                <div class=\"image-container\">\n");
        html.append("                    <h3>HTML Implementation (Actual)</h3>\n");
        html.append("                    <img src=\"").append(liveImg).append("\" alt=\"Live HTML\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("                <div class=\"image-container\">\n");
        html.append("                    <h3>Differences (Red Overlay)</h3>\n");
        html.append("                    <img src=\"").append(diffImg).append("\" alt=\"Diff\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Observations
        html.append("        <div class=\"section\">\n");
        html.append("            <h2>🔍 Human-Readable Observations</h2>\n");
        html.append("            <div class=\"observations\">\n");
        if (result.getObservations().isEmpty()) {
            html.append("                <div class=\"observation-item success\">✅ No significant visual differences detected!</div>\n");
        } else {
            for (String observation : result.getObservations()) {
                html.append("                <div class=\"observation-item\">").append(observation).append("</div>\n");
            }
        }
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Detailed Issue Regions
        if (!result.getRegions().isEmpty()) {
            html.append("        <div class=\"section\">\n");
            html.append("            <h2>📋 Detailed Issue Regions</h2>\n");
            html.append("            <table class=\"issues-table\">\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>Position (X, Y)</th>\n");
            html.append("                        <th>Size (W x H)</th>\n");
            html.append("                        <th>Issue Type</th>\n");
            html.append("                        <th>Observation</th>\n");
            html.append("                        <th>Impact %</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");
            
            for (DiffRegion region : result.getRegions()) {
                html.append("                    <tr>\n");
                html.append(String.format("                        <td>(%d, %d)</td>\n", region.getX(), region.getY()));
                html.append(String.format("                        <td>%d × %d</td>\n", region.getWidth(), region.getHeight()));
                html.append(String.format("                        <td><span class=\"issue-badge\">%s</span></td>\n", 
                        capitalize(region.getIssueType() != null ? region.getIssueType() : "other")));
                html.append(String.format("                        <td>%s</td>\n", 
                        region.getObservation() != null ? region.getObservation() : "Visual difference detected"));
                html.append(String.format("                        <td>%.2f%%</td>\n", region.getImpactPercent()));
                html.append("                    </tr>\n");
            }
            
            html.append("                </tbody>\n");
            html.append("            </table>\n");
            html.append("        </div>\n");
        }

        // Footer
        html.append("        <div class=\"footer\">\n");
        html.append("            <p>Generated by Figma HTML Visual Mirror Tool</p>\n");
        html.append("            <p>Severity Guide: <span style=\"color: #28a745;\">MINOR</span> (< 1%) | ");
        html.append("<span style=\"color: #ffc107;\">MAJOR</span> (1-5%) | ");
        html.append("<span style=\"color: #dc3545;\">CRITICAL</span> (> 5%)</p>\n");
        html.append("        </div>\n");

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String getCssStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                padding: 20px;
                min-height: 100vh;
            }
            .container {
                max-width: 1400px;
                margin: 0 auto;
                background: white;
                border-radius: 12px;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                overflow: hidden;
            }
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 30px;
                text-align: center;
            }
            .header h1 {
                font-size: 2.5em;
                margin-bottom: 10px;
            }
            .timestamp {
                opacity: 0.9;
                font-size: 0.9em;
            }
            .summary-card {
                display: flex;
                justify-content: space-around;
                padding: 30px;
                background: #f8f9fa;
                border-bottom: 2px solid #e9ecef;
            }
            .metric {
                text-align: center;
            }
            .metric-label {
                font-size: 0.9em;
                color: #6c757d;
                margin-bottom: 10px;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            .metric-value {
                font-size: 2.5em;
                font-weight: bold;
            }
            .section {
                padding: 30px;
                border-bottom: 1px solid #e9ecef;
            }
            .section h2 {
                color: #333;
                margin-bottom: 20px;
                font-size: 1.8em;
            }
            .image-comparison {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                gap: 20px;
                margin-top: 20px;
            }
            .image-container {
                text-align: center;
            }
            .image-container h3 {
                margin-bottom: 15px;
                color: #495057;
            }
            .comparison-image {
                width: 100%;
                max-width: 100%;
                height: auto;
                border: 2px solid #dee2e6;
                border-radius: 8px;
                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            }
            .observations {
                display: flex;
                flex-direction: column;
                gap: 10px;
            }
            .observation-item {
                padding: 15px;
                background: #fff3cd;
                border-left: 4px solid #ffc107;
                border-radius: 4px;
            }
            .observation-item.success {
                background: #d4edda;
                border-left-color: #28a745;
            }
            .issue-types {
                display: flex;
                flex-wrap: wrap;
                gap: 15px;
            }
            .issue-type-item {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 12px 20px;
                background: #e9ecef;
                border-radius: 6px;
                min-width: 200px;
            }
            .issue-type-name {
                font-weight: 600;
                color: #495057;
            }
            .issue-type-count {
                background: #667eea;
                color: white;
                padding: 4px 12px;
                border-radius: 12px;
                font-weight: bold;
            }
            .issues-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
            }
            .issues-table th {
                background: #667eea;
                color: white;
                padding: 12px;
                text-align: left;
            }
            .issues-table td {
                padding: 12px;
                border-bottom: 1px solid #dee2e6;
            }
            .issues-table tr:hover {
                background: #f8f9fa;
            }
            .issue-badge {
                display: inline-block;
                padding: 4px 10px;
                background: #667eea;
                color: white;
                border-radius: 4px;
                font-size: 0.85em;
                font-weight: 600;
            }
            .footer {
                padding: 20px;
                text-align: center;
                background: #f8f9fa;
                color: #6c757d;
                font-size: 0.9em;
            }
            """;
    }

    private String getSeverityColor(IssueSeverity severity) {
        switch (severity) {
            case MINOR: return "#28a745";
            case MAJOR: return "#ffc107";
            case CRITICAL: return "#dc3545";
            default: return "#6c757d";
        }
    }

    private Map<String, Long> countIssueTypes(java.util.List<DiffRegion> regions) {
        Map<String, Long> counts = new HashMap<>();
        for (DiffRegion region : regions) {
            String type = region.getIssueType() != null ? region.getIssueType() : "other";
            counts.put(type, counts.getOrDefault(type, 0L) + 1);
        }
        return counts;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
