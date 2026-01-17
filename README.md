# 🎨 Figma HTML Visual Regression Testing Tool

A QA-friendly Java-based Visual Regression Testing Tool that compares approved Figma UI designs with implemented HTML/CSS web pages at a pixel-to-pixel level.

## 📋 Overview

This tool helps QA testers and designers easily identify UI mismatches between Figma designs and live web pages by:
- Fetching design screens from Figma using Figma REST APIs
- Capturing full-page screenshots of HTML pages using Selenium WebDriver
- Performing pixel-to-pixel image comparison using OpenCV
- Classifying visual differences (alignment, spacing, font, color, missing elements)
- Generating comprehensive HTML reports with clear observations and severity levels

## 🎯 Key Features

✅ **Figma Integration** - Direct integration with Figma REST API  
✅ **Responsive Testing** - Support for Desktop, Tablet, and Mobile viewports  
✅ **Pixel-to-Pixel Comparison** - Accurate visual difference detection  
✅ **Issue Classification** - Automatic categorization of visual issues  
✅ **QA-Friendly Reports** - HTML reports with observations like "Button padding differs by ~6px"  
✅ **Severity Levels** - Minor, Major, and Critical classifications  
✅ **CLI & REST API** - Command-line interface and REST API support  
✅ **CI/CD Ready** - Easy integration into automated pipelines  

## 📦 Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+**
- **Chrome/Chromium** browser (for Selenium WebDriver)
- **Figma Personal Access Token** ([How to get one](https://www.figma.com/developers/api#access-tokens))
- **ChromeDriver** (automatically managed by Selenium 4.17+)

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd figma-html-visual-mirror
mvn clean install
```

### 2. Set Figma Token

Set your Figma personal access token as an environment variable:

**Windows (PowerShell):**
```powershell
$env:FIGMA_TOKEN="your_figma_token_here"
```

**Windows (Command Prompt):**
```cmd
set FIGMA_TOKEN=your_figma_token_here
```

**Linux/Mac:**
```bash
export FIGMA_TOKEN=your_figma_token_here
```

### 3. Run Comparison

**Option A: Interactive CLI Mode**
```bash
java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI
```

**Option B: Command-Line Arguments**
```bash
java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI \
    "https://your-website.com/page" \
    "abc123def456" \
    "789:123" \
    "DESKTOP"
```

**Option C: REST API (Spring Boot)**
```bash
mvn spring-boot:run
```

Then POST to `/api/compare`:
```bash
curl -X POST "http://localhost:8080/api/compare" \
  -d "url=https://your-website.com/page" \
  -d "figmaFile=abc123def456" \
  -d "figmaFrame=789:123" \
  -d "viewport=DESKTOP"
```

## 📖 Usage Guide for QA Testers

### Understanding Figma File and Node IDs

1. **Figma File ID**: Found in the Figma file URL
   - Example URL: `https://www.figma.com/file/abc123def456/MyDesign`
   - File ID: `abc123def456`

2. **Figma Node ID**: The frame/node ID you want to export
   - Open Figma file → Right-click frame → Copy link → Extract node ID
   - Or use Figma API: `GET /v1/files/{file_key}/nodes?ids={node_ids}`
   - Example: `789:123` or `1:23`

### Running a Comparison

1. **Prepare your inputs:**
   - Web URL (the page you want to test)
   - Figma File ID
   - Figma Node ID (the specific frame/component)
   - Viewport (Desktop/Tablet/Mobile)

2. **Run the tool:**
   ```bash
   java -cp target/figma-html-visual-mirror-1.0.0.jar com.mirror.cli.VisualComparisonCLI
   ```

3. **Enter the information when prompted:**
   ```
   🌐 Web URL to compare: https://myapp.com/dashboard
   📄 Figma File ID: abc123def456
   🎯 Figma Node ID (Frame ID): 789:123
   Select viewport [1-3, default: 1]: 1
   ```

4. **Review the HTML Report:**
   - Open `reports/report_*.html` in your browser
   - Check the visual comparison, observations, and issue details

### Understanding the HTML Report

The generated HTML report includes:

1. **Summary Card**
   - Mismatch Percentage: Overall difference between Figma and HTML
   - Severity: MINOR (<1%), MAJOR (1-5%), CRITICAL (>5%)
   - Issue Regions: Number of detected difference areas

2. **Visual Comparison**
   - **Figma Design (Expected)**: The approved design
   - **HTML Implementation (Actual)**: The live webpage
   - **Differences (Red Overlay)**: Highlighted mismatched pixels

3. **Human-Readable Observations**
   - Specific issues like "Button padding differs by ~6px"
   - "Font color mismatch in header text"
   - "Alignment issue detected around X:100-Y:50"

4. **Detailed Issue Regions Table**
   - Position coordinates (X, Y)
   - Size dimensions (Width × Height)
   - Issue Type (alignment, spacing, font, color, missing, extra)
   - Observation description
   - Impact percentage

## 🔍 Issue Classification

The tool automatically classifies visual differences into:

| Issue Type | Description | Example |
|------------|-------------|---------|
| **Alignment** | Elements are misaligned | "Horizontal alignment issue detected around X:100-Y:50" |
| **Spacing** | Padding or margins differ | "Spacing differs by approximately 6px at position X:200-Y:100" |
| **Font** | Font size or style mismatch | "Font size mismatch detected in text region X:50-Y:200" |
| **Color** | Color values don't match | "Color mismatch detected at X:150-Y:300" |
| **Missing** | UI element missing from HTML | "Missing or extra UI element detected at X:300-Y:400" |
| **Extra** | Extra element in HTML | "Missing or extra UI element detected at X:300-Y:400" |

## 📊 Severity Levels

| Severity | Threshold | Description |
|----------|-----------|-------------|
| **MINOR** | < 1% | Small differences that may not affect user experience |
| **MAJOR** | 1-5% | Noticeable differences that impact visual consistency |
| **CRITICAL** | > 5% | Significant deviations that break design integrity |

## 🛠️ Configuration

### Viewport Sizes

Default viewport configurations:

- **Desktop**: 1440 × 900 pixels
- **Tablet**: 768 × 1024 pixels
- **Mobile**: 375 × 667 pixels

### Pixel Difference Threshold

Default threshold: `30` (0-255 scale)
- Lower values = more sensitive (detects smaller differences)
- Higher values = less sensitive (only detects major differences)

To adjust, modify `PIXEL_DIFF_THRESHOLD` in `OpenCvDiffEngine.java`.

## 📁 Project Structure

```
figma-html-visual-mirror/
├── src/main/java/com/mirror/
│   ├── cli/
│   │   └── VisualComparisonCLI.java      # Command-line interface
│   ├── capture/
│   │   ├── SeleniumCaptureService.java   # Web page screenshot capture
│   │   └── WebCaptureService.java        # Capture interface
│   ├── figma/
│   │   ├── FigmaService.java             # Figma API interface
│   │   └── FigmaServiceImpl.java         # Figma API implementation
│   ├── image/
│   │   ├── ImageAligner.java             # Image size alignment
│   │   ├── ImageUtil.java                # BufferedImage ↔ Mat conversion
│   │   ├── OpenCvDiffEngine.java         # Pixel-to-pixel comparison
│   │   ├── VisualDiffClassifier.java     # Issue classification
│   │   └── VisualDiffEngine.java         # Diff engine interface
│   ├── model/
│   │   ├── DiffRegion.java               # Difference region model
│   │   ├── DiffResult.java               # Comparison result model
│   │   ├── IssueSeverity.java            # Severity enum
│   │   └── Viewport.java                 # Viewport enum
│   ├── orchestrator/
│   │   ├── ComparisonOrchestrator.java   # Main orchestration logic
│   │   └── CompareController.java        # REST API controller
│   ├── report/
│   │   ├── HtmlReportService.java        # HTML report generator
│   │   └── ReportService.java            # Report interface
│   ├── Main.java                         # CLI entry point
│   └── MirrorApplication.java            # Spring Boot application
├── pom.xml                                # Maven dependencies
└── README.md                              # This file
```

## 🔧 Advanced Usage

### CI/CD Integration

**Jenkins Pipeline Example:**
```groovy
stage('Visual Regression Test') {
    steps {
        sh '''
            export FIGMA_TOKEN="${FIGMA_TOKEN}"
            java -cp target/figma-html-visual-mirror-1.0.0.jar \
                com.mirror.cli.VisualComparisonCLI \
                "${WEB_URL}" \
                "${FIGMA_FILE_ID}" \
                "${FIGMA_NODE_ID}" \
                "DESKTOP"
        '''
    }
    post {
        always {
            archiveArtifacts 'reports/**/*.html'
            publishHTML([
                reportDir: 'reports',
                reportFiles: 'report_*.html',
                reportName: 'Visual Regression Report'
            ])
        }
    }
}
```

**GitHub Actions Example:**
```yaml
- name: Run Visual Regression Test
  env:
    FIGMA_TOKEN: ${{ secrets.FIGMA_TOKEN }}
  run: |
    java -cp target/figma-html-visual-mirror-1.0.0.jar \
      com.mirror.cli.VisualComparisonCLI \
      "${{ env.WEB_URL }}" \
      "${{ env.FIGMA_FILE_ID }}" \
      "${{ env.FIGMA_NODE_ID }}" \
      "DESKTOP"
      
- name: Upload HTML Report
  uses: actions/upload-artifact@v3
  with:
    name: visual-regression-report
    path: reports/*.html
```

### Programmatic Usage

```java
import com.mirror.model.Viewport;
import com.mirror.model.DiffResult;
import com.mirror.orchestrator.ComparisonOrchestrator;

ComparisonOrchestrator orchestrator = new ComparisonOrchestrator();

DiffResult result = orchestrator.compare(
    "https://myapp.com/page",
    "figma_file_id",
    "figma_node_id",
    Viewport.DESKTOP
);

System.out.println("Mismatch: " + result.getMismatchPercent() + "%");
System.out.println("Severity: " + result.getSeverity());
System.out.println("Issues: " + result.getRegions().size());
```

## 🐛 Troubleshooting

### Common Issues

1. **"Figma token not configured"**
   - Solution: Set `FIGMA_TOKEN` environment variable or update `FigmaServiceImpl.java`

2. **"Failed to capture webpage"**
   - Check Chrome/Chromium is installed
   - Ensure ChromeDriver is accessible (Selenium 4.17+ manages this automatically)
   - Verify the URL is accessible

3. **"Failed to fetch Figma frame"**
   - Verify Figma token is valid
   - Check File ID and Node ID are correct
   - Ensure you have access to the Figma file

4. **Out of Memory Errors**
   - Increase JVM heap size: `java -Xmx2g -cp ...`

## 📝 License

[Your License Here]

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues and questions, please open an issue on GitHub.

---

**Built with ❤️ for QA Teams**
