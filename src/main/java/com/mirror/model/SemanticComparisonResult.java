package com.mirror.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level, semantic comparison result between Figma design and HTML implementation.
 * This intentionally ignores pixel-level noise and focuses on design intent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SemanticComparisonResult {

    @JsonProperty("screen_dimensions")
    private ScreenDimensionsResult screenDimensions;

    @JsonProperty("text_typography")
    private List<TypographyIssue> textTypography = new ArrayList<>();

    @JsonProperty("element_sizes")
    private List<ElementSizeIssue> elementSizes = new ArrayList<>();

    @JsonProperty("alignment")
    private List<AlignmentIssue> alignment = new ArrayList<>();

    @JsonProperty("order")
    private List<OrderIssue> order = new ArrayList<>();

    @JsonProperty("colors")
    private List<ColorIssue> colors = new ArrayList<>();

    @JsonProperty("spacing")
    private List<SpacingIssue> spacing = new ArrayList<>();

    @JsonProperty("summary")
    private SemanticSummary summary = new SemanticSummary();

    public ScreenDimensionsResult getScreenDimensions() {
        return screenDimensions;
    }

    public void setScreenDimensions(ScreenDimensionsResult screenDimensions) {
        this.screenDimensions = screenDimensions;
    }

    public List<TypographyIssue> getTextTypography() {
        return textTypography;
    }

    public List<ElementSizeIssue> getElementSizes() {
        return elementSizes;
    }

    public List<AlignmentIssue> getAlignment() {
        return alignment;
    }

    public List<OrderIssue> getOrder() {
        return order;
    }

    public List<ColorIssue> getColors() {
        return colors;
    }

    public List<SpacingIssue> getSpacing() {
        return spacing;
    }

    public SemanticSummary getSummary() {
        return summary;
    }

    /**
     * Computes summary totals and overall severity from individual issue lists.
     */
    public void finalizeSummary() {
        int totalIssues =
                size(textTypography) +
                size(elementSizes) +
                size(alignment) +
                size(order) +
                size(colors) +
                size(spacing);
        summary.setTotalIssues(totalIssues);

        SemanticSeverity overall = SemanticSeverity.PASS;

        // FAIL if any structural / major issues
        if (hasSeverity(textTypography, SemanticSeverity.FAIL)
                || hasSeverity(elementSizes, SemanticSeverity.FAIL)
                || hasSeverity(alignment, SemanticSeverity.FAIL)
                || hasSeverity(order, SemanticSeverity.FAIL)
                || hasSeverity(colors, SemanticSeverity.FAIL)
                || hasSeverity(spacing, SemanticSeverity.FAIL)
                || (screenDimensions != null && screenDimensions.getSeverity() == SemanticSeverity.FAIL)) {
            overall = SemanticSeverity.FAIL;
        } else if (totalIssues > 0
                || (screenDimensions != null && screenDimensions.getSeverity() == SemanticSeverity.WARN)) {
            // WARN if only minor drift
            overall = SemanticSeverity.WARN;
        }

        summary.setSeverity(overall);
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private boolean hasSeverity(List<? extends SemanticIssue> issues, SemanticSeverity severity) {
        if (issues == null) return false;
        for (SemanticIssue issue : issues) {
            if (issue.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }

    // --- Base interfaces / DTOs ---

    public interface SemanticIssue {
        SemanticSeverity getSeverity();
    }

    public enum SemanticSeverity {
        PASS,
        WARN,
        FAIL
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SemanticSummary {

        @JsonProperty("total_issues")
        private int totalIssues;

        @JsonProperty("severity")
        private SemanticSeverity severity = SemanticSeverity.PASS;

        public int getTotalIssues() {
            return totalIssues;
        }

        public void setTotalIssues(int totalIssues) {
            this.totalIssues = totalIssues;
        }

        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ScreenDimensionsResult {

        @JsonProperty("figma_width")
        private int figmaWidth;

        @JsonProperty("figma_height")
        private int figmaHeight;

        @JsonProperty("html_width")
        private int htmlWidth;

        @JsonProperty("html_height")
        private int htmlHeight;

        @JsonProperty("width_diff_px")
        private int widthDiffPx;

        @JsonProperty("height_diff_px")
        private int heightDiffPx;

        @JsonProperty("within_breakpoint_tolerance")
        private boolean withinBreakpointTolerance;

        @JsonProperty("severity")
        private SemanticSeverity severity = SemanticSeverity.PASS;

        @JsonProperty("notes")
        private String notes;

        public int getFigmaWidth() {
            return figmaWidth;
        }

        public void setFigmaWidth(int figmaWidth) {
            this.figmaWidth = figmaWidth;
        }

        public int getFigmaHeight() {
            return figmaHeight;
        }

        public void setFigmaHeight(int figmaHeight) {
            this.figmaHeight = figmaHeight;
        }

        public int getHtmlWidth() {
            return htmlWidth;
        }

        public void setHtmlWidth(int htmlWidth) {
            this.htmlWidth = htmlWidth;
        }

        public int getHtmlHeight() {
            return htmlHeight;
        }

        public void setHtmlHeight(int htmlHeight) {
            this.htmlHeight = htmlHeight;
        }

        public int getWidthDiffPx() {
            return widthDiffPx;
        }

        public void setWidthDiffPx(int widthDiffPx) {
            this.widthDiffPx = widthDiffPx;
        }

        public int getHeightDiffPx() {
            return heightDiffPx;
        }

        public void setHeightDiffPx(int heightDiffPx) {
            this.heightDiffPx = heightDiffPx;
        }

        public boolean isWithinBreakpointTolerance() {
            return withinBreakpointTolerance;
        }

        public void setWithinBreakpointTolerance(boolean withinBreakpointTolerance) {
            this.withinBreakpointTolerance = withinBreakpointTolerance;
        }

        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TypographyIssue implements SemanticIssue {

        @JsonProperty("element_id")
        private String elementId;

        @JsonProperty("figma")
        private TypographySnapshot figma;

        @JsonProperty("html")
        private TypographySnapshot html;

        @JsonProperty("status")
        private String status; // MATCH | DRIFT | MISMATCH

        @JsonProperty("severity")
        private SemanticSeverity severity = SemanticSeverity.WARN;

        @JsonProperty("notes")
        private String notes;

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public TypographySnapshot getFigma() {
            return figma;
        }

        public void setFigma(TypographySnapshot figma) {
            this.figma = figma;
        }

        public TypographySnapshot getHtml() {
            return html;
        }

        public void setHtml(TypographySnapshot html) {
            this.html = html;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TypographySnapshot {

        @JsonProperty("text")
        private String text;

        @JsonProperty("font_family")
        private String fontFamily;

        @JsonProperty("font_size")
        private double fontSize;

        @JsonProperty("font_weight")
        private String fontWeight;

        @JsonProperty("line_height")
        private double lineHeight;

        @JsonProperty("letter_spacing")
        private double letterSpacing;

        @JsonProperty("color")
        private String color;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getFontFamily() {
            return fontFamily;
        }

        public void setFontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
        }

        public double getFontSize() {
            return fontSize;
        }

        public void setFontSize(double fontSize) {
            this.fontSize = fontSize;
        }

        public String getFontWeight() {
            return fontWeight;
        }

        public void setFontWeight(String fontWeight) {
            this.fontWeight = fontWeight;
        }

        public double getLineHeight() {
            return lineHeight;
        }

        public void setLineHeight(double lineHeight) {
            this.lineHeight = lineHeight;
        }

        public double getLetterSpacing() {
            return letterSpacing;
        }

        public void setLetterSpacing(double letterSpacing) {
            this.letterSpacing = letterSpacing;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ElementSizeIssue implements SemanticIssue {

        @JsonProperty("element_id")
        private String elementId;

        @JsonProperty("role")
        private String role; // image, logo, button, card, input, section

        @JsonProperty("figma_width")
        private double figmaWidth;

        @JsonProperty("figma_height")
        private double figmaHeight;

        @JsonProperty("html_width")
        private double htmlWidth;

        @JsonProperty("html_height")
        private double htmlHeight;

        @JsonProperty("width_diff_px")
        private double widthDiffPx;

        @JsonProperty("height_diff_px")
        private double heightDiffPx;

        @JsonProperty("severity")
        private SemanticSeverity severity;

        @JsonProperty("notes")
        private String notes;

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public double getFigmaWidth() {
            return figmaWidth;
        }

        public void setFigmaWidth(double figmaWidth) {
            this.figmaWidth = figmaWidth;
        }

        public double getFigmaHeight() {
            return figmaHeight;
        }

        public void setFigmaHeight(double figmaHeight) {
            this.figmaHeight = figmaHeight;
        }

        public double getHtmlWidth() {
            return htmlWidth;
        }

        public void setHtmlWidth(double htmlWidth) {
            this.htmlWidth = htmlWidth;
        }

        public double getHtmlHeight() {
            return htmlHeight;
        }

        public void setHtmlHeight(double htmlHeight) {
            this.htmlHeight = htmlHeight;
        }

        public double getWidthDiffPx() {
            return widthDiffPx;
        }

        public void setWidthDiffPx(double widthDiffPx) {
            this.widthDiffPx = widthDiffPx;
        }

        public double getHeightDiffPx() {
            return heightDiffPx;
        }

        public void setHeightDiffPx(double heightDiffPx) {
            this.heightDiffPx = heightDiffPx;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlignmentIssue implements SemanticIssue {

        @JsonProperty("element_id")
        private String elementId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("axis")
        private String axis; // horizontal | vertical

        @JsonProperty("offset_px")
        private double offsetPx;

        @JsonProperty("severity")
        private SemanticSeverity severity;

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAxis() {
            return axis;
        }

        public void setAxis(String axis) {
            this.axis = axis;
        }

        public double getOffsetPx() {
            return offsetPx;
        }

        public void setOffsetPx(double offsetPx) {
            this.offsetPx = offsetPx;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderIssue implements SemanticIssue {

        @JsonProperty("expected_order")
        private List<String> expectedOrder;

        @JsonProperty("actual_order")
        private List<String> actualOrder;

        @JsonProperty("description")
        private String description;

        @JsonProperty("severity")
        private SemanticSeverity severity;

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public List<String> getExpectedOrder() {
            return expectedOrder;
        }

        public void setExpectedOrder(List<String> expectedOrder) {
            this.expectedOrder = expectedOrder;
        }

        public List<String> getActualOrder() {
            return actualOrder;
        }

        public void setActualOrder(List<String> actualOrder) {
            this.actualOrder = actualOrder;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ColorIssue implements SemanticIssue {

        @JsonProperty("element_id")
        private String elementId;

        @JsonProperty("role")
        private String role; // text | background | border | shadow | button

        @JsonProperty("figma_color")
        private String figmaColor;

        @JsonProperty("html_color")
        private String htmlColor;

        @JsonProperty("delta_e")
        private double deltaE;

        @JsonProperty("severity")
        private SemanticSeverity severity;

        @JsonProperty("notes")
        private String notes;

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getFigmaColor() {
            return figmaColor;
        }

        public void setFigmaColor(String figmaColor) {
            this.figmaColor = figmaColor;
        }

        public String getHtmlColor() {
            return htmlColor;
        }

        public void setHtmlColor(String htmlColor) {
            this.htmlColor = htmlColor;
        }

        public double getDeltaE() {
            return deltaE;
        }

        public void setDeltaE(double deltaE) {
            this.deltaE = deltaE;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpacingIssue implements SemanticIssue {

        @JsonProperty("element_id")
        private String elementId;

        @JsonProperty("between")
        private String between; // e.g. "hero and features"

        @JsonProperty("figma_spacing")
        private double figmaSpacing;

        @JsonProperty("html_spacing")
        private double htmlSpacing;

        @JsonProperty("diff_px")
        private double diffPx;

        @JsonProperty("severity")
        private SemanticSeverity severity;

        @JsonProperty("notes")
        private String notes;

        @Override
        public SemanticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(SemanticSeverity severity) {
            this.severity = severity;
        }

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getBetween() {
            return between;
        }

        public void setBetween(String between) {
            this.between = between;
        }

        public double getFigmaSpacing() {
            return figmaSpacing;
        }

        public void setFigmaSpacing(double figmaSpacing) {
            this.figmaSpacing = figmaSpacing;
        }

        public double getHtmlSpacing() {
            return htmlSpacing;
        }

        public void setHtmlSpacing(double htmlSpacing) {
            this.htmlSpacing = htmlSpacing;
        }

        public double getDiffPx() {
            return diffPx;
        }

        public void setDiffPx(double diffPx) {
            this.diffPx = diffPx;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

