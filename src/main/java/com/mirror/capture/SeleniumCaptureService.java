package com.mirror.capture;

import com.mirror.model.Viewport;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
        options.addArguments("--window-size=" + viewport.getWidth() + "," + viewport.getHeight());
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), viewport.getHeight()));
            driver.get(url);

            // Wait for UI to stabilize (images, fonts, animations)
            Thread.sleep(3000);

            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);

            return ImageIO.read(new ByteArrayInputStream(screenshot));

        } catch (Exception e) {
            throw new RuntimeException("Failed to capture webpage at viewport " + viewport.getName(), e);
        } finally {
            driver.quit();
        }
    }
}
