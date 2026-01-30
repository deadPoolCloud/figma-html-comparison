package com.mirror.semantic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured snapshot of the rendered HTML page used for semantic comparison.
 * Populated from the browser via Selenium + JavaScript.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HtmlSemanticSnapshot {

    @JsonProperty("viewport_width")
    private int viewportWidth;

    @JsonProperty("viewport_height")
    private int viewportHeight;

    @JsonProperty("document_width")
    private int documentWidth;

    @JsonProperty("document_height")
    private int documentHeight;

    @JsonProperty("sections")
    private Sections sections = new Sections();

    @JsonProperty("text_nodes")
    private List<TextNode> textNodes = new ArrayList<>();

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public int getDocumentWidth() {
        return documentWidth;
    }

    public void setDocumentWidth(int documentWidth) {
        this.documentWidth = documentWidth;
    }

    public int getDocumentHeight() {
        return documentHeight;
    }

    public void setDocumentHeight(int documentHeight) {
        this.documentHeight = documentHeight;
    }

    public Sections getSections() {
        return sections;
    }

    public void setSections(Sections sections) {
        this.sections = sections;
    }

    public List<TextNode> getTextNodes() {
        return textNodes;
    }

    public void setTextNodes(List<TextNode> textNodes) {
        this.textNodes = textNodes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Sections {

        @JsonProperty("header")
        private Rect header;

        @JsonProperty("hero")
        private Rect hero;

        @JsonProperty("features")
        private Rect features;

        @JsonProperty("ctas")
        private Rect ctas;

        @JsonProperty("footer")
        private Rect footer;

        public Rect getHeader() {
            return header;
        }

        public void setHeader(Rect header) {
            this.header = header;
        }

        public Rect getHero() {
            return hero;
        }

        public void setHero(Rect hero) {
            this.hero = hero;
        }

        public Rect getFeatures() {
            return features;
        }

        public void setFeatures(Rect features) {
            this.features = features;
        }

        public Rect getCtas() {
            return ctas;
        }

        public void setCtas(Rect ctas) {
            this.ctas = ctas;
        }

        public Rect getFooter() {
            return footer;
        }

        public void setFooter(Rect footer) {
            this.footer = footer;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Rect {

        @JsonProperty("x")
        private double x;

        @JsonProperty("y")
        private double y;

        @JsonProperty("width")
        private double width;

        @JsonProperty("height")
        private double height;

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }
    }

    @JsonProperty("interactive_elements")
    private List<InteractiveElement> interactiveElements = new ArrayList<>();

    public List<InteractiveElement> getInteractiveElements() {
        return interactiveElements;
    }

    public void setInteractiveElements(List<InteractiveElement> interactiveElements) {
        this.interactiveElements = interactiveElements;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InteractiveElement {
        @JsonProperty("text")
        private String text;

        @JsonProperty("tag")
        private String tag;

        @JsonProperty("rect")
        private Rect rect;

        @JsonProperty("background_color")
        private String backgroundColor;

        @JsonProperty("border_radius")
        private String borderRadius;

        @JsonProperty("padding")
        private String padding;

        @JsonProperty("color")
        private String color;

        @JsonProperty("parent_id")
        private String parentId;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public Rect getRect() {
            return rect;
        }

        public void setRect(Rect rect) {
            this.rect = rect;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public String getBorderRadius() {
            return borderRadius;
        }

        public void setBorderRadius(String borderRadius) {
            this.borderRadius = borderRadius;
        }

        public String getPadding() {
            return padding;
        }

        public void setPadding(String padding) {
            this.padding = padding;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextNode {

        @JsonProperty("id")
        private String id;

        @JsonProperty("tag")
        private String tag;

        @JsonProperty("text")
        private String text;

        @JsonProperty("x")
        private double x;

        @JsonProperty("y")
        private double y;

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

        @JsonProperty("parent_id")
        private String parentId;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
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

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }
    }
}
