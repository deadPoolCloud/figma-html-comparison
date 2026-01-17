package com.mirror.capture;

import com.mirror.model.Viewport;

import java.awt.image.BufferedImage;

public interface WebCaptureService {
    BufferedImage capture(String url);
    BufferedImage capture(String url, Viewport viewport);
}
