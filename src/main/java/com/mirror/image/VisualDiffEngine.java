package com.mirror.image;

import com.mirror.model.DiffResult;

import java.awt.image.BufferedImage;

public interface VisualDiffEngine {
    DiffResult compare(BufferedImage figma, BufferedImage live);
}
