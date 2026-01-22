@echo off
REM Example usage script for Visual Regression Testing Tool (Windows)

REM Set your Figma token (or set as environment variable)
set FIGMA_TOKEN=your_figma_token_here

REM Example 1: Interactive mode
java -cp target\figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI

REM Example 2: Command-line arguments (Desktop viewport)
java -cp target\figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI ^
    "https://example.com/dashboard" ^
    "abc123def456" ^
    "789:123" ^
    "DESKTOP"

REM Example 3: Mobile viewport
java -cp target\figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI ^
    "https://example.com/dashboard" ^
    "abc123def456" ^
    "789:123" ^
    "MOBILE"

REM Example 4: Using REST API (after starting Spring Boot)
curl -X POST "http://localhost:8080/api/compare" ^
  -d "url=https://example.com/dashboard" ^
  -d "figmaFile=abc123def456" ^
  -d "figmaFrame=789:123" ^
  -d "viewport=DESKTOP"
