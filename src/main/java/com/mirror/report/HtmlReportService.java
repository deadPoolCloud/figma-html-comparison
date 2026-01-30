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
    public void generate(com.mirror.model.SemanticComparisonResult result) {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String htmlPath = OUTPUT_DIR + "report_" + timestamp + ".html";

            FileWriter writer = new FileWriter(htmlPath);
            writer.write(generateSemanticHtmlContent(result));
            writer.close();

            System.out.println("HTML Report generated: " + htmlPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }

    private String generateSemanticHtmlContent(com.mirror.model.SemanticComparisonResult result) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Semantic Comparison Report</title>\n");
        html.append("    <style>\n");
        html.append(getCssStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");

        // Header
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>🧠 Semantic Comparison Report</h1>\n");
        html.append("            <div class=\"timestamp\">Generated: ").append(new Date()).append("</div>\n");
        html.append("        </div>\n");

        // Summary Card
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Elements Audited</div>\n");
        html.append("                <div class=\"metric-value\">")
                .append(result.getSummary().getTotalElementsAudited())
                .append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Detected Issues</div>\n");
        html.append("                <div class=\"metric-value\">").append(result.getSummary().getTotalIssues())
                .append("</div>\n");
        html.append("            </div>\n");
        html.append("            <div class=\"metric\">\n");
        html.append("                <div class=\"metric-label\">Overall Status</div>\n");
        html.append(String.format("                <div class=\"metric-value\" style=\"color: %s;\">%s</div>\n",
                getSemanticSeverityColor(result.getSummary().getSeverity()), result.getSummary().getSeverity()));
        html.append("            </div>\n");
        html.append("        </div>\n");

        // NEW: Guidance / How to read the report
        html.append(generateGuidanceSection());

        // Detailed Semantic Sections
        html.append(generateTypographySection(result));
        html.append(generateSizeSection(result));
        html.append(generateColorSection(result));
        html.append(generateMissingExtraSection(result));

        // NEW: Quality Checklist
        html.append(generateChecklistSection());

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private String generateGuidanceSection() {
        return """
                <div class="section guidance-section">
                    <h2>📖 How to Audit This Report</h2>
                    <div class="guidance-grid">
                        <div class="guidance-item">
                            <span class="icon">🔍</span>
                            <strong>Inspect Element</strong>
                            <p>Right-click the unmatched element in your browser and use 'Inspect' to check its computed styles against the Figma values below.</p>
                        </div>
                        <div class="guidance-item">
                            <span class="icon">🎞️</span>
                            <strong>Scroll Animations</strong>
                            <p>If elements are marked as 'Missing', they might be hiding behind scroll animations. Try disabling JS animations to verify.</p>
                        </div>
                        <div class="guidance-item">
                            <span class="icon">📐</span>
                            <strong>Box Model</strong>
                            <p>Figma sizes often include padding. Ensure your CSS box-sizing is set correctly to match Figma dimensions.</p>
                        </div>
                    </div>
                    <div class="legend">
                        <span class="legend-item"><span class="badge-success">MATCH</span> Correct implementation</span>
                        <span class="legend-item"><span class="badge-drift">DRIFT</span> Text & Font match, but slightly different size/spacing</span>
                        <span class="legend-item"><span class="badge-fail">MISMATCH</span> Major style/text difference</span>
                        <span class="legend-item"><span class="badge-fail">🛑 MISSING</span> Not found in HTML</span>
                        <span class="legend-item"><span class="badge-warn">➕ EXTRA</span> Found in HTML but not in Design</span>
                    </div>
                </div>
                """;
    }

    private String generateChecklistSection() {
        return """
                <div class="section checklist-section">
                    <h2>✅ Post-Audit Checklist</h2>
                    <ul>
                        <li>Check if Google Fonts are loaded correctly if font-family mismatches.</li>
                        <li>Verify that line-height in CSS matches Figma's pixel or percentage value.</li>
                        <li>Ensure interactive elements have proper :hover and :focus tags (audit manually).</li>
                        <li>If 'Extra' elements are found, confirm they are intentional (e.g., helper divs, containers).</li>
                    </ul>
                </div>
                """;
    }

    private String getSemanticSeverityColor(com.mirror.model.SemanticComparisonResult.SemanticSeverity severity) {
        switch (severity) {
            case FAIL:
                return "#dc3545";
            case WARN:
                return "#ffc107";
            default:
                return "#28a745";
        }
    }

    private String generateTypographySection(com.mirror.model.SemanticComparisonResult result) {
        if (result.getTextTypography().isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>📝 Full Typography Audit</h2>");
        sb.append(
                "<p style=\"margin-bottom: 15px; color: #666;\">Comparing every matched text node between Figma and HTML.</p>");
        sb.append(
                "<table class=\"issues-table audit-table\"><thead>" +
                        "<tr>" +
                        "<th>Element/Text</th>" +
                        "<th>Status</th>" +
                        "<th>Font Family</th>" +
                        "<th>Size</th>" +
                        "<th>Color</th>" +
                        "</tr></thead><tbody>");

        for (var issue : result.getTextTypography()) {
            boolean isPass = issue.getSeverity() == com.mirror.model.SemanticComparisonResult.SemanticSeverity.PASS;
            String rowClass = isPass ? "audit-pass" : "audit-fail";
            String badgeClass = isPass ? "badge-success"
                    : (issue.getStatus().contains("DRIFT") ? "badge-drift" : "badge-fail");

            sb.append("<tr class=\"").append(rowClass).append("\">");

            // Col 1: Text Preview
            sb.append("<td><div class=\"text-preview\" title=\"").append(issue.getFigma().getText()).append("\">")
                    .append(truncate(issue.getFigma().getText(), 50))
                    .append("</div></td>");

            // Col 2: Status
            sb.append("<td><span class=\"issue-badge ").append(badgeClass).append("\">")
                    .append(issue.getStatus()).append("</span>");
            if (!isPass) {
                sb.append("<div class=\"issue-note\">").append(issue.getNotes()).append("</div>");
            }
            sb.append("</td>");

            // Col 3: Font Family Comparison
            sb.append("<td>").append(formatDiff(issue.getFigma().getFontFamily(), issue.getHtml().getFontFamily()))
                    .append("</td>");

            // Col 4: Size Comparison
            sb.append("<td>")
                    .append(formatDiff(issue.getFigma().getFontSize() + "px", issue.getHtml().getFontSize() + "px"))
                    .append(" <span style=\"color:#999\">/</span> ")
                    .append(formatDiff(issue.getFigma().getFontWeight(), issue.getHtml().getFontWeight()))
                    .append("</td>");

            // Col 5: Color Comparison
            sb.append("<td>").append(formatDiff(issue.getFigma().getColor(), issue.getHtml().getColor()))
                    .append("</td>");

            sb.append("</tr>");
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private String generateSizeSection(com.mirror.model.SemanticComparisonResult result) {
        if (result.getElementSizes().isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>📏 Layout & Size Audit</h2>");
        sb.append(
                "<table class=\"issues-table\"><thead><tr><th>Element</th><th>Role</th><th>Figma Size</th><th>HTML Size</th><th>Status</th></tr></thead><tbody>");

        for (var issue : result.getElementSizes()) {
            boolean isFail = issue.getSeverity() == com.mirror.model.SemanticComparisonResult.SemanticSeverity.FAIL;
            sb.append("<tr>");
            sb.append("<td>").append(issue.getElementId()).append("</td>");
            sb.append("<td>").append(issue.getRole()).append("</td>");
            sb.append(String.format("<td>%.0fx%.0fpx</td>", issue.getFigmaWidth(), issue.getFigmaHeight()));
            sb.append(String.format("<td>%.0fx%.0fpx</td>", issue.getHtmlWidth(), issue.getHtmlHeight()));
            sb.append("<td>").append(isFail ? "❌ FAIL" : "⚠️ WARN").append("</td>"); // Currently only warn/fail in this
                                                                                     // list
            sb.append("</tr>");
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    // Helpers for Audit Table
    private String truncate(String s, int len) {
        if (s == null)
            return "";
        return s.length() > len ? s.substring(0, len) + "..." : s;
    }

    private String formatDiff(String figma, String html) {
        if (figma == null)
            figma = "-";
        if (html == null)
            html = "-";
        if (figma.equalsIgnoreCase(html)) {
            return "<span class=\"val-match\">" + figma + "</span>";
        }
        return "<div class=\"val-diff\"><span class=\"val-figma\">F: " + figma
                + "</span><br><span class=\"val-html\">H: " + html + "</span></div>";
    }

    private String generateColorSection(com.mirror.model.SemanticComparisonResult result) {
        if (result.getColors().isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>🎨 Color Issues</h2>");
        sb.append(
                "<table class=\"issues-table\"><thead><tr><th>Element</th><th>Role</th><th>Note</th></tr></thead><tbody>");

        for (var issue : result.getColors()) {
            if (issue.getSeverity() == com.mirror.model.SemanticComparisonResult.SemanticSeverity.FAIL) {
                sb.append("<tr>");
                sb.append("<td>").append(issue.getElementId()).append("</td>");
                sb.append("<td>").append(issue.getRole()).append("</td>");
                sb.append("<td>").append(issue.getNotes()).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private String generateMissingExtraSection(com.mirror.model.SemanticComparisonResult result) {
        if (result.getMissingElements().isEmpty() && result.getExtraElements().isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"section\"><h2>🚫 Structural Discrepancies</h2>");
        sb.append(
                "<p class=\"section-desc\">These issues represent architecture differences. Missing items are in Figma but not HTML; Extra items might be unapproved dev additions.</p>");

        if (!result.getMissingElements().isEmpty()) {
            sb.append("<div class=\"sub-header missing\">🛑 Missing in HTML (Present in Figma)</div>");
            sb.append(
                    "<table class=\"issues-table\"><thead><tr><th>Element Name</th><th>Type</th><th>Design Content</th><th>Troubleshooting</th></tr></thead><tbody>");
            for (var issue : result.getMissingElements()) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(issue.getElementId()).append("</strong></td>");
                sb.append("<td><span class=\"badge-type\">").append(issue.getType()).append("</span></td>");
                sb.append("<td><div class=\"text-preview\">").append(truncate(issue.getText(), 100))
                        .append("</div></td>");
                sb.append("<td><span class=\"trouble-hint\">Check for visibility:hidden or animations.</span></td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        if (!result.getExtraElements().isEmpty()) {
            sb.append("<div class=\"sub-header extra\">➕ Extra in HTML (Not in Figma)</div>");
            sb.append(
                    "<table class=\"issues-table\"><thead><tr><th>Element Tag</th><th>Type</th><th>Live Content</th><th>Status</th></tr></thead><tbody>");
            for (var issue : result.getExtraElements()) {
                sb.append("<tr>");
                sb.append("<td><code>&lt;").append(issue.getTag()).append("&gt;</code></td>");
                sb.append("<td><span class=\"badge-type\">").append(issue.getTag()).append("</span></td>");
                sb.append("<td><div class=\"text-preview\">").append(truncate(issue.getText(), 100))
                        .append("</div></td>");
                sb.append("<td><span class=\"badge-warn\">REVIEW NEEDED</span></td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }

        sb.append("</div>");
        return sb.toString();
    }

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
        html.append("                <div class=\"metric-value\">").append(result.getRegions().size())
                .append("</div>\n");
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
        html.append("                    <img src=\"").append(figmaImg)
                .append("\" alt=\"Figma Design\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("                <div class=\"image-container\">\n");
        html.append("                    <h3>HTML Implementation (Actual)</h3>\n");
        html.append("                    <img src=\"").append(liveImg)
                .append("\" alt=\"Live HTML\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("                <div class=\"image-container\">\n");
        html.append("                    <h3>Differences (Red Overlay)</h3>\n");
        html.append("                    <img src=\"").append(diffImg)
                .append("\" alt=\"Diff\" class=\"comparison-image\">\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");

        // Observations
        html.append("        <div class=\"section\">\n");
        html.append("            <h2>🔍 Human-Readable Observations</h2>\n");
        html.append("            <div class=\"observations\">\n");
        if (result.getObservations().isEmpty()) {
            html.append(
                    "                <div class=\"observation-item success\">✅ No significant visual differences detected!</div>\n");
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
                html.append(String.format("                        <td>%d × %d</td>\n", region.getWidth(),
                        region.getHeight()));
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
                .audit-table th { background: #4a5568 !important; }
                .audit-pass { background: #f0fff4 !important; }
                .audit-fail { background: #fff5f5 !important; }
                .text-preview { font-family: monospace; font-size: 0.9em; color: #2d3748; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 250px; }
                .val-diff { font-size: 0.85em; line-height: 1.4; }
                .val-figma { color: #718096; text-decoration: line-through; margin-right: 5px; }
                .val-html { color: #e53e3e; font-weight: bold; }
                .val-match { color: #38a169; font-weight: 500; }
                .badge-success { background: #c6f6d5; color: #22543d; padding: 4px 12px; border-radius: 6px; font-weight: bold; }
                .badge-drift { background: #fff3cd; color: #856404; padding: 4px 12px; border-radius: 6px; font-weight: bold; border: 1px solid #ffeeba; }
                .badge-fail { background: #fed7d7; color: #822727; padding: 4px 12px; border-radius: 6px; font-weight: bold; }
                .badge-warn { background: #feebc8; color: #744210; padding: 4px 12px; border-radius: 6px; font-weight: bold; }
                .badge-type { background: #e2e8f0; color: #4a5568; padding: 2px 8px; border-radius: 4px; font-size: 0.85em; font-weight: 600; text-transform: uppercase; }
                .issue-note { font-size: 0.85em; color: #b7791f; margin-top: 6px; display: block; border-left: 2px solid #ed8936; padding-left: 8px; }
                .guidance-section { background: #f7fafc !important; }
                .guidance-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 20px; }
                .guidance-item { background: white; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; }
                .guidance-item .icon { font-size: 1.5em; display: block; margin-bottom: 10px; }
                .guidance-item strong { display: block; margin-bottom: 5px; color: #2d3748; }
                .guidance-item p { font-size: 0.85em; color: #718096; line-height: 1.4; }
                .legend { margin-top: 25px; padding-top: 15px; border-top: 1px dashed #cbd5e0; display: flex; gap: 20px; flex-wrap: wrap; }
                .legend-item { font-size: 0.85em; color: #4a5568; display: flex; align-items: center; gap: 8px; }
                .checklist-section { background: #fffaf0 !important; }
                .checklist-section ul { margin-left: 20px; color: #744210; margin-top: 15px; }
                .checklist-section li { margin-bottom: 8px; font-size: 0.9em; }
                .sub-header { font-weight: bold; margin: 20px 0 10px 0; padding: 8px 15px; border-radius: 6px; font-size: 1.1em; }
                .sub-header.missing { background: #fff5f5; color: #c53030; border-left: 4px solid #c53030; }
                .sub-header.extra { background: #fffaf0; color: #975a16; border-left: 4px solid #975a16; }
                .section-desc { color: #718096; font-size: 0.9em; margin-bottom: 15px; }
                .trouble-hint { color: #a0aec0; font-size: 0.8em; font-style: italic; }
                code { background: #edf2f7; padding: 2px 4px; border-radius: 4px; font-family: monospace; font-size: 0.9em; }
                """;
    }

    private String getSeverityColor(IssueSeverity severity) {
        switch (severity) {
            case MINOR:
                return "#28a745";
            case MAJOR:
                return "#ffc107";
            case CRITICAL:
                return "#dc3545";
            default:
                return "#6c757d";
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
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
