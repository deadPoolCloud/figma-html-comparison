package com.mirror.figma;

import com.fasterxml.jackson.databind.JsonNode;

import java.awt.image.BufferedImage;

public interface FigmaService {
    BufferedImage getFrame(String fileKey, String frameId);

    JsonNode getStructure(String fileKey, String frameId);
}
