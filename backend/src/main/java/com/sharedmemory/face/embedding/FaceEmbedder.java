package com.sharedmemory.face.embedding;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.sharedmemory.face.ImageUtils;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ArcFace (arcfaceresnet100-8, ONNX Model Zoo). Expects a 112x112 aligned/cropped face; produces
 * a 512-d embedding which callers should L2-normalize (done here) before comparing with cosine
 * similarity.
 */
@Component
public class FaceEmbedder implements AutoCloseable {

    public static final String MODEL_VERSION = "arcfaceresnet100-8";
    private static final int INPUT_SIZE = 112;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;

    public FaceEmbedder(@Value("${app.face.embedding-model-path}") String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
        this.outputName = session.getOutputNames().iterator().next();
    }

    public float[] embed(BufferedImage faceCrop) {
        float[][][][] input = preprocess(faceCrop);
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
                OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][] output = (float[][]) result.get(outputName).get().getValue();
            return l2Normalize(output[0]);
        } catch (OrtException e) {
            throw new IllegalStateException("Face embedding failed", e);
        }
    }

    private float[][][][] preprocess(BufferedImage faceCrop) {
        // Empirically verified (see FaceRecognitionPipelineTest / project notes): this model
        // expects raw 0-255 RGB pixel values with NO mean/scale normalization applied by the
        // caller — normalization is baked into the model's own graph. Applying the "standard"
        // (px-127.5)/128 ArcFace normalization here collapses same- and different-identity
        // cosine similarity into the same ~0.94-0.99 band (unusable); raw pixels give a clean
        // same-identity >=0.22 / different-identity <=0.10 separation on real test photos.
        BufferedImage resized = ImageUtils.resize(faceCrop, INPUT_SIZE, INPUT_SIZE);
        float[][][][] input = new float[1][3][INPUT_SIZE][INPUT_SIZE];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                input[0][0][y][x] = (rgb >> 16) & 0xFF;
                input[0][1][y][x] = (rgb >> 8) & 0xFF;
                input[0][2][y][x] = rgb & 0xFF;
            }
        }
        return input;
    }

    private float[] l2Normalize(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = norm == 0 ? 0 : (float) (vector[i] / norm);
        }
        return normalized;
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
