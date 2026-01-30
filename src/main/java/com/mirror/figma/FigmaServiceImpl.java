package com.mirror.figma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class FigmaServiceImpl implements FigmaService {

    private static final String FIGMA_TOKEN = System.getenv("FIGMA_TOKEN") != null
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

            HttpURLConnection conn = createConnection(apiUrl);
            JsonNode json = readResponse(conn);

            String imageUrl = json.get("images").get(frameId).asText();

            // ---------------- IMAGE DOWNLOAD ----------------
            BufferedImage image;
            try (InputStream imgStream = URI.create(imageUrl).toURL().openStream()) {
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

    @Override
    public JsonNode getStructure(String fileKey, String frameId) {
        if ("YOUR_FIGMA_TOKEN".equals(FIGMA_TOKEN)) {
            throw new RuntimeException("FIGMA_TOKEN not configured");
        }

        try {
            // ---------------- CACHE ----------------
            Path cachedFile = CACHE_DIR
                    .resolve(fileKey)
                    .resolve(frameId + ".json");

            if (Files.exists(cachedFile)) {
                System.out.println("Figma structure cache hit: " + cachedFile);
                return new ObjectMapper().readTree(cachedFile.toFile());
            }

            Files.createDirectories(cachedFile.getParent());

            // ---------------- API CALL ----------------
            // Fetch node data
            String apiUrl = "https://api.figma.com/v1/files/" + fileKey +
                    "/nodes?ids=" + frameId;

            HttpURLConnection conn = createConnection(apiUrl);
            JsonNode json = readResponse(conn);

            // Response is { nodes: { "ID": { document: ... } } }
            JsonNode nodes = json.get("nodes");
            if (nodes == null || nodes.isMissingNode()) {
                throw new RuntimeException(
                        "Figma API response is missing the 'nodes' object. Response: " + json.toString());
            }

            // Try to find the node. Users often input 1-4 instead of 1:4.
            JsonNode nodeResult = nodes.get(frameId);
            if (nodeResult == null) {
                // Try replacing - with : as Figma often uses : internal but - in URLs
                String altId = frameId.replace("-", ":");
                nodeResult = nodes.get(altId);
                if (nodeResult == null) {
                    // Still not found, list available keys to help user
                    StringBuilder available = new StringBuilder();
                    nodes.fieldNames().forEachRemaining(name -> available.append(name).append(", "));
                    throw new RuntimeException("Node ID '" + frameId + "' (or '" + altId
                            + "') not found in Figma response. Available nodes in this file: [" + available.toString()
                            + "]");
                }
            }

            JsonNode nodeData = nodeResult.get("document");
            if (nodeData == null || nodeData.isMissingNode()) {
                throw new RuntimeException("Node '" + frameId + "' found, but it has no 'document' data.");
            }

            // ---------------- SAVE CACHE ----------------
            new ObjectMapper().writeValue(cachedFile.toFile(), nodeData);
            System.out.println("Figma structure cached: " + cachedFile);

            return nodeData;

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch Figma structure", e);
        }
    }

    private HttpURLConnection createConnection(String urlStr) throws IOException {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Figma-Token", FIGMA_TOKEN);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        return conn;
    }

    private JsonNode readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        if (status == 429) {
            String retryAfter = conn.getHeaderField("Retry-After");
            int waitSeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 60;
            throw new RuntimeException("Figma rate limit hit. Retry after " + waitSeconds + " seconds.");
        }
        if (status == 403) {
            throw new RuntimeException(
                    "Figma API 403 Forbidden: Access denied. Please check your FIGMA_TOKEN permissions or if the File ID '"
                            + conn.getURL().toString() + "' is correct and accessible.");
        }
        if (status == 401) {
            throw new RuntimeException(
                    "Figma API 401 Unauthorized: Invalid FIGMA_TOKEN. Current token (first 5 chars): "
                            + (FIGMA_TOKEN != null && FIGMA_TOKEN.length() > 5 ? FIGMA_TOKEN.substring(0, 5) : "None"));
        }
        if (status != 200) {
            throw new RuntimeException("Figma API failed: HTTP " + status + " for URL: " + conn.getURL());
        }
        return new ObjectMapper().readTree(conn.getInputStream());
    }
}
