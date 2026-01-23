package com.mirror.figma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class FigmaServiceImpl implements FigmaService {

    private static final String FIGMA_TOKEN =
            System.getenv("FIGMA_TOKEN") != null
                    ? System.getenv("FIGMA_TOKEN")
                    : "YOUR_FIGMA_TOKEN";

    private static final Path CACHE_DIR = Path.of("cache", "figma");

    @Override
    public BufferedImage getFrame(String fileKey, String frameId) {

        if ("YOUR_FIGMA_TOKEN".equals(FIGMA_TOKEN)) {
            throw new RuntimeException("FIGMA_TOKEN not configured");
        }

        try {
            // ---------------- CACHE ----------------
            Path cachedImage = CACHE_DIR
                    .resolve(fileKey)
                    .resolve(frameId + ".png");

            if (Files.exists(cachedImage)) {
                System.out.println("Figma cache hit: " + cachedImage);
                return ImageIO.read(cachedImage.toFile());
            }

            Files.createDirectories(cachedImage.getParent());

            // ---------------- API CALL ----------------
            String apiUrl = "https://api.figma.com/v1/images/" + fileKey +
                    "?ids=" + frameId + "&format=png";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Figma-Token", FIGMA_TOKEN);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);

            int status = conn.getResponseCode();

            // --------- RATE LIMIT HANDLING ---------
            if (status == 429) {
                String retryAfter = conn.getHeaderField("Retry-After");
                int waitSeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 60;

                throw new RuntimeException(
                        "Figma rate limit hit. Retry after " + waitSeconds + " seconds."
                );
            }

            if (status != 200) {
                throw new RuntimeException("Figma API failed: HTTP " + status);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(conn.getInputStream());

            String imageUrl = json.get("images").get(frameId).asText();

            // ---------------- IMAGE DOWNLOAD ----------------
            BufferedImage image;
            try (InputStream imgStream = new URL(imageUrl).openStream()) {
                image = ImageIO.read(imgStream);
            }

            // ---------------- SAVE CACHE ----------------
            ImageIO.write(image, "png", cachedImage.toFile());
            System.out.println("Figma image cached: " + cachedImage);

            return image;

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch Figma frame", e);
        }
    }
}
