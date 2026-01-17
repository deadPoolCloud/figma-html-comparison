package com.mirror.figma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FigmaServiceImpl implements FigmaService {

    // Get Figma token from environment variable or use default
    // Set FIGMA_TOKEN environment variable: export FIGMA_TOKEN=your_token_here
    private static final String FIGMA_TOKEN = System.getenv("FIGMA_TOKEN") != null ? 
            System.getenv("FIGMA_TOKEN") : "YOUR_FIGMA_TOKEN";

    @Override
    public BufferedImage getFrame(String fileKey, String frameId) {
        
        if (FIGMA_TOKEN.equals("YOUR_FIGMA_TOKEN")) {
            throw new RuntimeException(
                "Figma token not configured! " +
                "Please set FIGMA_TOKEN environment variable or update FigmaServiceImpl.java"
            );
        }

        try {
            // Step 1: Get image URL from Figma
            String apiUrl = "https://api.figma.com/v1/images/" + fileKey +
                    "?ids=" + frameId + "&format=png";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("X-Figma-Token", FIGMA_TOKEN);
            conn.setRequestMethod("GET");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(conn.getInputStream());

            String imageUrl = json.get("images").get(frameId).asText();

            // Step 2: Download the PNG
            InputStream imgStream = new URL(imageUrl).openStream();
            return ImageIO.read(imgStream);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Figma frame", e);
        }
    }
}
