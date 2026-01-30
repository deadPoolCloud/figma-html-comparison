package com.mirror.capture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.mirror.model.Viewport;
import com.mirror.semantic.HtmlSemanticSnapshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class PlaywrightCaptureService implements WebCaptureService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public BufferedImage capture(String url) {
    return capture(url, Viewport.DESKTOP);
  }

  @Override
  public BufferedImage capture(String url, Viewport viewport) {
    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      Page page = createPage(browser, viewport);

      navigateAndWait(page, url);

      byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
      return ImageIO.read(new ByteArrayInputStream(screenshotBytes));
    } catch (Exception e) {
      throw new RuntimeException("Failed to capture screenshot with Playwright", e);
    }
  }

  @Override
  public HtmlSemanticSnapshot captureSemantic(String url, Viewport viewport) {
    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      Page page = createPage(browser, viewport);

      navigateAndWait(page, url);

      String script = buildSnapshotScript(500);
      Object result = page.evaluate(script);
      String json;
      if (result instanceof String) {
        json = (String) result;
      } else {
        // Should not happen if script returns JSON.stringify
        throw new RuntimeException("Script did not return a string");
      }

      return objectMapper.readValue(json, HtmlSemanticSnapshot.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to capture semantic snapshot with Playwright", e);
    }
  }

  private Page createPage(Browser browser, Viewport viewport) {
    BrowserContext context = browser.newContext(new Browser.NewContextOptions()
        .setViewportSize(viewport.getWidth(), viewport.getHeight()));
    return context.newPage();
  }

  private void navigateAndWait(Page page, String url) {
    page.navigate(url, new Page.NavigateOptions().setTimeout(60000));
    page.waitForTimeout(2000); // Allow initial JS layout to settle

    // Wait for DOM content to be loaded (faster/safer than networkidle)
    page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);

    // Optional: Wait for network idle with a short timeout, but don't fail if it
    // times out
    try {
      page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
          new Page.WaitForLoadStateOptions().setTimeout(5000));
    } catch (TimeoutError e) {
      System.out.println("Warning: Network idle timeout (proceeding anyway)");
    }

    // Wait for fonts
    page.evaluate("document.fonts.ready");

    // Trigger lazy loading and scroll animations via incremental scrolling
    page.evaluate("""
            async () => {
                const delay = 150;
                const step = 300;
                let lastHeight = document.body.scrollHeight;
                let currentScroll = 0;

                while (true) {
                    window.scrollBy(0, step);
                    currentScroll += step;
                    await new Promise(resolve => setTimeout(resolve, delay));

                    if (currentScroll >= document.body.scrollHeight) {
                        await new Promise(resolve => setTimeout(resolve, 500));
                        if (document.body.scrollHeight === lastHeight) break;
                        lastHeight = document.body.scrollHeight;
                    }
                }

                window.scrollTo(0, 0);
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        """);

    // Give a small extra buffer for final animation frames
    page.waitForTimeout(1000);
  }

  private String buildSnapshotScript(int maxTextNodes) {
    // Wrapped in IIFE to allow 'return' and isolated scope
    return String.format(
        """
            (() => {
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

                // Expanded selectors to capture ALL text content including labels and table cells
                function isVisible(el) {
                  const style = window.getComputedStyle(el);
                  return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0' && !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
                }

                // Whitelist containers that usually represent a single Figma text block
                const textContainers = Array.from(document.querySelectorAll("h1,h2,h3,h4,h5,h6,p,li,label,button,a"));
                const textNodes = [];

                function addNode(el) {
                  const r = el.getBoundingClientRect();
                  if (r.width === 0 || r.height === 0) return;

                  const style = window.getComputedStyle(el);
                  let text = el.innerText.trim().replace(/\\s+/g, " ");
                  if (!text || text.length < 1) return;

                  // Avoid duplicates and sub-strings (nested elements)
                  // If we already have a node at this spot with overlapping text, it's the same designer intent
                  const existing = textNodes.find(n => (n.text.includes(text) || text.includes(n.text)) && Math.abs(n.x - r.x) < 20 && Math.abs(n.y - r.y) < 20);
                  if (existing) {
                    // If the new one is longer, it carries more content. Replace.
                    if (text.length > existing.text.length) {
                       existing.text = text;
                       existing.tag = el.tagName.toLowerCase();
                       // We keep the first one's styles if it was a leaf, but update the text
                    }
                    return;
                  }

                  textNodes.push({
                    id: el.id || null,
                    tag: el.tagName.toLowerCase(),
                    text: text,
                    x: r.x,
                    y: r.y,
                    font_family: style.fontFamily || "",
                    font_size: getNumeric(style.fontSize),
                    font_weight: style.fontWeight || "",
                    line_height: getNumeric(style.lineHeight),
                    letter_spacing: getNumeric(style.letterSpacing),
                    color: style.color || "",
                    parent_id: el.parentElement ? (el.parentElement.id || el.parentElement.tagName.toLowerCase()) : null
                  });
                }

                // 1. Process leaves first (specific styles)
                const leaves = Array.from(document.querySelectorAll("span, b, strong, i, em, small, a, button, label, td, th"));
                leaves.forEach(el => {
                   if (isVisible(el)) addNode(el);
                });

                // 2. Process containers (paragraphs, headings)
                const containers = Array.from(document.querySelectorAll("h1,h2,h3,h4,h5,h6,p,li,div"));
                containers.forEach(el => {
                   if (isVisible(el)) addNode(el);
                });

                textNodes.sort((a, b) => a.y === b.y ? a.x - b.x : a.y - b.y);

                // Expanded interactive selectors
                const interactives = Array.from(document.querySelectorAll("button, a, input:not([type='hidden']), textarea, select, [role='button'], [role='link']"));
                const interactiveNodes = [];
                for (const el of interactives) {
                   if (el.offsetParent === null) continue; // skip hidden
                   const r = el.getBoundingClientRect();
                   if (r.width === 0 || r.height === 0) continue;
                   const style = window.getComputedStyle(el);

                   // Determine text content for interactive
                   let text = el.innerText || el.value || el.getAttribute("aria-label") || "";

                   interactiveNodes.push({
                       text: text.trim().replace(/\\s+/g, " "),
                       tag: el.tagName.toLowerCase(),
                       rect: { x: r.x, y: r.y, width: r.width, height: r.height },
                       background_color: style.backgroundColor,
                       border_radius: style.borderRadius,
                       padding: style.padding,
                       color: style.color,
                       parent_id: el.parentElement ? (el.parentElement.id || el.parentElement.tagName.toLowerCase()) : null
                   });
                }

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
                  text_nodes: limitedText,
                  interactive_elements: interactiveNodes
                };

                return JSON.stringify(snapshot);
            })();
            """,
        maxTextNodes);
  }
}
