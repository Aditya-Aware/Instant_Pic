package com.sharedmemory.face.matching;

import com.sharedmemory.face.ImageUtils;
import com.sharedmemory.face.detection.FaceBox;
import com.sharedmemory.face.detection.FaceDetector;
import com.sharedmemory.face.embedding.FaceEmbedder;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Detects every face in a photo, embeds each, and — independently of any sharing decision —
 * reports the best-matching enrolled profile (if any) above the configured confidence threshold.
 * This class produces participant matches only; it must never decide who a photo is shared with
 * (see README.md / the sharing-rules-engine boundary).
 */
@Component
public class FaceMatcher {

    private static final double CROP_MARGIN_RATIO = 0.2;

    private final FaceDetector faceDetector;
    private final FaceEmbedder faceEmbedder;
    private final double matchThreshold;

    public FaceMatcher(
            FaceDetector faceDetector,
            FaceEmbedder faceEmbedder,
            @Value("${app.face.match-confidence-threshold}") double matchThreshold) {
        this.faceDetector = faceDetector;
        this.faceEmbedder = faceEmbedder;
        this.matchThreshold = matchThreshold;
    }

    public List<DetectedFaceMatch> matchAll(BufferedImage photo, List<MatchCandidate> candidates) {
        List<DetectedFaceMatch> results = new ArrayList<>();
        for (FaceBox box : faceDetector.detectFaces(photo)) {
            BufferedImage crop = ImageUtils.cropWithMargin(photo, box, CROP_MARGIN_RATIO);
            float[] embedding = faceEmbedder.embed(crop);
            results.add(bestMatch(box, embedding, candidates));
        }
        return results;
    }

    private DetectedFaceMatch bestMatch(FaceBox box, float[] embedding, List<MatchCandidate> candidates) {
        Optional<MatchCandidate> best = Optional.empty();
        double bestScore = -1;
        for (MatchCandidate candidate : candidates) {
            double score = FaceVectorMath.cosineSimilarity(embedding, candidate.embedding());
            if (score > bestScore) {
                bestScore = score;
                best = Optional.of(candidate);
            }
        }
        if (best.isPresent() && bestScore >= matchThreshold) {
            return new DetectedFaceMatch(box, embedding, Optional.of(best.get().profileId()), bestScore);
        }
        return new DetectedFaceMatch(box, embedding, Optional.empty(), Math.max(bestScore, 0));
    }
}
