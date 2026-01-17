package com.mirror.image;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;

public class ImageAligner {

    public static BufferedImage align(BufferedImage figma, BufferedImage live) {
        try {
            // Resize live screenshot to match Figma frame
            return Thumbnails.of(live)
                    .size(figma.getWidth(), figma.getHeight())
                    .asBufferedImage();
        } catch (Exception e) {
            throw new RuntimeException("Failed to align images", e);
        }
    }
}
