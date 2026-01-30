# 🎨 Figma HTML Visual & Semantic Diff Framework

A production-grade Visual Regression Testing Tool that compares approved Figma designs with implemented HTML/CSS web pages. It goes beyond pixel diffs by performing **deep semantic analysis** directly from the Figma Node Tree.

## 📋 Overview

This tool automatically detects both matching logic and pixel-perfect accuracy defects between design and implementation:

1.  **Visual Diff**: Pixel-to-pixel image comparison using OpenCV.
2.  **Semantic Diff**: Structural analysis of Layout, Typography, Spacing, and Colors.

**New in v2:**
*   **Playwright Engine**: Replaces Selenium for robust, full-page rendering and accurate DOM extraction.
*   **Smart Matching**: Uses a multi-strategy engine (Exact Text > Fuzzy Text > Spatial) to pair semantic elements.
*   **Deep Figma Integration**: Fetches the full design node tree via Figma Rest API (no manual JSON exports required).

## 🎯 Key Features

✅ **Figma API Integration** - Fetches both visual renders (PNG) and **semantic node trees (JSON)** directly.
✅ **Smart Element Matching** - Intelligently pairs Figma text nodes with HTML elements using content analysis.
✅ **Playwright Capture** - Accurate, full-page screenshots with lazy-loading support and network idle detection.
✅ **Responsive Testing** - Configurable viewports (Desktop, Tablet, Mobile).
✅ **Hybrid Comparison** - Combines visual heatmaps with structural data (e.g., "Font size mismatch: 16px vs 14px").
✅ **Local Cache Strategy** - Caches Figma API responses to optimize performance and avoid rate limits.
✅ **QA-Friendly Reporting** - Generates HTML reports with side-by-side visual and semantic insights.
✅ **CI/CD Ready** - Deployable as a Spring Boot JAR or Docker container.

## 📦 Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.8+**
- **Figma Personal Access Token** ([Get one here](https://www.figma.com/developers/api#access-tokens))

*Note: Playwright will automatically download the necessary browser binaries (Chromium) on first run.*

## 🚀 How to Run

### 1. Build the Framework
Ensure you have **Java 17+** and **Maven** installed. Build the project using:
```bash
mvn clean package
```

### 2. Set Figma Token
The tool requires a Figma API token to fetch designs. Set it as an environment variable:
- **Windows (PowerShell)**: `$env:FIGMA_TOKEN = "your_token"`
- **Linux/Mac**: `export FIGMA_TOKEN=your_token`

---

### 3. Option A: Run via Command Line (CLI)
The CLI supports interactive and direct argument modes.

#### Interactive Mode (easiest)
Simply run the JAR without arguments:
```bash
java -jar target/figma-html-visual-mirror-1.0.0.jar
```
Follow the prompts to enter your **URL**, **Figma File ID**, and **Node ID**.

#### Direct Arguments Mode
For automation or scripts, provide arguments in this order:
`java -jar <jar> <url> <figmaFileId> <figmaNodeId> [viewport] [mode]`
- `viewport`: `DESKTOP`, `TABLET`, or `MOBILE`
- `mode`: `ALL` (Pixel + Semantic) or `SEMANTIC` (Fast logic only)

**Example:**
```bash
java -jar target/figma-html-visual-mirror-1.0.0.jar https://google.com kaDcp2VTNV8RL0G58l3bwO 68:108 DESKTOP ALL
```

---

### 4. Option B: Run as a REST API
Start the application in server mode (default when no CLI args are passed, if not using the Main wrapper):
```bash
java -jar target/figma-html-visual-mirror-1.0.0.jar
```
*Note: If your JAR is configured to run CLI by default, use `mvn spring-boot:run` to start the server.*

The API will be available at `http://localhost:8080/api/compare`.

**POST Request Example:**
```bash
curl -X POST "http://localhost:8080/api/compare" \
     -d "url=https://your-site.com" \
     -d "figmaFile=kaDcp2..." \
     -d "figmaFrame=68:108" \
     -d "viewport=DESKTOP" \
     -d "semanticOnly=false"
```

---

### 5. View Results
After a run, check the `reports/` folder:
- **`report_YYYYMMDD_HHMMSS.html`**: A visual QA report with heatmaps and side-by-side typography audit.
- **`semantic_report_YYYYMMDD_HHMMSS.json`**: A machine-readable JSON file for CI/CD failures.
- **`visual_report_YYYYMMDD_HHMMSS.json`**: Metadata summary of visual discrepancies.


## 🧠 How it Works (The Simple Version)

Imagine you are checking a shopping receipt against what's actually in your bags. This tool does the same for your website:

1.  **The "Wishlist" (Figma)**: We look at the Figma design as a digital blueprint. We extract exactly what should be there: specific colors, font sizes, and exact words.
2.  **The "Reality" (Your Website)**: We open your website in a real browser and "scan" every single element. We don't just look at it; we measure it down to the pixel.
3.  **The "Smart Match"**: The tool is smart enough to know that if the design says "Sign Up" and the website says "Sign up", they are likely the same thing. It "pairs" them together so it can compare them.
4.  **The Audit Report**: Finally, it tells you:
    *   ✅ **Match**: Perfect! Code matches Design.
    *   ⚠️ **Drift**: It's the right text/font, but slightly the wrong size or spacing.
    *   ❌ **Mismatch**: Something is wrong (e.g. wrong color or font used).
    *   🛑 **Missing**: You forgot to build something that was in the design!

## 🛠️ Under the Hood: Data & APIs

For those who want to know exactly what’s happening behind the scenes:

### 1. The Figma Extraction
*   **API Used**: `https://api.figma.com/v1/files/{file_key}/nodes?ids={frame_id}`
Header required: X-Figma-Token: your_figma_token

*   **Format**: The tool receives a massive JSON tree from Figma.
*   **What we extract**: We filter out all the "design layers" and keep only the **Semantic Data**:
    *   Raw content (strings)
    *   Typography properties (font family, weight, size, line-height)
    *   Exact X/Y coordinates
    *   Brand colors

### 2. The Website Extraction (Live Data)
*   **Form**: We use **Playwright** to run a specialized JavaScript "crawler" inside your live website.
*   **What we get**:
    *   **Computed Styles**: This is the most important part. We don't look at your CSS files; we ask the browser: *"What is the final, rendered font-size and color the user is seeing right now?"*
    *   **Bounding Boxes**: We get the exact pixel location of every word and button on the screen.

### 3. The Comparison Logic
*   **What is a "Snapshot"?**: Think of a snapshot as a **"Data Map"** or a spreadsheet of your UI. It’s not an image; it’s a list of every element, where it is, what it says, and how it’s styled.
    *   **Figma Snapshot**: A map of the "Design Intent."
    *   **HTML Snapshot**: A map of the "Final Implementation."
*   **The Match**: The tool then compares these two maps side-by-side to find discrepancies.

---

## 📉 Why Mismatches Happen & Current Drawbacks

If you are seeing a high number of mismatches despite the site looking "correct," here are the common reasons and current technical limitations:

### 1. The "Invisible Element" Problem
Modern websites use many "wrapper" elements (like extra `divs` for layout or hidden menus). Sometimes our capture script sees these extra layers as "Extra Elements" because they don't exist in Figma's simpler design structure.

### 2. Punctuation & Micro-Text
The tool is very literal. If Figma has a "smart quote" (`“`) but the website uses a standard quote (`"`), it may flag a mismatch. We are constantly improving our "Normalization" to ignore these minor differences.

### 3. Animation Timing (The biggest drawback)
If your website has fade-in animations or "Slide up on scroll" effects, the tool might try to capture the element while it is still invisible or moving. Even though the tool waits for the page to settle, complex JavaScript animations can occasionally cause intermittent "Missing" or "Drift" reports.

### 4. Coordinate Shifts
Figma uses a fixed, absolute canvas. HTML coordinates change based on screen size, scroll position, and browser zoom. While we use "Relative Proximity" to match elements, a high-density layout can sometimes cause a "Misalignment" flag if the website layout shifted by even a few pixels to accommodate browser scrollbars.

---

## 🎛️ Configuration

### Viewport Sizes
Modify `src/main/java/com/mirror/model/Viewport.java` to adjust standard sizes:
- **Desktop**: 1440 × 900
- **Tablet**: 768 × 1024
- **Mobile**: 375 × 667

### Configuration Classes
- **Capture**: `PlaywrightCaptureService` (Wait times, Scroll logic).
- **Matching**: `MatchingEngine` (Fuzzy thresholds, Max spatial distance).
- **Semantics**: `SemanticAnalyzer` (Tolerance thresholds for pixels, colors).

## 📁 Project Structure

```
src/main/java/com/mirror/
├── capture/
│   ├── PlaywrightCaptureService.java  # [NEW] Comparison engine (Visual + Semantic)
│   └── WebCaptureService.java         # Interface
├── figma/
│   ├── FigmaService.java
│   ├── FigmaServiceImpl.java          # [UPDATED] Fetches Images + Node Structure
│   └── FigmaServiceMock.java          # Local file fallback
├── semantic/
│   ├── MatchingEngine.java            # [NEW] Smart Element Pairing Logic
│   ├── SemanticAnalyzer.java          # Core comparison logic
│   ├── FigmaSemanticExtractor.java    # [UPDATED] Parses API JSON
│   ├── HtmlSemanticSnapshot.java      # HTML Data Model
│   └── FigmaSemanticSnapshot.java     # Figma Data Model
├── image/
│   ├── OpenCvDiffEngine.java          # Pixel-to-pixel comparison
│   └── ImageAligner.java              # Dimension alignment
├── orchestrator/
│   └── ComparisonOrchestrator.java    # Workflow Coordinator
└── report/
    └── HtmlReportService.java         # HTML Report Generator
```

## 🐛 working with Rate Limits

The `FigmaServiceImpl` includes automatic rate limit handling. If the API returns a `429`, the service will throw a nice exception with the `Retry-After` duration.

To avoid hitting limits during development:
1.  Run one test to fetch the data.
2.  The service automatically caches images and JSON to `cache/figma/`.
3.  Subsequent runs use the cache. Delete the `cache/` folder to force a refresh.

## 🤝 Contributing

Contributions are welcome! Please ensure you verify changes with `mvn test` before submitting a Pull Request.
