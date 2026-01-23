# 🎨 Figma HTML Visual Regression Testing Tool

A QA-friendly Java-based Visual Regression Testing Tool that compares approved Figma UI designs with implemented HTML/CSS web pages at a pixel-to-pixel level.

## 📋 Overview

This tool helps QA testers and designers easily identify UI mismatches between Figma designs and live web pages by:
- Fetching design screens from Figma using Figma REST APIs (or using local cached images)
- Capturing full-page screenshots of HTML pages using Selenium WebDriver with lazy-loading support
- Performing pixel-to-pixel image comparison using OpenCV
- Classifying visual differences (alignment, spacing, font, color, missing elements)
- Generating comprehensive HTML reports with clear observations and severity levels

## 🎯 Key Features

✅ **Figma Integration** - Direct integration with Figma REST API with rate limit handling  
✅ **Local Figma Cache** - Use downloaded Figma designs to avoid API rate limits  
✅ **Responsive Testing** - Support for Desktop, Tablet, and Mobile viewports  
✅ **Full-Page Capture** - Captures entire scrollable page with lazy-loaded content  
✅ **Smart Image Alignment** - Automatically aligns images of different dimensions  
✅ **Pixel-to-Pixel Comparison** - Accurate visual difference detection  
✅ **Issue Classification** - Automatic categorization of visual issues  
✅ **QA-Friendly Reports** - HTML reports with observations like "Button padding differs by ~6px"  
✅ **Severity Levels** - Minor, Major, and Critical classifications  
✅ **REST API** - Spring Boot REST API support  
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
cd figma-html-comparison-master
mvn clean package
```

### 2. Set Figma Token

Set your Figma personal access token as an environment variable:

**Windows (PowerShell):**
```powershell
$env:FIGMA_TOKEN = "your_figma_token_here"
```

**Windows (Command Prompt):**
```cmd
set FIGMA_TOKEN=your_figma_token_here
```

**Linux/Mac:**
```bash
export FIGMA_TOKEN=your_figma_token_here
```

### 3. Start the Application

**Using Spring Boot JAR (Recommended):**
```powershell
# PowerShell
java -jar target\figma-html-visual-mirror-1.0.0.jar
```

```bash
# Linux/Mac
java -jar target/figma-html-visual-mirror-1.0.0.jar
```

The application will start on `http://localhost:8080`

### 4. Run Comparison via REST API

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/compare" `
    -Method POST `
    -Body @{
        url = "https://your-website.com"
        figmaFile = "abc123def456"
        figmaFrame = "68:108"
        viewport = "DESKTOP"
    }
```

**curl (Linux/Mac/Git Bash):**
```bash
curl -X POST "http://localhost:8080/api/compare" \
  -d "url=https://your-website.com" \
  -d "figmaFile=abc123def456" \
  -d "figmaFrame=68:108" \
  -d "viewport=DESKTOP"
```

**Example with Real Data:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/compare" `
    -Method POST `
    -Body @{
        url = "https://zolanah-new-weavers.webflow.io"
        figmaFile = "kaDcp2VTNV8RL0G58l3bwO"
        figmaFrame = "68:108"
        viewport = "DESKTOP"
    }
```

## 📖 Usage Guide for QA Testers

### Understanding Figma File and Node IDs

1. **Figma File ID**: Found in the Figma file URL
   - Example URL: `https://www.figma.com/file/abc123def456/MyDesign`
   - File ID: `abc123def456`

2. **Figma Node ID**: The frame/node ID you want to export
   - In Figma: Right-click frame → **Copy link to selection**
   - URL format: `https://www.figma.com/file/abc123/name?node-id=68-108`
   - Node ID: `68:108` (replace the dash with colon)
   - Example: `68:108`, `123:456`, `1:23`

### Running a Comparison

**Step 1: Start the Server**
```powershell
java -jar target\figma-html-visual-mirror-1.0.0.jar
```

**Step 2: Run Comparison (in a new PowerShell window)**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/compare" `
    -Method POST `
    -Body @{
        url = "https://your-website.com"
        figmaFile = "your_figma_file_id"
        figmaFrame = "your_node_id"
        viewport = "DESKTOP"
    }
```

**Step 3: View the Report**
```powershell
# Open the most recent report
Invoke-Item (Get-ChildItem reports\*.html | Sort-Object LastWriteTime -Descending | Select-Object -First 1)

# Or open the reports folder
Invoke-Item reports\
```

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

## 🔄 How It Works

### Current Workflow

```
Run Comparison
    ↓
1. Capture LIVE website (fresh screenshot each time)
   - Opens Chrome headless
   - Waits for page load, fonts, and images
   - Scrolls through page to trigger lazy-loaded content
   - Captures full-page screenshot
   - Saves to debug_images/web_screenshot.png
    ↓
2. Load Figma design
   - Option A: Fetch from Figma API (with retry on rate limits)
   - Option B: Use local cached file (figma_frame.png)
    ↓
3. Align images to same dimensions
   - Pads smaller image with white background
   - Ensures both images match in width and height
    ↓
4. Compare pixel-by-pixel with OpenCV
   - Detects visual differences
   - Classifies issues by type
   - Calculates mismatch percentage
    ↓
5. Generate HTML report
   - Visual side-by-side comparison
   - Issue highlights and observations
   - Saves to reports/ folder
```

### What Gets Updated on Each Run

✅ **Web Screenshot** - Captured fresh EVERY TIME  
✅ **Comparison Results** - Generated fresh EVERY TIME  
✅ **HTML Report** - New report generated EVERY TIME  
❌ **Figma Design** - Only updated when using FigmaServiceImpl (not FigmaServiceMock)

## 🎛️ Working with Figma API Rate Limits

### Option 1: Use Local Figma Cache (Current Setup)

To avoid API rate limits during testing, the tool uses a local Figma image:

**Setup:**
1. Download Figma frame once:
```powershell
$headers = @{ "X-Figma-Token" = "YOUR_TOKEN" }
$exportData = Invoke-RestMethod -Uri "https://api.figma.com/v1/images/FILE_ID?ids=NODE_ID&format=png&scale=1" -Headers $headers
Invoke-WebRequest -Uri $exportData.images.'NODE_ID' -OutFile "figma_frame.png"
```

2. Use `FigmaServiceMock` in `ComparisonOrchestrator.java`:
```java
//private final FigmaService figmaService = new FigmaServiceMock();
```

**Benefits:**
- ✅ No API calls (avoids rate limits)
- ✅ Faster comparisons
- ✅ Works offline
- ❌ Figma design not auto-updated

### Option 2: Use Figma API with Auto-Retry

For production use with fresh Figma designs:

1. Switch to `FigmaServiceImpl` in `ComparisonOrchestrator.java`:

[//]: # (```java)

[//]: # (private final FigmaService figmaService = new FigmaServiceImpl&#40;&#41;;)

[//]: # (```)

2. The tool automatically retries on rate limits (429 errors) with exponential backoff

**Benefits:**
- ✅ Always uses latest Figma design
- ✅ Automatic retry on rate limits
- ❌ Requires valid Figma token
- ❌ Subject to API rate limits

### Checking Figma Dimensions

To verify your Figma frame dimensions:

```powershell
$headers = @{ "X-Figma-Token" = "YOUR_TOKEN" }
$exportData = Invoke-RestMethod -Uri "https://api.figma.com/v1/images/FILE_ID?ids=NODE_ID&format=png&scale=1" -Headers $headers
Invoke-WebRequest -Uri $exportData.images.'NODE_ID' -OutFile "figma_frame.png"

Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile("$PWD\figma_frame.png")
Write-Host "Figma Frame: $($img.Width) x $($img.Height) pixels"
$img.Dispose()
```

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

To modify, edit `src/main/java/com/mirror/model/Viewport.java`

### Pixel Difference Threshold

Default threshold: `30` (0-255 scale)
- Lower values = more sensitive (detects smaller differences)
- Higher values = less sensitive (only detects major differences)

To adjust, modify `PIXEL_DIFF_THRESHOLD` in `OpenCvDiffEngine.java`

### Selenium Capture Settings

The enhanced `SeleniumCaptureService` includes:

- ✅ Full page height capture (including scrollable content)
- ✅ Lazy-loaded content triggering via scrolling
- ✅ Web font loading detection
- ✅ Image loading verification
- ✅ CSS animation completion waiting
- ✅ Dynamic content stabilization

Wait times can be adjusted in `SeleniumCaptureService.java`

## 📁 Project Structure

```
figma-html-comparison-master/
├── src/main/java/com/mirror/
│   ├── capture/
│   │   ├── SeleniumCaptureService.java   # Enhanced web capture with lazy loading
│   │   └── WebCaptureService.java        # Capture interface
│   ├── figma/
│   │   ├── FigmaService.java             # Figma API interface
│   │   ├── FigmaServiceImpl.java         # Figma API with retry logic
│   │   └── FigmaServiceMock.java         # Local file loader (for testing)
│   ├── image/
│   │   ├── ImageAligner.java             # Smart image dimension alignment
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
│   └── MirrorApplication.java            # Spring Boot application
├── debug_images/                          # Debug screenshots
│   ├── web_screenshot.png                # Latest web capture
│   └── figma_design.png                  # Latest Figma design
├── reports/                               # Generated HTML reports
├── figma_frame.png                        # Cached Figma design (optional)
├── pom.xml                                # Maven dependencies
└── README.md                              # This file
```

## 🔧 Advanced Usage

### Batch Testing Multiple Pages

Create a PowerShell script to test multiple pages:

```powershell
# batch-test.ps1
$pages = @(
    @{url="https://myapp.com/home"; frame="123:456"},
    @{url="https://myapp.com/about"; frame="123:789"},
    @{url="https://myapp.com/contact"; frame="123:012"}
)

foreach ($page in $pages) {
    Write-Host "Testing: $($page.url)" -ForegroundColor Yellow
    Invoke-RestMethod -Uri "http://localhost:8080/api/compare" `
        -Method POST `
        -Body @{
            url = $page.url
            figmaFile = "YOUR_FIGMA_FILE_ID"
            figmaFrame = $page.frame
            viewport = "DESKTOP"
        }
    Start-Sleep -Seconds 5
}

Invoke-Item reports\
```

### CI/CD Integration

**Jenkins Pipeline Example:**
```groovy
stage('Visual Regression Test') {
    steps {
        sh '''
            export FIGMA_TOKEN="${FIGMA_TOKEN}"
            java -jar target/figma-html-visual-mirror-1.0.0.jar &
            SERVER_PID=$!
            sleep 10
            
            curl -X POST "http://localhost:8080/api/compare" \
              -d "url=${WEB_URL}" \
              -d "figmaFile=${FIGMA_FILE_ID}" \
              -d "figmaFrame=${FIGMA_NODE_ID}" \
              -d "viewport=DESKTOP"
            
            kill $SERVER_PID
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
- name: Start Visual Regression Server
  env:
    FIGMA_TOKEN: ${{ secrets.FIGMA_TOKEN }}
  run: |
    java -jar target/figma-html-visual-mirror-1.0.0.jar &
    sleep 10

- name: Run Visual Regression Test
  run: |
    curl -X POST "http://localhost:8080/api/compare" \
      -d "url=${{ env.WEB_URL }}" \
      -d "figmaFile=${{ env.FIGMA_FILE_ID }}" \
      -d "figmaFrame=${{ env.FIGMA_NODE_ID }}" \
      -d "viewport=DESKTOP"
      
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

//ComparisonOrchestrator orchestrator = new ComparisonOrchestrator();
//
//DiffResult result = orchestrator.compare(
//    "https://myapp.com/page",
//    "figma_file_id",
//    "figma_node_id",
//    Viewport.DESKTOP
//);
//
//System.out.println("Mismatch: " + result.getMismatchPercent() + "%");
//System.out.println("Severity: " + result.getSeverity());
//System.out.println("Issues: " + result.getRegions().size());
```

## 🐛 Troubleshooting

### Common Issues

1. **"Figma token not configured"**
   - Solution: Set `FIGMA_TOKEN` environment variable before starting the server
   - Verify: `echo $env:FIGMA_TOKEN` (PowerShell) or `echo $FIGMA_TOKEN` (Linux/Mac)

2. **"Failed to capture webpage"**
   - Check Chrome/Chromium is installed
   - Ensure ChromeDriver is accessible (Selenium 4.17+ manages this automatically)
   - Verify the URL is accessible from your network
   - Check for firewall or proxy issues

3. **"HTTP 429: Rate limit exceeded"**
   - Switch to `FigmaServiceMock` to use local cached images
   - Wait 10-15 minutes for rate limit to reset
   - Or download Figma frame manually and place in project root as `figma_frame.png`

4. **"Images have different dimensions"**
   - This is now automatically handled by `ImageAligner`
   - Check `debug_images/` folder for actual captured dimensions
   - Ensure full page is loading (check for lazy-loaded content)

5. **"ArrayIndexOutOfBoundsException in ImageUtil"**
   - Fixed in updated `ImageUtil.java` - handles grayscale and color images
   - Rebuild project: `mvn clean package`

6. **Screenshots miss lazy-loaded content**
   - Enhanced `SeleniumCaptureService` now scrolls through entire page
   - Adjust wait times in `SeleniumCaptureService.java` if needed
   - Increase `Thread.sleep()` durations for slower websites

7. **Out of Memory Errors**
   - Increase JVM heap size: `java -Xmx2g -jar target/figma-html-visual-mirror-1.0.0.jar`
   - For very large pages, consider: `java -Xmx4g -jar ...`

### Debug Information

The tool saves debug images to `debug_images/` folder:
- `web_screenshot.png` - Latest captured web page
- `figma_design.png` - Latest loaded Figma design

Check these images to verify:
- Full page was captured (no missing content)
- Dimensions match expectations
- Images loaded correctly

### Verbose Logging

The server console shows detailed logs:
```
=== WEB PAGE CAPTURE ===
URL: https://example.com
Viewport: Desktop (1440x900)
Full page dimensions: 1440 x 6587
Screenshot captured: 1440 x 6587
========================

=== FIGMA DESIGN FETCH ===
Figma design fetched: 1440 x 6587
==========================

=== IMAGE ALIGNMENT ===
Target dimensions: 1440 x 6587
==========================

=== OPENCV COMPARISON ===
Mismatch percentage: 31.72%
=========================
```

## 📝 Best Practices

1. **Use Local Figma Cache for Testing**
   - Download Figma designs once
   - Use `FigmaServiceMock` during development
   - Switch to `FigmaServiceImpl` for production/CI

2. **Test Multiple Viewports**
   - Run comparisons for DESKTOP, TABLET, and MOBILE
   - Ensure responsive design matches across all viewports

3. **Monitor Page Load Times**
   - Adjust wait times for slower sites
   - Check lazy-loaded content is fully loaded

4. **Review Debug Images**
   - Always check `debug_images/` folder
   - Verify screenshots captured correctly
   - Confirm dimensions match Figma design

5. **Organize Reports**
   - Reports are timestamped automatically
   - Archive old reports regularly
   - Use descriptive Figma frame names

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
