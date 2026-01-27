package com.mirror.semantic;

import com.mirror.model.SemanticComparisonResult;
import com.mirror.model.SemanticComparisonResult.AlignmentIssue;
import com.mirror.model.SemanticComparisonResult.ColorIssue;
import com.mirror.model.SemanticComparisonResult.ElementSizeIssue;
import com.mirror.model.SemanticComparisonResult.ScreenDimensionsResult;
import com.mirror.model.SemanticComparisonResult.SemanticSeverity;
import com.mirror.model.SemanticComparisonResult.SpacingIssue;
import com.mirror.model.SemanticComparisonResult.TypographyIssue;
import com.mirror.model.SemanticComparisonResult.TypographySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Core semantic comparison logic between Figma and HTML snapshots.
 * Applies opinionated thresholds to ignore pixel noise and highlight
 * only meaningful design intent differences.
 */
public class SemanticAnalyzer {

    // Tolerances (can be externalized later)
    private static final int PIXEL_NOISE_THRESHOLD = 3;       // px
    private static final double BREAKPOINT_TOLERANCE_RATIO = 0.05; // 5% size tolerance within same breakpoint

    private static final double FONT_SIZE_TOLERANCE = 1.0;    // px
    private static final double LINE_HEIGHT_TOLERANCE = 2.0;  // px
    private static final double LETTER_SPACING_TOLERANCE = 0.2; // px

    private static final double SPACING_TOLERANCE = 8.0;      // px

    private static final double COLOR_DELTA_E_WARN = 2.0;
    private static final double COLOR_DELTA_E_FAIL = 6.0;

    public SemanticComparisonResult analyze(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html) {
        SemanticComparisonResult result = new SemanticComparisonResult();

        analyzeScreenDimensions(figma, html, result);
        analyzeSectionOrder(figma, html, result);
        analyzeTypography(figma, html, result);
        analyzeSectionSizesAndAlignment(figma, html, result);
        analyzeSectionSpacing(figma, html, result);
        // Color comparison can be extended once HTML color roles are mapped to Figma; placeholder for now

        result.finalizeSummary();
        return result;
    }

    private void analyzeScreenDimensions(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html, SemanticComparisonResult result) {
        ScreenDimensionsResult sd = new ScreenDimensionsResult();
        sd.setFigmaWidth(figma.getFrameWidth());
        sd.setFigmaHeight(figma.getFrameHeight());
        sd.setHtmlWidth(html.getDocumentWidth());
        sd.setHtmlHeight(html.getDocumentHeight());

        int widthDiff = Math.abs(figma.getFrameWidth() - html.getDocumentWidth());
        int heightDiff = Math.abs(figma.getFrameHeight() - html.getDocumentHeight());
        sd.setWidthDiffPx(widthDiff);
        sd.setHeightDiffPx(heightDiff);

        boolean withinTolerance = withinBreakpointTolerance(figma.getFrameWidth(), html.getDocumentWidth())
                && withinBreakpointTolerance(figma.getFrameHeight(), html.getDocumentHeight());
        sd.setWithinBreakpointTolerance(withinTolerance);

        if (withinTolerance) {
            sd.setSeverity(SemanticSeverity.PASS);
            sd.setNotes("Dimensions within breakpoint-level tolerance");
        } else {
            // treat large dimension drift as structural
            sd.setSeverity(SemanticSeverity.FAIL);
            sd.setNotes("Frame vs HTML dimensions differ beyond breakpoint tolerance");
        }

        result.setScreenDimensions(sd);
    }

    private boolean withinBreakpointTolerance(int expected, int actual) {
        if (expected == 0 || actual == 0) return true;
        double diff = Math.abs(expected - actual);
        double ratio = diff / (double) expected;
        return ratio <= BREAKPOINT_TOLERANCE_RATIO;
    }

    private void analyzeTypography(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html, SemanticComparisonResult result) {
        List<FigmaSemanticSnapshot.TextNode> figmaTexts = new ArrayList<>(figma.getTextNodes());
        List<HtmlSemanticSnapshot.TextNode> htmlTexts = new ArrayList<>(html.getTextNodes());

        int count = Math.min(figmaTexts.size(), htmlTexts.size());
        for (int i = 0; i < count; i++) {
            FigmaSemanticSnapshot.TextNode ft = figmaTexts.get(i);
            HtmlSemanticSnapshot.TextNode ht = htmlTexts.get(i);

            TypographyIssue issue = new TypographyIssue();
            issue.setElementId(ft.getName() != null ? ft.getName() : ("text_" + i));

            TypographySnapshot figmaSnap = new TypographySnapshot();
            figmaSnap.setText(normalizeText(ft.getText()));
            figmaSnap.setFontFamily(safe(ft.getFontFamily()));
            figmaSnap.setFontSize(ft.getFontSize());
            figmaSnap.setFontWeight(safe(ft.getFontWeight()));
            figmaSnap.setLineHeight(ft.getLineHeight());
            figmaSnap.setLetterSpacing(ft.getLetterSpacing());
            figmaSnap.setColor(safe(ft.getColor()));

            TypographySnapshot htmlSnap = new TypographySnapshot();
            htmlSnap.setText(normalizeText(ht.getText()));
            htmlSnap.setFontFamily(safe(ht.getFontFamily()));
            htmlSnap.setFontSize(ht.getFontSize());
            htmlSnap.setFontWeight(safe(ht.getFontWeight()));
            htmlSnap.setLineHeight(ht.getLineHeight());
            htmlSnap.setLetterSpacing(ht.getLetterSpacing());
            htmlSnap.setColor(safe(ht.getColor()));

            issue.setFigma(figmaSnap);
            issue.setHtml(htmlSnap);

            boolean textMatches = equalsIgnoreWhitespace(figmaSnap.getText(), htmlSnap.getText());
            boolean fontFamilyMatches = normalizeFontFamily(figmaSnap.getFontFamily())
                    .equalsIgnoreCase(normalizeFontFamily(htmlSnap.getFontFamily()));

            double fontSizeDiff = Math.abs(figmaSnap.getFontSize() - htmlSnap.getFontSize());
            double lineHeightDiff = Math.abs(figmaSnap.getLineHeight() - htmlSnap.getLineHeight());
            double letterSpacingDiff = Math.abs(figmaSnap.getLetterSpacing() - htmlSnap.getLetterSpacing());

            boolean withinFontTolerance = fontSizeDiff <= FONT_SIZE_TOLERANCE
                    && lineHeightDiff <= LINE_HEIGHT_TOLERANCE
                    && letterSpacingDiff <= LETTER_SPACING_TOLERANCE;

            StringBuilder notes = new StringBuilder();
            if (!textMatches) {
                notes.append("Text differs. ");
            }
            if (!fontFamilyMatches) {
                notes.append("Font family differs. ");
            }
            if (fontSizeDiff > FONT_SIZE_TOLERANCE) {
                notes.append("Font size ").append(diffString(figmaSnap.getFontSize(), htmlSnap.getFontSize())).append(". ");
            }
            if (lineHeightDiff > LINE_HEIGHT_TOLERANCE) {
                notes.append("Line height ").append(diffString(figmaSnap.getLineHeight(), htmlSnap.getLineHeight())).append(". ");
            }
            if (letterSpacingDiff > LETTER_SPACING_TOLERANCE) {
                notes.append("Letter spacing ").append(diffString(figmaSnap.getLetterSpacing(), htmlSnap.getLetterSpacing())).append(". ");
            }

            if (textMatches && fontFamilyMatches && withinFontTolerance) {
                issue.setStatus("MATCH");
                issue.setSeverity(SemanticSeverity.PASS);
                issue.setNotes("Within typography tolerance");
            } else if (!textMatches || !fontFamilyMatches) {
                issue.setStatus("MISMATCH");
                issue.setSeverity(SemanticSeverity.FAIL);
                issue.setNotes(notes.toString().trim());
            } else {
                issue.setStatus("DRIFT");
                issue.setSeverity(SemanticSeverity.WARN);
                issue.setNotes(notes.toString().trim());
            }

            // ignore trivial differences where everything is effectively the same
            if (!"MATCH".equals(issue.getStatus())) {
                result.getTextTypography().add(issue);
            }
        }
    }

    private void analyzeSectionSizesAndAlignment(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html, SemanticComparisonResult result) {
        FigmaSemanticSnapshot.Sections fs = figma.getSections();
        HtmlSemanticSnapshot.Sections hs = html.getSections();

        compareSection("header", fs.getHeader(), hs.getHeader(), "header", result);
        compareSection("hero", fs.getHero(), hs.getHero(), "hero", result);
        compareSection("features", fs.getFeatures(), hs.getFeatures(), "features", result);
        compareSection("ctas", fs.getCtas(), hs.getCtas(), "ctas", result);
        compareSection("footer", fs.getFooter(), hs.getFooter(), "footer", result);
    }

    private void compareSection(String id, FigmaSemanticSnapshot.Rect figmaRect, HtmlSemanticSnapshot.Rect htmlRect,
                                String role, SemanticComparisonResult result) {
        if (figmaRect == null || htmlRect == null) {
            // If one is missing entirely, treat as structural failure
            if (figmaRect != null || htmlRect != null) {
                ElementSizeIssue es = new ElementSizeIssue();
                es.setElementId(id);
                es.setRole(role);
                es.setSeverity(SemanticSeverity.FAIL);
                es.setNotes("Section present in one layout but missing in the other");
                result.getElementSizes().add(es);
            }
            return;
        }

        double widthDiff = Math.abs(figmaRect.getWidth() - htmlRect.getWidth());
        double heightDiff = Math.abs(figmaRect.getHeight() - htmlRect.getHeight());

        ElementSizeIssue sizeIssue = new ElementSizeIssue();
        sizeIssue.setElementId(id);
        sizeIssue.setRole(role);
        sizeIssue.setFigmaWidth(figmaRect.getWidth());
        sizeIssue.setFigmaHeight(figmaRect.getHeight());
        sizeIssue.setHtmlWidth(htmlRect.getWidth());
        sizeIssue.setHtmlHeight(htmlRect.getHeight());
        sizeIssue.setWidthDiffPx(widthDiff);
        sizeIssue.setHeightDiffPx(heightDiff);

        boolean majorSizeDiff = widthDiff > PIXEL_NOISE_THRESHOLD || heightDiff > PIXEL_NOISE_THRESHOLD;
        if (majorSizeDiff) {
            sizeIssue.setSeverity(SemanticSeverity.WARN);
            sizeIssue.setNotes("Section size drift beyond noise threshold");
            result.getElementSizes().add(sizeIssue);
        }

        // Alignment: compare x and y offsets
        double xDiff = Math.abs(figmaRect.getX() - htmlRect.getX());
        double yDiff = Math.abs(figmaRect.getY() - htmlRect.getY());

        if (xDiff > PIXEL_NOISE_THRESHOLD) {
            AlignmentIssue ai = new AlignmentIssue();
            ai.setElementId(id);
            ai.setAxis("horizontal");
            ai.setOffsetPx(xDiff);
            ai.setDescription("Horizontal alignment differs for section '" + id + "'");
            ai.setSeverity(xDiff > SPACING_TOLERANCE ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
            result.getAlignment().add(ai);
        }
        if (yDiff > PIXEL_NOISE_THRESHOLD) {
            AlignmentIssue ai = new AlignmentIssue();
            ai.setElementId(id);
            ai.setAxis("vertical");
            ai.setOffsetPx(yDiff);
            ai.setDescription("Vertical alignment differs for section '" + id + "'");
            ai.setSeverity(yDiff > SPACING_TOLERANCE ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
            result.getAlignment().add(ai);
        }
    }

    private void analyzeSectionSpacing(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html, SemanticComparisonResult result) {
        addSpacingIssue("header-hero", "header and hero",
                figma.getSections().getHeader(), figma.getSections().getHero(),
                html.getSections().getHeader(), html.getSections().getHero(),
                result);
        addSpacingIssue("hero-features", "hero and features",
                figma.getSections().getHero(), figma.getSections().getFeatures(),
                html.getSections().getHero(), html.getSections().getFeatures(),
                result);
        addSpacingIssue("features-ctas", "features and ctas",
                figma.getSections().getFeatures(), figma.getSections().getCtas(),
                html.getSections().getFeatures(), html.getSections().getCtas(),
                result);
        addSpacingIssue("ctas-footer", "ctas and footer",
                figma.getSections().getCtas(), figma.getSections().getFooter(),
                html.getSections().getCtas(), html.getSections().getFooter(),
                result);
    }

    private void addSpacingIssue(String id, String between,
                                 FigmaSemanticSnapshot.Rect figmaA, FigmaSemanticSnapshot.Rect figmaB,
                                 HtmlSemanticSnapshot.Rect htmlA, HtmlSemanticSnapshot.Rect htmlB,
                                 SemanticComparisonResult result) {
        if (figmaA == null || figmaB == null || htmlA == null || htmlB == null) {
            return;
        }
        double figmaSpacing = figmaB.getY() - (figmaA.getY() + figmaA.getHeight());
        double htmlSpacing = htmlB.getY() - (htmlA.getY() + htmlA.getHeight());

        double diff = Math.abs(figmaSpacing - htmlSpacing);
        if (Math.abs(diff) <= PIXEL_NOISE_THRESHOLD) {
            return;
        }

        SpacingIssue si = new SpacingIssue();
        si.setElementId(id);
        si.setBetween(between);
        si.setFigmaSpacing(figmaSpacing);
        si.setHtmlSpacing(htmlSpacing);
        si.setDiffPx(diff);
        si.setNotes("Section spacing differs by " + Math.round(diff) + "px");
        si.setSeverity(diff > SPACING_TOLERANCE ? SemanticSeverity.WARN : SemanticSeverity.WARN);

        result.getSpacing().add(si);
    }

    private void analyzeSectionOrder(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html, SemanticComparisonResult result) {
        List<String> expected = new ArrayList<>();
        if (figma.getSections().getHeader() != null) expected.add("header");
        if (figma.getSections().getHero() != null) expected.add("hero");
        if (figma.getSections().getFeatures() != null) expected.add("features");
        if (figma.getSections().getCtas() != null) expected.add("ctas");
        if (figma.getSections().getFooter() != null) expected.add("footer");

        List<SectionWithY> htmlSections = new ArrayList<>();
        if (html.getSections().getHeader() != null) {
            htmlSections.add(new SectionWithY("header", html.getSections().getHeader().getY()));
        }
        if (html.getSections().getHero() != null) {
            htmlSections.add(new SectionWithY("hero", html.getSections().getHero().getY()));
        }
        if (html.getSections().getFeatures() != null) {
            htmlSections.add(new SectionWithY("features", html.getSections().getFeatures().getY()));
        }
        if (html.getSections().getCtas() != null) {
            htmlSections.add(new SectionWithY("ctas", html.getSections().getCtas().getY()));
        }
        if (html.getSections().getFooter() != null) {
            htmlSections.add(new SectionWithY("footer", html.getSections().getFooter().getY()));
        }

        htmlSections.sort((a, b) -> Double.compare(a.y, b.y));

        List<String> actual = new ArrayList<>();
        for (SectionWithY s : htmlSections) {
            actual.add(s.name);
        }

        if (expected.isEmpty() || actual.isEmpty()) {
            return;
        }

        if (!expected.equals(actual)) {
            SemanticComparisonResult.OrderIssue oi = new SemanticComparisonResult.OrderIssue();
            oi.setExpectedOrder(expected);
            oi.setActualOrder(actual);
            oi.setDescription("Major UI sections appear in a different sequence than Figma");
            oi.setSeverity(SemanticSeverity.FAIL);
            result.getOrder().add(oi);
        }
    }

    private static class SectionWithY {
        String name;
        double y;

        SectionWithY(String name, double y) {
            this.name = name;
            this.y = y;
        }
    }

    // --- Color comparison helpers (Delta E approximation on sRGB) ---

    public ColorIssue compareColor(String elementId, String role, String figmaColorHex, String htmlColorCss) {
        if (figmaColorHex == null || htmlColorCss == null) {
            return null;
        }
        int[] figmaRgb = parseColor(figmaColorHex);
        int[] htmlRgb = parseCssColor(htmlColorCss);
        if (figmaRgb == null || htmlRgb == null) {
            return null;
        }
        double deltaE = deltaE(figmaRgb, htmlRgb);

        if (deltaE < COLOR_DELTA_E_WARN) {
            return null; // treat as noise
        }

        ColorIssue ci = new ColorIssue();
        ci.setElementId(elementId);
        ci.setRole(role);
        ci.setFigmaColor(toHex(figmaRgb));
        ci.setHtmlColor(toHex(htmlRgb));
        ci.setDeltaE(deltaE);
        ci.setSeverity(deltaE > COLOR_DELTA_E_FAIL ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
        ci.setNotes("Color difference ΔE ≈ " + Math.round(deltaE));
        return ci;
    }

    private int[] parseColor(String hex) {
        if (hex == null) return null;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0)
                    + s.charAt(1) + s.charAt(1)
                    + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6) return null;
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int[] parseCssColor(String css) {
        if (css == null) return null;
        String s = css.trim().toLowerCase();
        if (s.startsWith("#")) {
            return parseColor(s);
        }
        if (s.startsWith("rgb")) {
            int start = s.indexOf('(');
            int end = s.indexOf(')');
            if (start < 0 || end < 0) return null;
            String[] parts = s.substring(start + 1, end).split(",");
            if (parts.length < 3) return null;
            int[] rgb = new int[3];
            for (int i = 0; i < 3; i++) {
                String p = parts[i].trim();
                if (p.endsWith("%")) {
                    double pct = Double.parseDouble(p.substring(0, p.length() - 1));
                    rgb[i] = (int) Math.round(255.0 * pct / 100.0);
                } else {
                    rgb[i] = Integer.parseInt(p);
                }
            }
            return rgb;
        }
        // Basic named colors can be extended; default to null for unknown
        if ("white".equals(s)) return new int[]{255, 255, 255};
        if ("black".equals(s)) return new int[]{0, 0, 0};
        if ("transparent".equals(s)) return null;
        return null;
    }

    private double deltaE(int[] rgb1, int[] rgb2) {
        // Simple Euclidean distance normalized to approximate ΔE; good enough for small thresholds
        double dr = rgb1[0] - rgb2[0];
        double dg = rgb1[1] - rgb2[1];
        double db = rgb1[2] - rgb2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db) / Math.sqrt(3 * 255 * 255) * 100.0;
    }

    private String toHex(int[] rgb) {
        return String.format("#%02X%02X%02X",
                clamp(rgb[0], 0, 255),
                clamp(rgb[1], 0, 255),
                clamp(rgb[2], 0, 255));
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    private boolean equalsIgnoreWhitespace(String a, String b) {
        return normalizeText(a).equals(normalizeText(b));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String normalizeFontFamily(String family) {
        if (family == null) return "";
        String f = family.toLowerCase();
        // strip quotes and fallbacks (keep primary family)
        int comma = f.indexOf(',');
        if (comma >= 0) {
            f = f.substring(0, comma);
        }
        return f.replace("\"", "").replace("'", "").trim();
    }

    private String diffString(double expected, double actual) {
        double diff = actual - expected;
        String sign = diff > 0 ? "+" : "";
        return sign + Math.round(diff) + "px";
    }
}

