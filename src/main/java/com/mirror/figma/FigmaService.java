package com.mirror.figma;

import java.awt.image.BufferedImage;

public interface FigmaService {
    BufferedImage getFrame(String fileKey, String frameId);
}
