package com.mirror.capture;

import com.mirror.model.Viewport;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;

/**
 * Selenium-based web page capture service with responsive viewport support
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

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

            // Set initial viewport size
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), viewport.getHeight()));
            driver.get(url);

            // Wait for UI to stabilize (images, fonts, animations)
            Thread.sleep(3000);

            // Get the FULL page dimensions (including scrollable content)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long scrollWidth = (Long) js.executeScript("return document.documentElement.scrollWidth");
            Long scrollHeight = (Long) js.executeScript("return document.documentElement.scrollHeight");

            int pageWidth = scrollWidth != null ? scrollWidth.intValue() : viewport.getWidth();
            int pageHeight = scrollHeight != null ? scrollHeight.intValue() : viewport.getHeight();

            System.out.println("=== WEB PAGE CAPTURE ===");
            System.out.println("URL: " + url);
            System.out.println("Viewport: " + viewport.getName() + " (" + viewport.getWidth() + "x" + viewport.getHeight() + ")");
            System.out.println("Full page dimensions: " + pageWidth + " x " + pageHeight);

            // Resize window to capture full page
            driver.manage().window().setSize(new Dimension(pageWidth, pageHeight));

            // Wait a bit after resize
            Thread.sleep(1000);

            // Take full page screenshot
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