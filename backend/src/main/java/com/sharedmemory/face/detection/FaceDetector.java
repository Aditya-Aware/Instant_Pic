package com.sharedmemory.face.detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.sharedmemory.face.ImageUtils;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ultra-Light-Fast-Generic-Face-Detector-1MB (version-RFB-320). The ONNX export already bakes in
 * anchor-box decoding, so raw "boxes" output is already normalized [x1,y1,x2,y2] — only score
 * filtering, greedy NMS, and denormalization happen here (ported from the model's reference
 * detect_imgs_onnx.py: resize 320x240, mean 127 / scale 128, RGB, score threshold 0.7, NMS IoU
 * 0.3, candidate cap 200).
 */
@Component
public class FaceDetector implements AutoCloseable {

    private static final int INPUT_WIDTH = 320;
    private static final int INPUT_HEIGHT = 240;
    private static final float SCORE_THRESHOLD = 0.7f;
    private static final float IOU_THRESHOLD = 0.3f;
    private static final int CANDIDATE_SIZE = 200;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final List<String> outputNames;

    public FaceDetector(@Value("${app.face.detector-model-path}") String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
        this.outputNames = new ArrayList<>(session.getOutputNames());
    }

    public List<FaceBox> detectFaces(BufferedImage image) {
        int origWidth = image.getWidth();
        int origHeight = image.getHeight();
        float[][][][] input = preprocess(image);
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
                OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][][] confidences = (float[][][]) result.get(outputNames.get(0)).get().getValue();
            float[][][] boxes = (float[][][]) result.get(outputNames.get(1)).get().getValue();
            return postprocess(confidences[0], boxes[0], origWidth, origHeight);
        } catch (OrtException e) {
            throw new IllegalStateException("Face detection failed", e);
        }
    }

    private float[][][][] preprocess(BufferedImage image) {
        BufferedImage resized = ImageUtils.resize(image, INPUT_WIDTH, INPUT_HEIGHT);
        float[][][][] input = new float[1][3][INPUT_HEIGHT][INPUT_WIDTH];
        for (int y = 0; y < INPUT_HEIGHT; y++) {
            for (int x = 0; x < INPUT_WIDTH; x++) {
                int rgb = resized.getRGB(x, y);
                input[0][0][y][x] = (((rgb >> 16) & 0xFF) - 127f) / 128f;
                input[0][1][y][x] = (((rgb >> 8) & 0xFF) - 127f) / 128f;
                input[0][2][y][x] = ((rgb & 0xFF) - 127f) / 128f;
            }
        }
        return input;
    }

    private List<FaceBox> postprocess(float[][] confidences, float[][] boxes, int origWidth, int origHeight) {
        List<float[]> candidates = new ArrayList<>();
        for (int i = 0; i < confidences.length; i++) {
            float faceScore = confidences[i][1];
            if (faceScore > SCORE_THRESHOLD) {
                candidates.add(new float[] {boxes[i][0], boxes[i][1], boxes[i][2], boxes[i][3], faceScore});
            }
        }
        candidates.sort((a, b) -> Float.compare(b[4], a[4]));
        if (candidates.size() > CANDIDATE_SIZE) {
            candidates = new ArrayList<>(candidates.subList(0, CANDIDATE_SIZE));
        }

        List<float[]> picked = new ArrayList<>();
        while (!candidates.isEmpty()) {
            float[] best = candidates.remove(0);
            picked.add(best);
            candidates.removeIf(c -> iou(best, c) > IOU_THRESHOLD);
        }

        List<FaceBox> results = new ArrayList<>();
        for (float[] p : picked) {
            int x1 = clamp(Math.round(p[0] * origWidth), origWidth);
            int y1 = clamp(Math.round(p[1] * origHeight), origHeight);
            int x2 = clamp(Math.round(p[2] * origWidth), origWidth);
            int y2 = clamp(Math.round(p[3] * origHeight), origHeight);
            if (x2 > x1 && y2 > y1) {
                results.add(new FaceBox(x1, y1, x2, y2, p[4]));
            }
        }
        return results;
    }

    private int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private double iou(float[] a, float[] b) {
        float ix1 = Math.max(a[0], b[0]);
        float iy1 = Math.max(a[1], b[1]);
        float ix2 = Math.min(a[2], b[2]);
        float iy2 = Math.min(a[3], b[3]);
        float interW = Math.max(0, ix2 - ix1);
        float interH = Math.max(0, iy2 - iy1);
        float interArea = interW * interH;
        float areaA = (a[2] - a[0]) * (a[3] - a[1]);
        float areaB = (b[2] - b[0]) * (b[3] - b[1]);
        float union = areaA + areaB - interArea;
        return union <= 0 ? 0 : interArea / union;
    }

    @PreDestroy
    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            // ignore on shutdown
        }
    }
}
