package com.mirror.figma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class FigmaServiceMock implements FigmaService {

    @Override
    public BufferedImage getFrame(String fileKey, String frameId) {
        try {
            System.out.println("=== MOCK FIGMA SERVICE (Using Local File) ===");
            System.out.println("File Key: " + fileKey + " (ignored)");
            System.out.println("Frame ID: " + frameId + " (ignored)");

            // Try multiple locations for the Figma image
            File figmaFile = new File("figma_frame.png");
            if (!figmaFile.exists()) {
                figmaFile = new File("debug_images/figma_design.png");
            }

            if (!figmaFile.exists()) {
                throw new RuntimeException(
                        "Figma image not found! Please ensure figma_frame.png exists in the project root.");
            }

            System.out.println("Loading from: " + figmaFile.getAbsolutePath());

            BufferedImage image = ImageIO.read(figmaFile);

            System.out.println("Figma design loaded: " + image.getWidth() + " x " + image.getHeight());
            System.out.println("==============================================");

            return image;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock Figma image: " + e.getMessage(), e);
        }
    }

    @Override
    public JsonNode getStructure(String fileKey, String frameId) {
        try {
            System.out.println("=== MOCK FIGMA SERVICE (Structure) ===");
            File structureFile = new File("figma_structure.json");
            if (!structureFile.exists()) {
                // Return empty or throw? For mock, let's warn.
                System.out.println("WARN: figma_structure.json not found for mock semantic analysis.");
                return null;
            }
            return new ObjectMapper().readTree(structureFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mock Figma structure", e);
        }
    }
}