package com.sharedmemory.face.detection;

public record FaceBox(int x1, int y1, int x2, int y2, double confidence) {
    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }
}
