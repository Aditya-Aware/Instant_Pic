package com.sharedmemory.face.matching;

import com.sharedmemory.face.ImageUtils;
import com.sharedmemory.face.detection.FaceBox;
import com.sharedmemory.face.detection.FaceDetector;
import com.sharedmemory.face.embedding.FaceEmbedder;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns a handful of selfie images into a single event-scoped face profile embedding. Each
 * selfie must contain exactly one detected face (ambiguous/empty selfies are rejected rather
 * than silently enrolling a bad sample) — raw selfie images are never persisted; only the
 * resulting embedding is.
 */
@Component
public class FaceEnrollmentService {

    private static final double CROP_MARGIN_RATIO = 0.2;

    private final FaceDetector faceDetector;
    private final FaceEmbedder faceEmbedder;

    public FaceEnrollmentService(FaceDetector faceDetector, FaceEmbedder faceEmbedder) {
        this.faceDetector = faceDetector;
        this.faceEmbedder = faceEmbedder;
    }

    public EnrollmentResult enroll(List<BufferedImage> selfies) {
        if (selfies.isEmpty()) {
            throw new IllegalArgumentException("At least one selfie is required for enrollment");
        }
        List<float[]> embeddings = new ArrayList<>();
        for (int i = 0; i < selfies.size(); i++) {
            BufferedImage selfie = selfies.get(i);
            List<FaceBox> faces = faceDetector.detectFaces(selfie);
            if (faces.size() != 1) {
                throw new IllegalArgumentException(
                        "Selfie " + (i + 1) + " must contain exactly one face, found " + faces.size());
            }
            BufferedImage crop = ImageUtils.cropWithMargin(selfie, faces.get(0), CROP_MARGIN_RATIO);
            embeddings.add(faceEmbedder.embed(crop));
        }
        return new EnrollmentResult(FaceVectorMath.l2NormalizedCentroid(embeddings), embeddings.size());
    }
}
