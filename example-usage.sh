#!/bin/bash
# Example usage script for Visual Regression Testing Tool

# Set your Figma token (or export as environment variable)
export FIGMA_TOKEN="your_figma_token_here"

# Example 1: Interactive mode
java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI

# Example 2: Command-line arguments (Desktop viewport)
java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI \
    "https://example.com/dashboard" \
    "abc123def456" \
    "789:123" \
    "DESKTOP"

# Example 3: Mobile viewport
java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI \
    "https://example.com/dashboard" \
    "abc123def456" \
    "789:123" \
    "MOBILE"

# Example 4: Using REST API (after starting Spring Boot)
curl -X POST "http://localhost:8080/api/compare" \
  -d "url=https://example.com/dashboard" \
  -d "figmaFile=abc123def456" \
  -d "figmaFrame=789:123" \
  -d "viewport=DESKTOP"
