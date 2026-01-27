package com.mirror.semantic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.model.Viewport;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Uses Selenium to capture a structured semantic snapshot of the rendered HTML page.
 * This runs separately from pixel-based screenshot capture and focuses only on
 * layout, section structure, and typography.
 */
public class HtmlSemanticExtractor {

    private static final int MAX_TEXT_NODES = 80;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HtmlSemanticSnapshot capture(String url, Viewport viewport) {
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
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            driver.manage().window().setSize(new Dimension(viewport.getWidth(), viewport.getHeight()));

            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until((ExpectedCondition<Boolean>) wd ->
                    ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // wait for fonts
            try {
                wait.until((ExpectedCondition<Boolean>) wd ->
                        (Boolean) ((JavascriptExecutor) wd).executeScript(
                                "return document.fonts ? document.fonts.status === 'loaded' : true"));
            } catch (Exception ignored) {
            }

            // wait for images
            try {
                wait.until((ExpectedCondition<Boolean>) wd ->
                        (Boolean) ((JavascriptExecutor) wd).executeScript(
                                "return Array.from(document.images).every(img => img.complete)"));
            } catch (Exception ignored) {
            }

            // small stabilization delay
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            String json = (String) js.executeScript(buildSnapshotScript(MAX_TEXT_NODES));
            return objectMapper.readValue(json, HtmlSemanticSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to capture semantic HTML snapshot", e);
        } finally {
            driver.quit();
        }
    }

    private String buildSnapshotScript(int maxTextNodes) {
        // language=JavaScript
        String template = """
                const maxTextNodes = %d;

                function toRect(el) {
                  if (!el) return null;
                  const r = el.getBoundingClientRect();
                  return {
                    x: r.x,
                    y: r.y,
                    width: r.width,
                    height: r.height
                  };
                }

                function pick(selectors) {
                  for (const sel of selectors) {
                    const el = document.querySelector(sel);
                    if (el) return el;
                  }
                  return null;
                }

                function getNumeric(value) {
                  const n = parseFloat(value);
                  return isNaN(n) ? 0 : n;
                }

                const headerEl = pick(["header", "[data-section='header']", "[data-figma-section='header']"]);
                const heroEl = pick(["main section", "section[data-section='hero']", "[data-figma-section='hero']"]);
                const featuresEl = pick(["section.features", "[data-section='features']", "[data-figma-section='features']"]);
                const ctasEl = pick(["section.cta", "[data-section='cta']", "[data-figma-section='cta']", "[data-section='ctas']"]);
                const footerEl = pick(["footer", "[data-section='footer']", "[data-figma-section='footer']"]);

                const candidates = Array.from(document.querySelectorAll("h1,h2,h3,h4,p,button,a,[role='button']"));
                const textNodes = [];
                for (const el of candidates) {
                  if (!el.innerText) continue;
                  const r = el.getBoundingClientRect();
                  const style = window.getComputedStyle(el);
                  textNodes.push({
                    id: el.id || null,
                    tag: el.tagName.toLowerCase(),
                    text: el.innerText.trim().replace(/\\s+/g, " "),
                    x: r.x,
                    y: r.y,
                    font_family: style.fontFamily || "",
                    font_size: getNumeric(style.fontSize),
                    font_weight: style.fontWeight || "",
                    line_height: getNumeric(style.lineHeight),
                    letter_spacing: getNumeric(style.letterSpacing),
                    color: style.color || ""
                  });
                }

                textNodes.sort((a, b) => a.y === b.y ? a.x - b.x : a.y - b.y);

                const limitedText = textNodes.slice(0, maxTextNodes);

                const snapshot = {
                  viewport_width: window.innerWidth,
                  viewport_height: window.innerHeight,
                  document_width: Math.max(document.documentElement.scrollWidth || 0, document.body.scrollWidth || 0),
                  document_height: Math.max(document.documentElement.scrollHeight || 0, document.body.scrollHeight || 0),
                  sections: {
                    header: toRect(headerEl),
                    hero: toRect(heroEl),
                    features: toRect(featuresEl),
                    ctas: toRect(ctasEl),
                    footer: toRect(footerEl)
                  },
                  text_nodes: limitedText
                };

                return JSON.stringify(snapshot);
                """;
        return String.format(template, maxTextNodes);
    }
}

