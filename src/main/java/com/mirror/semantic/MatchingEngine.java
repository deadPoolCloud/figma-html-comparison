package com.mirror.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent matching engine to pair Figma nodes with HTML nodes.
 * Strategies:
 * 1. Exact Text Content
 * 2. Fuzzy Text Content (Levenshtein)
 * 3. Spatial Proximity (fallback)
 */
public class MatchingEngine {

    private final SemanticConfig config;

    public MatchingEngine() {
        this(SemanticConfig.DEFAULT);
    }

    public MatchingEngine(SemanticConfig config) {
        this.config = config;
    }

    public static class Match {
        public FigmaSemanticSnapshot.TextNode figma;
        public HtmlSemanticSnapshot.TextNode html;
        public double confidence;

        public Match(FigmaSemanticSnapshot.TextNode figma, HtmlSemanticSnapshot.TextNode html, double confidence) {
            this.figma = figma;
            this.html = html;
            this.confidence = confidence;
        }
    }

    public List<Match> matchTextNodes(List<FigmaSemanticSnapshot.TextNode> figmaNodes,
            List<HtmlSemanticSnapshot.TextNode> htmlNodes) {
        List<Match> matches = new ArrayList<>();
        List<FigmaSemanticSnapshot.TextNode> unmatchedFigma = new ArrayList<>(figmaNodes);
        List<HtmlSemanticSnapshot.TextNode> unmatchedHtml = new ArrayList<>(htmlNodes);

        // Multi-pass matching strategy

        // 1. High-confidence text matches
        matches.addAll(findMatches(unmatchedFigma, unmatchedHtml, 0.8));

        // 2. Medium-confidence matches (fuzzy text + spatial)
        matches.addAll(findMatches(unmatchedFigma, unmatchedHtml, 0.5));

        // 3. Low-confidence fallback (spatial only)
        matches.addAll(findMatches(unmatchedFigma, unmatchedHtml, 0.2));

        return matches;
    }

    private List<Match> findMatches(List<FigmaSemanticSnapshot.TextNode> figmaPool,
            List<HtmlSemanticSnapshot.TextNode> htmlPool, double minConfidence) {
        List<Match> found = new ArrayList<>();

        while (true) {
            Match bestMatch = null;
            double bestScore = -1.0;

            for (FigmaSemanticSnapshot.TextNode f : figmaPool) {
                for (HtmlSemanticSnapshot.TextNode h : htmlPool) {
                    double score = calculateMatchScore(f, h);
                    if (score >= minConfidence && score > bestScore) {
                        bestScore = score;
                        bestMatch = new Match(f, h, score);
                    }
                }
            }

            if (bestMatch != null) {
                found.add(bestMatch);
                figmaPool.remove(bestMatch.figma);
                htmlPool.remove(bestMatch.html);
            } else {
                break;
            }
        }

        return found;
    }

    private double calculateMatchScore(FigmaSemanticSnapshot.TextNode f, HtmlSemanticSnapshot.TextNode h) {
        double textScore = calculateTextScore(f.getText(), h.getText());
        double spatialScore = calculateSpatialScore(f, h);
        double typeScore = calculateTypeScore(f, h);

        double totalWeight = config.matchTextWeight + config.matchSpatialWeight + config.matchTypeWeight;

        return (textScore * config.matchTextWeight +
                spatialScore * config.matchSpatialWeight +
                typeScore * config.matchTypeWeight) / totalWeight;
    }

    private double calculateTextScore(String s1, String s2) {
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        if (n1.isEmpty() || n2.isEmpty())
            return 0;
        if (n1.equalsIgnoreCase(n2))
            return 1.0;

        // "Contains" match for handling partially split blocks
        if (n1.length() > 20 && n2.length() > 20) {
            if (n1.contains(n2) || n2.contains(n1))
                return 0.85;
        }

        int dist = levenshtein(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        double similarity = 1.0 - (double) dist / maxLen;

        return similarity > 0.65 ? similarity : 0; // Slightly lower threshold
    }

    private double calculateSpatialScore(FigmaSemanticSnapshot.TextNode f, HtmlSemanticSnapshot.TextNode h) {
        double dist = getDistance(f, h);
        if (dist > 800)
            return 0; // Increased tolerance for different layouts
        return Math.max(0, 1.0 - (dist / 800.0));
    }

    private double calculateTypeScore(FigmaSemanticSnapshot.TextNode f, HtmlSemanticSnapshot.TextNode h) {
        // Simple heuristics to match HTML tags to Figma meanings
        String tag = h.getTag() != null ? h.getTag().toLowerCase() : "";
        String fName = f.getName() != null ? f.getName().toLowerCase() : "";

        if (tag.matches("h[1-6]")) {
            if (fName.contains("title") || fName.contains("heading") || fName.contains("header"))
                return 1.0;
        }
        if (tag.equals("p") || tag.equals("span") || tag.equals("div")) {
            if (fName.contains("body") || fName.contains("text") || fName.contains("description")
                    || fName.contains("paragraph"))
                return 1.0;
        }
        if (tag.equals("a") || tag.equals("button") || tag.equals("li")) {
            if (fName.contains("link") || fName.contains("button") || fName.contains("cta") || fName.contains("item"))
                return 1.0;
        }

        return 0.5; // Neutral match if unknown
    }

    private double getDistance(FigmaSemanticSnapshot.TextNode f, HtmlSemanticSnapshot.TextNode h) {
        return Math.sqrt(Math.pow(f.getX() - h.getX(), 2) + Math.pow(f.getY() - h.getY(), 2));
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        return s.toLowerCase()
                .trim()
                .replaceAll("[\u2013\u2014]", "-") // Normalize smart dashes
                .replaceAll("[\u201c\u201d\u2018\u2019]", "'") // Normalize smart quotes
                .replaceAll("[^a-z0-9\\s]", "") // Remove punctuation for matching
                .replaceAll("\\s+", " ");
    }

    private int levenshtein(String s1, String s2) {
        // Simple Levenshtein implementation
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++)
            dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
