package com.mirror.capture;

import com.mirror.model.Viewport;
import com.mirror.semantic.HtmlSemanticSnapshot;

import java.awt.image.BufferedImage;

public interface WebCaptureService {
    BufferedImage capture(String url);

    BufferedImage capture(String url, Viewport viewport);

    HtmlSemanticSnapshot captureSemantic(String url, Viewport viewport);
}
