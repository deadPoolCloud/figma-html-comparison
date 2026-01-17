package com.mirror.image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

/**
 * Utility class for converting between Java BufferedImage and OpenCV Mat
 */
public class ImageUtil {

    /**
     * Converts a BufferedImage to OpenCV Mat format
     */
    public static Mat toMat(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        byte[] data = new byte[width * height * 3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int idx = (y * width + x) * 3;
                
                // Convert ARGB to BGR for OpenCV
                data[idx] = (byte) (pixel & 0xFF);           // Blue
                data[idx + 1] = (byte) ((pixel >> 8) & 0xFF); // Green
                data[idx + 2] = (byte) ((pixel >> 16) & 0xFF); // Red
            }
        }
        
        mat.put(0, 0, data);
        return mat;
    }

    /**
     * Converts an OpenCV Mat to BufferedImage
     */
    public static BufferedImage toBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();
        
        byte[] data = new byte[width * height * channels];
        mat.get(0, 0, data);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * channels;
                int b = data[idx] & 0xFF;
                int g = data[idx + 1] & 0xFF;
                int r = data[idx + 2] & 0xFF;
                
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }
        
        return image;
    }
}
