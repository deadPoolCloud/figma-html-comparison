package com.mirror.capture;

import com.mirror.model.Viewport;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;

/**
 * Selenium-based web page capture service with responsive viewport support
 * Enhanced to handle lazy loading, web fonts, and dynamic content
 */
public class SeleniumCaptureService implements WebCaptureService {

    @Override
    public BufferedImage capture(String url) {
        return capture(url, Viewport.DESKTOP);
    }

    @Override
    public BufferedImage capture(String url, Viewport viewport) {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");

        // Disable lazy loading to ensure all images load
        options.addArguments("--disable-features=LazyFrameLoading,LazyImageLoading");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

            // Set initial viewport size
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), viewport.getHeight()));

            System.out.println("=== WEB PAGE CAPTURE ===");
            System.out.println("URL: " + url);
            System.out.println("Viewport: " + viewport.getName() + " (" + viewport.getWidth() + "x" + viewport.getHeight() + ")");

            // Load the page
            driver.get(url);

            // Wait for document ready state
            System.out.println("Waiting for page to load...");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until((ExpectedCondition<Boolean>) wd ->
                    ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Wait for fonts to load
            System.out.println("Waiting for web fonts...");
            try {
                wait.until((ExpectedCondition<Boolean>) wd ->
                        (Boolean) ((JavascriptExecutor) wd).executeScript(
                                "return document.fonts ? document.fonts.status === 'loaded' : true"));
            } catch (Exception e) {
                System.out.println("Font check not supported, continuing...");
            }

            // Wait for images to load (best-effort; do not fail the whole capture if this times out)
            System.out.println("Waiting for images to load...");
            try {
                wait.until((ExpectedCondition<Boolean>) wd ->
                        (Boolean) ((JavascriptExecutor) wd).executeScript(
                                "return Array.from(document.images).every(img => img.complete)"));
            } catch (TimeoutException te) {
                System.out.println("Image load wait timed out, continuing with capture anyway.");
            } catch (Exception e) {
                System.out.println("Image load check failed, continuing: " + e.getMessage());
            }

            // Additional wait for any animations or dynamic content
            Thread.sleep(2000);

            // Get the FULL page dimensions BEFORE scrolling
            Long scrollWidth = (Long) js.executeScript("return Math.max(document.documentElement.scrollWidth, document.body.scrollWidth)");
            Long scrollHeight = (Long) js.executeScript("return Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)");

            int pageWidth = scrollWidth != null ? scrollWidth.intValue() : viewport.getWidth();
            int pageHeight = scrollHeight != null ? scrollHeight.intValue() : viewport.getHeight();

            System.out.println("Initial page dimensions: " + pageWidth + " x " + pageHeight);

            // Scroll through the entire page to trigger lazy-loaded content
            System.out.println("Scrolling to trigger lazy-loaded content...");
            int viewportHeight = viewport.getHeight();
            int scrollSteps = (int) Math.ceil((double) pageHeight / viewportHeight);

            for (int i = 0; i < scrollSteps; i++) {
                int scrollY = i * viewportHeight;
                js.executeScript("window.scrollTo(0, " + scrollY + ");");
                Thread.sleep(300); // Wait for lazy content to load
            }

            // Scroll back to top
            js.executeScript("window.scrollTo(0, 0);");
            Thread.sleep(500);

            // Re-check page dimensions after scrolling (lazy content may have loaded)
            scrollWidth = (Long) js.executeScript("return Math.max(document.documentElement.scrollWidth, document.body.scrollWidth)");
            scrollHeight = (Long) js.executeScript("return Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)");

            pageWidth = scrollWidth != null ? scrollWidth.intValue() : pageWidth;
            pageHeight = scrollHeight != null ? scrollHeight.intValue() : pageHeight;

            System.out.println("Final page dimensions after lazy loading: " + pageWidth + " x " + pageHeight);

            // Set window size to capture the EXACT full page
            driver.manage().window().setSize(new Dimension(pageWidth, pageHeight));

            // Final wait for resize and any remaining animations
            Thread.sleep(1500);

            // Wait for any final animations to complete
            try {
                js.executeScript(
                        "const animations = document.getAnimations();" +
                                "return Promise.all(animations.map(a => a.finished));"
                );
            } catch (Exception e) {
                System.out.println("Animation check skipped: " + e.getMessage());
            }

            // Take the screenshot
            System.out.println("Capturing screenshot...");
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot));

            System.out.println("Screenshot captured: " + image.getWidth() + " x " + image.getHeight());

            // Save debug image
            try {
                File debugDir = new File("debug_images");
                debugDir.mkdirs();
                ImageIO.write(image, "png", new File("debug_images/web_screenshot.png"));
                System.out.println("Debug: Web screenshot saved to debug_images/web_screenshot.png");
            } catch (Exception e) {
                System.err.println("Warning: Failed to save debug screenshot: " + e.getMessage());
            }

            System.out.println("========================");

            return image;

        } catch (Exception e) {
            throw new RuntimeException("Failed to capture webpage at viewport " + viewport.getName(), e);
        } finally {
            driver.quit();
        }
    }
}