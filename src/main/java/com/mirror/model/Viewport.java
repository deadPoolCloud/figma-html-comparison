package com.mirror.model;

/**
 * Viewport configurations for responsive testing
 */
public enum Viewport {
    DESKTOP("Desktop", 1920, 1080),
    TABLET("Tablet", 768, 1024),
    MOBILE("Mobile", 375, 667);

    private final String name;
    private final int width;
    private final int height;

    Viewport(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDimensionString() {
        return width + "x" + height;
    }
}
