package com.mirror.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parses the exported figma_structure.json file into a simplified semantic snapshot.
 * This focuses on:
 * - Frame dimensions
 * - Major sections (header, hero, features, CTAs, footer) based on layer names
 * - Ordered text nodes with typography information
 */
public class FigmaSemanticExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FigmaSemanticSnapshot loadFromFile(File figmaStructureFile) {
        try {
            // figma_structure.json may be UTF-16 encoded – normalize to UTF-8 string first
            byte[] raw = readAllBytes(figmaStructureFile);
            String content = new String(raw, detectEncoding(raw));

            JsonNode root = objectMapper.readTree(content);
            return fromJson(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Figma structure from " + figmaStructureFile, e);
        }
    }

    private byte[] readAllBytes(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            return in.readAllBytes();
        }
    }

    private java.nio.charset.Charset detectEncoding(byte[] data) {
        if (data.length >= 2) {
            // UTF-16 LE BOM 0xFF 0xFE, UTF-16 BE 0xFE 0xFF
            if ((data[0] == (byte) 0xFF && data[1] == (byte) 0xFE)
                    || (data[0] == (byte) 0xFE && data[1] == (byte) 0xFF)) {
                return StandardCharsets.UTF_16;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private FigmaSemanticSnapshot fromJson(JsonNode root) {
        FigmaSemanticSnapshot snapshot = new FigmaSemanticSnapshot();

        // Find the first FRAME node (e.g. "Home") and treat it as the main frame
        JsonNode frameNode = findFirstByType(root, "FRAME");
        if (frameNode != null) {
            JsonNode box = frameNode.get("absoluteBoundingBox");
            if (box != null) {
                snapshot.setFrameWidth((int) box.path("width").asDouble(0));
                snapshot.setFrameHeight((int) box.path("height").asDouble(0));
            }

            // Sections by name heuristics
            snapshot.getSections().setHeader(findSectionRect(frameNode, "header"));
            snapshot.getSections().setHero(findSectionRect(frameNode, "hero"));
            snapshot.getSections().setFeatures(findSectionRect(frameNode, "feature"));
            snapshot.getSections().setCtas(findSectionRect(frameNode, "cta"));
            snapshot.getSections().setFooter(findSectionRect(frameNode, "footer"));

            // Text nodes
            List<FigmaSemanticSnapshot.TextNode> textNodes = new ArrayList<>();
            collectTextNodes(frameNode, textNodes);
            textNodes.sort((a, b) -> {
                int cmpY = Double.compare(a.getY(), b.getY());
                return cmpY != 0 ? cmpY : Double.compare(a.getX(), b.getX());
            });
            snapshot.setTextNodes(textNodes);
        }

        return snapshot;
    }

    private JsonNode findFirstByType(JsonNode root, String type) {
        if (root == null) return null;
        if (type.equalsIgnoreCase(root.path("type").asText())) {
            return root;
        }
        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                JsonNode found = findFirstByType(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private FigmaSemanticSnapshot.Rect findSectionRect(JsonNode frameNode, String nameContains) {
        JsonNode match = findByName(frameNode, nameContains);
        if (match == null) {
            return null;
        }
        JsonNode box = match.get("absoluteBoundingBox");
        if (box == null) {
            return null;
        }
        FigmaSemanticSnapshot.Rect rect = new FigmaSemanticSnapshot.Rect();
        rect.setX(box.path("x").asDouble(0));
        rect.setY(box.path("y").asDouble(0));
        rect.setWidth(box.path("width").asDouble(0));
        rect.setHeight(box.path("height").asDouble(0));
        return rect;
    }

    private JsonNode findByName(JsonNode root, String nameContains) {
        String lower = nameContains.toLowerCase();
        if (root.hasNonNull("name")) {
            String name = root.get("name").asText("").toLowerCase();
            if (name.contains(lower)) {
                return root;
            }
        }
        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                JsonNode found = findByName(child, nameContains);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void collectTextNodes(JsonNode root, List<FigmaSemanticSnapshot.TextNode> out) {
        if (root == null) return;
        if ("TEXT".equalsIgnoreCase(root.path("type").asText())) {
            FigmaSemanticSnapshot.TextNode tn = new FigmaSemanticSnapshot.TextNode();
            tn.setId(root.path("id").asText(null));
            tn.setName(root.path("name").asText(null));
            tn.setText(root.path("characters").asText(null));

            JsonNode box = root.get("absoluteBoundingBox");
            if (box != null) {
                tn.setX(box.path("x").asDouble(0));
                tn.setY(box.path("y").asDouble(0));
            }

            JsonNode style = root.get("style");
            if (style != null) {
                if (style.isObject()) {
                    // Standard Figma REST shape
                    tn.setFontFamily(style.path("fontFamily").asText(null));
                    tn.setFontSize(style.path("fontSize").asDouble(0));
                    tn.setFontWeight(style.path("fontWeight").asText(null));
                    // Prefer pixel line height if present
                    if (style.has("lineHeightPx")) {
                        tn.setLineHeight(style.path("lineHeightPx").asDouble(0));
                    } else if (style.has("lineHeightPercentFontSize")) {
                        double percent = style.path("lineHeightPercentFontSize").asDouble(0);
                        double size = tn.getFontSize();
                        tn.setLineHeight(size > 0 ? size * percent / 100.0 : 0);
                    }
                    tn.setLetterSpacing(style.path("letterSpacing").asDouble(0));
                } else if (style.isTextual()) {
                    // Your figma_structure.json encodes style as a serialized map string, e.g.:
                    // "@{fontFamily=Montserrat; fontSize=16.0; fontWeight=400; lineHeightPx=19.5; letterSpacing=0.0}"
                    applyStyleString(style.asText(), tn);
                }
            }

            JsonNode fills = root.get("fills");
            if (fills != null && fills.isArray()) {
                Iterator<JsonNode> it = fills.elements();
                while (it.hasNext()) {
                    JsonNode f = it.next();
                    if (!"SOLID".equalsIgnoreCase(f.path("type").asText())) continue;
                    JsonNode color = f.get("color");
                    if (color != null) {
                        double r = color.path("r").asDouble(0);
                        double g = color.path("g").asDouble(0);
                        double b = color.path("b").asDouble(0);
                        tn.setColor(rgbToHex(r, g, b));
                        break;
                    }
                }
            }

            out.add(tn);
        }

        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectTextNodes(child, out);
            }
        }
    }

    private String rgbToHex(double r, double g, double b) {
        int ri = clamp((int) Math.round(r * 255.0), 0, 255);
        int gi = clamp((int) Math.round(g * 255.0), 0, 255);
        int bi = clamp((int) Math.round(b * 255.0), 0, 255);
        return String.format("#%02X%02X%02X", ri, gi, bi);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Parses Figma desktop-export style strings like:
     * "@{fontFamily=Montserrat; fontPostScriptName=Montserrat-Regular; fontStyle=Regular; fontWeight=400; fontSize=16.0; lineHeightPx=19.5; letterSpacing=0.0}"
     * into the TextNode typography fields.
     */
    private void applyStyleString(String styleString, FigmaSemanticSnapshot.TextNode tn) {
        if (styleString == null) return;
        String s = styleString.trim();
        if (s.startsWith("@{") && s.endsWith("}")) {
            s = s.substring(2, s.length() - 1);
        }
        if (s.isEmpty()) return;

        String[] parts = s.split(";");
        String fontFamily = null;
        String fontWeight = null;
        Double fontSize = null;
        Double lineHeightPx = null;
        Double lineHeightPercent = null;
        Double letterSpacing = null;

        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String key = p.substring(0, eq).trim();
            String value = p.substring(eq + 1).trim();

            switch (key) {
                case "fontFamily":
                    fontFamily = value;
                    break;
                case "fontWeight":
                    fontWeight = value;
                    break;
                case "fontSize":
                    fontSize = parseDoubleSafe(value);
                    break;
                case "lineHeightPx":
                    lineHeightPx = parseDoubleSafe(value);
                    break;
                case "lineHeightPercent":
                case "lineHeightPercentFontSize":
                    lineHeightPercent = parseDoubleSafe(value);
                    break;
                case "letterSpacing":
                    letterSpacing = parseDoubleSafe(value);
                    break;
                default:
                    // ignore others
                    break;
            }
        }

        if (fontFamily != null) {
            tn.setFontFamily(fontFamily);
        }
        if (fontWeight != null) {
            tn.setFontWeight(fontWeight);
        }
        if (fontSize != null) {
            tn.setFontSize(fontSize);
        }
        if (letterSpacing != null) {
            tn.setLetterSpacing(letterSpacing);
        }

        // Prefer absolute line height if available; otherwise compute from percent of font size
        if (lineHeightPx != null) {
            tn.setLineHeight(lineHeightPx);
        } else if (lineHeightPercent != null && fontSize != null && fontSize > 0) {
            tn.setLineHeight(fontSize * lineHeightPercent / 100.0);
        }
    }

    private Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

