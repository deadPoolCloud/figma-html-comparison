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
import java.util.List;

/**
 * Core semantic comparison logic between Figma and HTML snapshots.
 * Applies opinionated thresholds to ignore pixel noise and highlight
 * only meaningful design intent differences.
 */
public class SemanticAnalyzer {

    private final SemanticConfig config;

    public SemanticAnalyzer() {
        this(SemanticConfig.DEFAULT);
    }

    public SemanticAnalyzer(SemanticConfig config) {
        this.config = config;
    }

    public SemanticComparisonResult analyze(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html) {
        SemanticComparisonResult result = new SemanticComparisonResult();

        analyzeScreenDimensions(figma, html, result);
        analyzeSectionOrder(figma, html, result);
        analyzeTypography(figma, html, result);
        analyzeSectionSizesAndAlignment(figma, html, result);
        analyzeSectionSpacing(figma, html, result);
        analyzeInteractiveElements(figma, html, result);
        // Color comparison can be extended once HTML color roles are mapped to Figma;
        // placeholder for now

        result.finalizeSummary();
        return result;
    }

    private void analyzeInteractiveElements(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
        if (figma.getInteractiveNodes() == null || html.getInteractiveElements() == null) {
            return;
        }

        List<HtmlSemanticSnapshot.InteractiveElement> htmlCandidates = new ArrayList<>(html.getInteractiveElements());
        List<FigmaSemanticSnapshot.InteractiveNode> matchedFigma = new ArrayList<>();

        for (FigmaSemanticSnapshot.InteractiveNode fn : figma.getInteractiveNodes()) {
            // Find match by text
            HtmlSemanticSnapshot.InteractiveElement match = findInteractiveMatch(fn, htmlCandidates);

            if (match != null) {
                htmlCandidates.remove(match);
                matchedFigma.add(fn);
                compareInteractiveElement(fn, match, result);
            }
        }

        // Identify Missing Interactive (Figma ONLY)
        for (FigmaSemanticSnapshot.InteractiveNode fn : figma.getInteractiveNodes()) {
            if (!matchedFigma.contains(fn)) {
                SemanticComparisonResult.MissingElementIssue missing = new SemanticComparisonResult.MissingElementIssue();
                missing.setElementId(fn.getName());
                missing.setText(fn.getText());
                missing.setType("interactive");
                result.getMissingElements().add(missing);
            }
        }

        // Identify Extra Interactive (HTML ONLY)
        for (HtmlSemanticSnapshot.InteractiveElement hn : htmlCandidates) {
            SemanticComparisonResult.ExtraElementIssue extra = new SemanticComparisonResult.ExtraElementIssue();
            extra.setElementId(hn.getTag() + "[" + hn.getText() + "]");
            extra.setText(hn.getText());
            extra.setTag(hn.getTag());
            result.getExtraElements().add(extra);
        }
    }

    private HtmlSemanticSnapshot.InteractiveElement findInteractiveMatch(FigmaSemanticSnapshot.InteractiveNode target,
            List<HtmlSemanticSnapshot.InteractiveElement> candidates) {
        String targetText = normalizeText(target.getText());
        if (targetText.isEmpty())
            return null;

        for (HtmlSemanticSnapshot.InteractiveElement candidate : candidates) {
            if (normalizeText(candidate.getText()).equalsIgnoreCase(targetText)) {
                return candidate;
            }
        }
        return null;
    }

    private void compareInteractiveElement(FigmaSemanticSnapshot.InteractiveNode fn,
            HtmlSemanticSnapshot.InteractiveElement hn, SemanticComparisonResult result) {
        String elementId = "Interactive[" + fn.getText() + "]";

        // Compare Size
        double widthDiff = Math.abs(fn.getRect().getWidth() - hn.getRect().getWidth());
        double heightDiff = Math.abs(fn.getRect().getHeight() - hn.getRect().getHeight());

        if (widthDiff > config.pixelNoiseThreshold || heightDiff > config.pixelNoiseThreshold) {
            ElementSizeIssue sizeIssue = new ElementSizeIssue();
            sizeIssue.setElementId(elementId);
            sizeIssue.setRole("button/interactive");
            sizeIssue.setFigmaWidth(fn.getRect().getWidth());
            sizeIssue.setFigmaHeight(fn.getRect().getHeight());
            sizeIssue.setHtmlWidth(hn.getRect().getWidth());
            sizeIssue.setHtmlHeight(hn.getRect().getHeight());
            sizeIssue.setWidthDiffPx(widthDiff);
            sizeIssue.setHeightDiffPx(heightDiff);
            sizeIssue.setSeverity(SemanticSeverity.WARN); // Warn on size mismatch
            sizeIssue.setNotes("Interactive element size differs");
            result.getElementSizes().add(sizeIssue);
        }

        // Compare Background Color
        ColorIssue colorIssue = compareColor(elementId, "background", fn.getBackgroundColor(), hn.getBackgroundColor());
        if (colorIssue != null) {
            result.getColors().add(colorIssue);
        }
    }

    private void analyzeScreenDimensions(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
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
        if (expected == 0 || actual == 0)
            return true;
        double diff = Math.abs(expected - actual);
        double ratio = diff / (double) expected;
        return ratio <= config.breakpointToleranceRatio;
    }

    private void analyzeTypography(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
        List<FigmaSemanticSnapshot.TextNode> figmaTexts = new ArrayList<>(figma.getTextNodes());
        List<HtmlSemanticSnapshot.TextNode> availableHtmlTexts = new ArrayList<>(html.getTextNodes());

        // 1. Map Figma nodes to closest HTML nodes spatially
        MatchingEngine matchingEngine = new MatchingEngine();
        List<MatchingEngine.Match> matches = matchingEngine.matchTextNodes(figmaTexts, availableHtmlTexts);

        List<FigmaSemanticSnapshot.TextNode> matchedFigma = new ArrayList<>();
        List<HtmlSemanticSnapshot.TextNode> matchedHtml = new ArrayList<>();

        for (MatchingEngine.Match match : matches) {
            compareTypography(match.figma, match.html, result);
            matchedFigma.add(match.figma);
            matchedHtml.add(match.html);
        }

        // Identify Missing (Figma ONLY)
        for (FigmaSemanticSnapshot.TextNode fn : figmaTexts) {
            if (!matchedFigma.contains(fn)) {
                SemanticComparisonResult.MissingElementIssue missing = new SemanticComparisonResult.MissingElementIssue();
                missing.setElementId(fn.getName());
                missing.setText(fn.getText());
                missing.setType("text");
                result.getMissingElements().add(missing);
            }
        }

        // Identify Extra (HTML ONLY)
        for (HtmlSemanticSnapshot.TextNode hn : availableHtmlTexts) {
            if (!matchedHtml.contains(hn)) {
                SemanticComparisonResult.ExtraElementIssue extra = new SemanticComparisonResult.ExtraElementIssue();
                extra.setElementId(hn.getId());
                extra.setText(hn.getText());
                extra.setTag(hn.getTag());
                result.getExtraElements().add(extra);
            }
        }
    }

    private void compareTypography(FigmaSemanticSnapshot.TextNode ft, HtmlSemanticSnapshot.TextNode ht,
            SemanticComparisonResult result) {
        TypographyIssue issue = new TypographyIssue();
        issue.setElementId(ft.getName() != null ? ft.getName() : ft.getText()); // Use text as ID fallback

        TypographySnapshot figmaSnap = new TypographySnapshot();
        figmaSnap.setText(normalizeText(ft.getText()));
        figmaSnap.setFontFamily(safe(ft.getFontFamily()));
        figmaSnap.setFontSize(ft.getFontSize());
        figmaSnap.setFontWeight(safe(ft.getFontWeight()));
        figmaSnap.setLineHeight(ft.getLineHeight());
        figmaSnap.setLetterSpacing(ft.getLetterSpacing());
        figmaSnap.setColor(normalizeToHex(ft.getColor()));

        TypographySnapshot htmlSnap = new TypographySnapshot();
        htmlSnap.setText(normalizeText(ht.getText()));
        htmlSnap.setFontFamily(safe(ht.getFontFamily()));
        htmlSnap.setFontSize(ht.getFontSize());
        htmlSnap.setFontWeight(safe(ht.getFontWeight()));
        htmlSnap.setLineHeight(ht.getLineHeight());
        htmlSnap.setLetterSpacing(ht.getLetterSpacing());
        htmlSnap.setColor(normalizeToHex(ht.getColor()));

        issue.setFigma(figmaSnap);
        issue.setHtml(htmlSnap);

        boolean textMatches = equalsIgnoreWhitespace(figmaSnap.getText(), htmlSnap.getText());
        boolean fontFamilyMatches = normalizeFontFamily(figmaSnap.getFontFamily())
                .equalsIgnoreCase(normalizeFontFamily(htmlSnap.getFontFamily()));

        double fontSizeDiff = Math.abs(figmaSnap.getFontSize() - htmlSnap.getFontSize());
        double lineHeightDiff = Math.abs(figmaSnap.getLineHeight() - htmlSnap.getLineHeight());
        double letterSpacingDiff = Math.abs(figmaSnap.getLetterSpacing() - htmlSnap.getLetterSpacing());

        boolean withinFontTolerance = fontSizeDiff <= config.fontSizeTolerance
                && lineHeightDiff <= config.lineHeightTolerance
                && letterSpacingDiff <= config.letterSpacingTolerance;

        double colorDeltaE = 0;
        int[] fRgb = parseColor(figmaSnap.getColor());
        int[] hRgb = parseColor(htmlSnap.getColor());
        if (fRgb != null && hRgb != null) {
            colorDeltaE = deltaE(fRgb, hRgb);
        }
        boolean colorMatches = colorDeltaE <= config.colorDeltaEWarn;

        StringBuilder notes = new StringBuilder();
        if (!textMatches) {
            notes.append("Text differs. ");
        }
        if (!fontFamilyMatches) {
            notes.append("Font family differs. ");
        }
        if (!colorMatches) {
            notes.append("Color differs (ΔE=").append(Math.round(colorDeltaE)).append("). ");
        }
        if (fontSizeDiff > config.fontSizeTolerance) {
            notes.append("Font size ").append(diffString(figmaSnap.getFontSize(), htmlSnap.getFontSize())).append(". ");
        }
        if (lineHeightDiff > config.lineHeightTolerance) {
            notes.append("Line height ").append(diffString(figmaSnap.getLineHeight(), htmlSnap.getLineHeight()))
                    .append(". ");
        }
        if (letterSpacingDiff > config.letterSpacingTolerance) {
            notes.append("Letter spacing ")
                    .append(diffString(figmaSnap.getLetterSpacing(), htmlSnap.getLetterSpacing())).append(". ");
        }

        if (textMatches && fontFamilyMatches && withinFontTolerance && colorMatches) {
            issue.setStatus("MATCH");
            issue.setSeverity(SemanticSeverity.PASS);
            issue.setNotes("Within typography tolerance");
        } else if (!textMatches || !fontFamilyMatches || colorDeltaE > config.colorDeltaEFail) {
            issue.setStatus("MISMATCH");
            issue.setSeverity(SemanticSeverity.FAIL);
            issue.setNotes(notes.toString().trim());
        } else {
            issue.setStatus("SIZE/SPACING DRIFT");
            issue.setSeverity(SemanticSeverity.WARN);
            issue.setNotes(notes.toString().trim());
        }

        // RECORD EVERYTHING (Audit Log)
        result.getTextTypography().add(issue);
    }

    private void analyzeSectionSizesAndAlignment(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
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

                if (figmaRect != null) {
                    es.setFigmaWidth(figmaRect.getWidth());
                    es.setFigmaHeight(figmaRect.getHeight());
                }
                if (htmlRect != null) {
                    es.setHtmlWidth(htmlRect.getWidth());
                    es.setHtmlHeight(htmlRect.getHeight());
                }

                String missingSide = figmaRect == null ? "Figma" : "HTML";
                es.setNotes("Section '" + id + "' found in " + (figmaRect != null ? "Figma" : "HTML") +
                        " but missing in " + missingSide + ". (Check layer naming)");

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

        boolean majorSizeDiff = widthDiff > config.pixelNoiseThreshold || heightDiff > config.pixelNoiseThreshold;
        if (majorSizeDiff) {
            sizeIssue.setSeverity(SemanticSeverity.WARN);
            sizeIssue.setNotes("Section size drift beyond noise threshold");
            result.getElementSizes().add(sizeIssue);
        }

        // Alignment: compare x and y offsets
        double xDiff = Math.abs(figmaRect.getX() - htmlRect.getX());
        double yDiff = Math.abs(figmaRect.getY() - htmlRect.getY());

        if (xDiff > config.pixelNoiseThreshold) {
            AlignmentIssue ai = new AlignmentIssue();
            ai.setElementId(id);
            ai.setAxis("horizontal");
            ai.setOffsetPx(xDiff);
            ai.setDescription("Horizontal alignment differs for section '" + id + "'");
            ai.setSeverity(xDiff > config.spacingTolerance ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
            result.getAlignment().add(ai);
        }
        if (yDiff > config.pixelNoiseThreshold) {
            AlignmentIssue ai = new AlignmentIssue();
            ai.setElementId(id);
            ai.setAxis("vertical");
            ai.setOffsetPx(yDiff);
            ai.setDescription("Vertical alignment differs for section '" + id + "'");
            ai.setSeverity(yDiff > config.spacingTolerance ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
            result.getAlignment().add(ai);
        }
    }

    private void analyzeSectionSpacing(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
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
        if (Math.abs(diff) <= config.pixelNoiseThreshold) {
            return;
        }

        SpacingIssue si = new SpacingIssue();
        si.setElementId(id);
        si.setBetween(between);
        si.setFigmaSpacing(figmaSpacing);
        si.setHtmlSpacing(htmlSpacing);
        si.setDiffPx(diff);
        si.setNotes("Section spacing differs by " + Math.round(diff) + "px");
        si.setSeverity(diff > config.spacingTolerance ? SemanticSeverity.WARN : SemanticSeverity.WARN);

        result.getSpacing().add(si);
    }

    private void analyzeSectionOrder(FigmaSemanticSnapshot figma, HtmlSemanticSnapshot html,
            SemanticComparisonResult result) {
        List<String> expected = new ArrayList<>();
        if (figma.getSections().getHeader() != null)
            expected.add("header");
        if (figma.getSections().getHero() != null)
            expected.add("hero");
        if (figma.getSections().getFeatures() != null)
            expected.add("features");
        if (figma.getSections().getCtas() != null)
            expected.add("ctas");
        if (figma.getSections().getFooter() != null)
            expected.add("footer");

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

        if (deltaE < config.colorDeltaEWarn) {
            return null; // treat as noise
        }

        ColorIssue ci = new ColorIssue();
        ci.setElementId(elementId);
        ci.setRole(role);
        ci.setFigmaColor(toHex(figmaRgb));
        ci.setHtmlColor(toHex(htmlRgb));
        ci.setDeltaE(deltaE);
        ci.setSeverity(deltaE > config.colorDeltaEFail ? SemanticSeverity.FAIL : SemanticSeverity.WARN);
        ci.setNotes("Color difference ΔE ≈ " + Math.round(deltaE));
        return ci;
    }

    private int[] parseColor(String hex) {
        if (hex == null)
            return null;
        String s = hex.trim();
        if (s.startsWith("#"))
            s = s.substring(1);
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0)
                    + s.charAt(1) + s.charAt(1)
                    + s.charAt(2) + s.charAt(2);
        }
        if (s.length() != 6)
            return null;
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new int[] { r, g, b };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int[] parseCssColor(String css) {
        if (css == null)
            return null;
        String s = css.trim().toLowerCase();
        if (s.startsWith("#")) {
            return parseColor(s);
        }
        if (s.startsWith("rgb")) {
            int start = s.indexOf('(');
            int end = s.indexOf(')');
            if (start < 0 || end < 0)
                return null;
            String[] parts = s.substring(start + 1, end).split(",");
            if (parts.length < 3)
                return null;
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
        if ("white".equals(s))
            return new int[] { 255, 255, 255 };
        if ("black".equals(s))
            return new int[] { 0, 0, 0 };
        if ("transparent".equals(s))
            return null;
        return null;
    }

    private double deltaE(int[] rgb1, int[] rgb2) {
        // Simple Euclidean distance normalized to approximate ΔE; good enough for small
        // thresholds
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
        if (text == null)
            return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    private boolean equalsIgnoreWhitespace(String a, String b) {
        return normalizeText(a).equals(normalizeText(b));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String normalizeFontFamily(String family) {
        if (family == null)
            return "";
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

    private String normalizeToHex(String color) {
        if (color == null || color.isEmpty())
            return "";
        int[] rgb = parseCssColor(color);
        if (rgb == null)
            return color;
        return toHex(rgb);
    }
}
