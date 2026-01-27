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
        options.addArguments("--disable-features=LazyFrameLoading,LazyImageLoading");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), viewport.getHeight()));

            System.out.println("=== WEB PAGE CAPTURE ===");
            System.out.println("URL: " + url);

            driver.get(url);

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 🔹 Soft bootstrap wait (no ExpectedCondition)
            Thread.sleep(4000);

            // 🔹 Scroll for lazy loading
            Long scrollHeight = (Long) js.executeScript(
                    "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"
            );

            int totalHeight = scrollHeight != null ? scrollHeight.intValue() : viewport.getHeight();
            int viewportHeight = viewport.getHeight();
            int steps = (int) Math.ceil((double) totalHeight / viewportHeight);

            for (int i = 0; i < steps; i++) {
                js.executeScript("window.scrollTo(0, arguments[0]);", i * viewportHeight);
                Thread.sleep(250);
            }

            // Go top again
            js.executeScript("window.scrollTo(0, 0);");
            Thread.sleep(500);

            // 🔹 Recalculate height (lazy images expanded DOM)
            scrollHeight = (Long) js.executeScript(
                    "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"
            );
            totalHeight = scrollHeight != null ? scrollHeight.intValue() : totalHeight;

            // 🔹 Resize window for full screenshot
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), totalHeight));
            Thread.sleep(500);

            // Final wait
            Thread.sleep(1000);

            // 🔹 Capture screenshot
            byte[] data = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));

            // Save for debugging
            File debugDir = new File("debug_images");
            debugDir.mkdirs();
            ImageIO.write(img, "png", new File(debugDir, "web_screenshot.png"));

            System.out.println("Captured: " + img.getWidth() + "x" + img.getHeight());
            System.out.println("========================");

            return img;

        } catch (Exception e) {
            throw new RuntimeException("Failed to capture webpage at viewport " + viewport.getName(), e);
        } finally {
            driver.quit();
        }
    }
}
