package com.sharedmemory.face;

import com.sharedmemory.face.detection.FaceBox;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static BufferedImage resize(BufferedImage src, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    public static BufferedImage cropWithMargin(BufferedImage image, FaceBox box, double marginRatio) {
        int marginX = (int) Math.round(box.width() * marginRatio);
        int marginY = (int) Math.round(box.height() * marginRatio);
        int x1 = Math.max(0, box.x1() - marginX);
        int y1 = Math.max(0, box.y1() - marginY);
        int x2 = Math.min(image.getWidth(), box.x2() + marginX);
        int y2 = Math.min(image.getHeight(), box.y2() + marginY);
        return image.getSubimage(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }
}
