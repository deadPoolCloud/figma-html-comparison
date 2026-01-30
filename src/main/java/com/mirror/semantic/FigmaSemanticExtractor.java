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
 * Parses the Figma Node Tree (JSON) into a semantic snapshot.
 */
public class FigmaSemanticExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FigmaSemanticSnapshot loadFromFile(File figmaStructureFile) {
        try {
            // Support legacy file loading for mock/debug
            byte[] raw = readAllBytes(figmaStructureFile);
            String content = new String(raw, detectEncoding(raw));
            JsonNode root = objectMapper.readTree(content);
            return extract(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Figma structure from " + figmaStructureFile, e);
        }
    }

    public FigmaSemanticSnapshot extract(JsonNode root) {
        FigmaSemanticSnapshot snapshot = new FigmaSemanticSnapshot();

        // Handle /nodes endpoint response wrapper
        if (root.has("nodes")) {
            JsonNode nodes = root.get("nodes");
            if (nodes.isObject()) {
                Iterator<JsonNode> elements = nodes.elements();
                if (elements.hasNext()) {
                    JsonNode nodeData = elements.next();
                    if (nodeData.has("document")) {
                        root = nodeData.get("document");
                    }
                }
            }
        }

        // Find the first FRAME/COMPONENT/INSTANCE node to treat as the main frame
        JsonNode frameNode = findFirstFrame(root);
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

            // Interactive nodes
            List<FigmaSemanticSnapshot.InteractiveNode> interactiveNodes = new ArrayList<>();
            collectInteractiveNodes(frameNode, interactiveNodes);
            snapshot.setInteractiveNodes(interactiveNodes);
        }

        return snapshot;
    }

    private byte[] readAllBytes(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            return in.readAllBytes();
        }
    }

    private java.nio.charset.Charset detectEncoding(byte[] data) {
        if (data.length >= 2) {
            if ((data[0] == (byte) 0xFF && data[1] == (byte) 0xFE)
                    || (data[0] == (byte) 0xFE && data[1] == (byte) 0xFF)) {
                return StandardCharsets.UTF_16;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private JsonNode findFirstFrame(JsonNode root) {
        if (root == null)
            return null;
        String type = root.path("type").asText("");
        if ("FRAME".equals(type) || "COMPONENT".equals(type) || "INSTANCE".equals(type)) {
            return root;
        }
        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                JsonNode found = findFirstFrame(child);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private FigmaSemanticSnapshot.Rect findSectionRect(JsonNode frameNode, String nameContains) {
        JsonNode match = findByName(frameNode, nameContains);
        if (match == null)
            return null;
        JsonNode box = match.get("absoluteBoundingBox");
        if (box == null)
            return null;

        FigmaSemanticSnapshot.Rect rect = new FigmaSemanticSnapshot.Rect();
        rect.setX(box.path("x").asDouble(0));
        rect.setY(box.path("y").asDouble(0));
        rect.setWidth(box.path("width").asDouble(0));
        rect.setHeight(box.path("height").asDouble(0));

        rect.setPaddingLeft(match.path("paddingLeft").asDouble(0));
        rect.setPaddingRight(match.path("paddingRight").asDouble(0));
        rect.setPaddingTop(match.path("paddingTop").asDouble(0));
        rect.setPaddingBottom(match.path("paddingBottom").asDouble(0));
        rect.setItemSpacing(match.path("itemSpacing").asDouble(0));
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
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void collectTextNodes(JsonNode root, List<FigmaSemanticSnapshot.TextNode> out) {
        collectTextNodes(root, out, null);
    }

    private void collectTextNodes(JsonNode root, List<FigmaSemanticSnapshot.TextNode> out, String parentId) {
        if (root == null)
            return;

        String id = root.path("id").asText();
        String type = root.path("type").asText();

        if ("TEXT".equalsIgnoreCase(type)) {
            if (root.path("visible").asBoolean(true)) {
                FigmaSemanticSnapshot.TextNode tn = new FigmaSemanticSnapshot.TextNode();
                tn.setId(id);
                tn.setName(root.path("name").asText());
                tn.setText(root.path("characters").asText());
                tn.setParentId(parentId);
                tn.setType(type);

                JsonNode box = root.get("absoluteBoundingBox");
                if (box != null) {
                    tn.setX(box.path("x").asDouble(0));
                    tn.setY(box.path("y").asDouble(0));
                }

                JsonNode style = root.get("style");
                if (style != null) {
                    tn.setFontFamily(style.path("fontFamily").asText());
                    tn.setFontWeight(String.valueOf(style.path("fontWeight").asInt(400)));
                    tn.setFontSize(style.path("fontSize").asDouble(16));
                    tn.setLetterSpacing(style.path("letterSpacing").asDouble(0));

                    if (style.has("lineHeightPx")) {
                        tn.setLineHeight(style.path("lineHeightPx").asDouble());
                    } else if (style.has("lineHeightPercentFontSize")) {
                        tn.setLineHeight(tn.getFontSize() * style.path("lineHeightPercentFontSize").asDouble() / 100.0);
                    }
                }

                // Color from fills
                tn.setColor(extractColorFromFills(root.get("fills")));

                out.add(tn);
            }
        }

        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectTextNodes(child, out, id);
            }
        }
    }

    private void collectInteractiveNodes(JsonNode root, List<FigmaSemanticSnapshot.InteractiveNode> out) {
        collectInteractiveNodes(root, out, null);
    }

    private void collectInteractiveNodes(JsonNode root, List<FigmaSemanticSnapshot.InteractiveNode> out,
            String parentId) {
        if (root == null)
            return;

        String id = root.path("id").asText();

        if (isInteractiveCandidate(root)) {
            FigmaSemanticSnapshot.InteractiveNode node = extractInteractiveNode(root, parentId);
            if (node != null) {
                out.add(node);
            }
        }

        JsonNode children = root.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectInteractiveNodes(child, out, id);
            }
        }
    }

    private boolean isInteractiveCandidate(JsonNode node) {
        String type = node.path("type").asText("");
        return "FRAME".equals(type) || "GROUP".equals(type) || "INSTANCE".equals(type) || "COMPONENT".equals(type);
    }

    private FigmaSemanticSnapshot.InteractiveNode extractInteractiveNode(JsonNode node, String parentId) {
        String textLabel = findFirstTextContent(node);
        if (textLabel == null || textLabel.isEmpty()) {
            return null;
        }

        FigmaSemanticSnapshot.InteractiveNode in = new FigmaSemanticSnapshot.InteractiveNode();
        in.setId(node.path("id").asText());
        in.setName(node.path("name").asText());
        in.setText(textLabel);
        in.setParentId(parentId);
        in.setType(node.path("type").asText());

        JsonNode box = node.get("absoluteBoundingBox");
        if (box != null) {
            FigmaSemanticSnapshot.Rect r = new FigmaSemanticSnapshot.Rect();
            r.setX(box.path("x").asDouble(0));
            r.setY(box.path("y").asDouble(0));
            r.setWidth(box.path("width").asDouble(0));
            r.setHeight(box.path("height").asDouble(0));

            r.setPaddingLeft(node.path("paddingLeft").asDouble(0));
            r.setPaddingRight(node.path("paddingRight").asDouble(0));
            r.setPaddingTop(node.path("paddingTop").asDouble(0));
            r.setPaddingBottom(node.path("paddingBottom").asDouble(0));
            r.setItemSpacing(node.path("itemSpacing").asDouble(0));

            in.setRect(r);
        }

        in.setBackgroundColor(extractBackgroundColor(node));
        in.setCornerRadius(node.path("cornerRadius").asDouble(0));
        return in;
    }

    private String findFirstTextContent(JsonNode node) {
        if ("TEXT".equalsIgnoreCase(node.path("type").asText())) {
            return node.path("characters").asText("");
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                String text = findFirstTextContent(child);
                if (text != null && !text.isEmpty())
                    return text;
            }
        }
        return null;
    }

    private String extractBackgroundColor(JsonNode node) {
        String color = extractColorFromFills(node.get("fills"));
        if (color != null)
            return color;

        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                String type = child.path("type").asText("");
                if ("RECTANGLE".equals(type) || "ELLIPSE".equals(type)) {
                    String bg = extractColorFromFills(child.get("fills"));
                    if (bg != null)
                        return bg;
                }
            }
        }
        return null;
    }

    private String extractColorFromFills(JsonNode fills) {
        if (fills != null && fills.isArray()) {
            for (JsonNode f : fills) {
                if ("SOLID".equalsIgnoreCase(f.path("type").asText()) && f.path("visible").asBoolean(true)) {
                    JsonNode c = f.get("color");
                    if (c != null) {
                        return rgbToHex(c.path("r").asDouble(), c.path("g").asDouble(), c.path("b").asDouble());
                    }
                }
            }
        }
        return null;
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
}
