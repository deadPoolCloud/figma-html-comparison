package com.mirror.semantic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified semantic representation of the Figma frame,
 * derived from the exported figma_structure.json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FigmaSemanticSnapshot {

    @JsonProperty("frame_width")
    private int frameWidth;

    @JsonProperty("frame_height")
    private int frameHeight;

    @JsonProperty("sections")
    private Sections sections = new Sections();

    @JsonProperty("text_nodes")
    private List<TextNode> textNodes = new ArrayList<>();

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
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

        @JsonProperty("padding_left")
        private double paddingLeft;
        @JsonProperty("padding_right")
        private double paddingRight;
        @JsonProperty("padding_top")
        private double paddingTop;
        @JsonProperty("padding_bottom")
        private double paddingBottom;
        @JsonProperty("item_spacing")
        private double itemSpacing;

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

        public double getPaddingLeft() {
            return paddingLeft;
        }

        public void setPaddingLeft(double paddingLeft) {
            this.paddingLeft = paddingLeft;
        }

        public double getPaddingRight() {
            return paddingRight;
        }

        public void setPaddingRight(double paddingRight) {
            this.paddingRight = paddingRight;
        }

        public double getPaddingTop() {
            return paddingTop;
        }

        public void setPaddingTop(double paddingTop) {
            this.paddingTop = paddingTop;
        }

        public double getPaddingBottom() {
            return paddingBottom;
        }

        public void setPaddingBottom(double paddingBottom) {
            this.paddingBottom = paddingBottom;
        }

        public double getItemSpacing() {
            return itemSpacing;
        }

        public void setItemSpacing(double itemSpacing) {
            this.itemSpacing = itemSpacing;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextNode {

        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

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

        @JsonProperty("type")
        private String type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @JsonProperty("interactive_nodes")
    private List<InteractiveNode> interactiveNodes = new ArrayList<>();

    public List<InteractiveNode> getInteractiveNodes() {
        return interactiveNodes;
    }

    public void setInteractiveNodes(List<InteractiveNode> interactiveNodes) {
        this.interactiveNodes = interactiveNodes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InteractiveNode {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("text")
        private String text;

        @JsonProperty("rect")
        private Rect rect;

        @JsonProperty("background_color")
        private String backgroundColor;

        @JsonProperty("corner_radius")
        private double cornerRadius;

        @JsonProperty("parent_id")
        private String parentId;

        @JsonProperty("type")
        private String type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
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

        public double getCornerRadius() {
            return cornerRadius;
        }

        public void setCornerRadius(double cornerRadius) {
            this.cornerRadius = cornerRadius;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
