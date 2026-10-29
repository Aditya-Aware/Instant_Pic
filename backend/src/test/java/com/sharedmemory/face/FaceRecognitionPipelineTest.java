package com.sharedmemory.face;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharedmemory.face.detection.FaceBox;
import com.sharedmemory.face.detection.FaceDetector;
import com.sharedmemory.face.embedding.FaceEmbedder;
import com.sharedmemory.face.matching.DetectedFaceMatch;
import com.sharedmemory.face.matching.EnrollmentResult;
import com.sharedmemory.face.matching.FaceEnrollmentService;
import com.sharedmemory.face.matching.FaceMatcher;
import com.sharedmemory.face.matching.FaceVectorMath;
import com.sharedmemory.face.matching.MatchCandidate;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Standalone proof of the face pipeline (detection -> embedding -> matching) against real
 * photos, with no Spring context and no database — per the plan, this must pass and the match
 * threshold must be empirically validated BEFORE any Drools/rules-engine wiring happens. Fixture
 * images are from the MIT-licensed ageitgey/face_recognition example set (public figures, widely
 * used for exactly this kind of face-recognition testing).
 */
class FaceRecognitionPipelineTest {

    private static final Path FIXTURES =
            Path.of("src/test/resources/fixtures/faces");
    // Matches app.face.match-confidence-threshold's default (application.yml) — see that file
    // for how this was empirically derived.
    private static final double MATCH_THRESHOLD = 0.18;

    private static FaceDetector detector;
    private static FaceEmbedder embedder;
    private static FaceEnrollmentService enrollmentService;
    private static FaceMatcher matcher;

    @BeforeAll
    static void setUp() throws Exception {
        detector = new FaceDetector("models/version-RFB-320.onnx");
        embedder = new FaceEmbedder("models/arcfaceresnet100-8.onnx");
        enrollmentService = new FaceEnrollmentService(detector, embedder);
        matcher = new FaceMatcher(detector, embedder, MATCH_THRESHOLD);
    }

    @Test
    void detectsExactlyOneFaceInASingleObamaPortrait() throws IOException {
        List<FaceBox> faces = detector.detectFaces(loadImage("obama.jpg"));
        assertThat(faces).hasSize(1);
        assertThat(faces.get(0).confidence()).isGreaterThan(0.7);
    }

    @Test
    void detectsTwoFacesInATwoPersonPhoto() throws IOException {
        List<FaceBox> faces = detector.detectFaces(loadImage("two_people.jpg"));
        assertThat(faces).hasSize(2);
    }

    @Test
    void enrollsFromMultipleSelfiesAndMatchesTheSamePersonInADifferentPhoto() throws IOException {
        EnrollmentResult enrollment = enrollmentService.enroll(
                List.of(loadImage("obama.jpg"), loadImage("obama-480p.jpg"), loadImage("obama-720p.jpg")));
        assertThat(enrollment.sampleCount()).isEqualTo(3);
        assertThat(enrollment.embedding()).hasSize(512);

        UUID obamaProfileId = UUID.randomUUID();
        List<MatchCandidate> candidates = List.of(new MatchCandidate(obamaProfileId, enrollment.embedding()));

        List<DetectedFaceMatch> matches = matcher.matchAll(loadImage("obama2.jpg"), candidates);
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).matchedProfileId()).contains(obamaProfileId);
        assertThat(matches.get(0).bestScore()).isGreaterThanOrEqualTo(MATCH_THRESHOLD);
    }

    @Test
    void doesNotMatchADifferentPersonAgainstAnEnrolledProfile() throws IOException {
        EnrollmentResult enrollment = enrollmentService.enroll(
                List.of(loadImage("obama.jpg"), loadImage("obama-480p.jpg")));
        UUID obamaProfileId = UUID.randomUUID();
        List<MatchCandidate> candidates = List.of(new MatchCandidate(obamaProfileId, enrollment.embedding()));

        List<DetectedFaceMatch> matches = matcher.matchAll(loadImage("biden.jpg"), candidates);
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).matchedProfileId()).isEmpty();
    }

    @Test
    void sameIdentityScoresHigherThanDifferentIdentity() throws IOException {
        float[] obamaRef = embedFirstFace(loadImage("obama.jpg"));
        float[] obamaOther = embedFirstFace(loadImage("obama2.jpg"));
        float[] biden = embedFirstFace(loadImage("biden.jpg"));

        double sameIdentityScore = FaceVectorMath.cosineSimilarity(obamaRef, obamaOther);
        double differentIdentityScore = FaceVectorMath.cosineSimilarity(obamaRef, biden);

        assertThat(sameIdentityScore).isGreaterThan(differentIdentityScore);
        assertThat(sameIdentityScore).isGreaterThanOrEqualTo(MATCH_THRESHOLD);
        assertThat(differentIdentityScore).isLessThan(MATCH_THRESHOLD);
    }

    private float[] embedFirstFace(BufferedImage image) {
        FaceBox box = detector.detectFaces(image).get(0);
        return embedder.embed(com.sharedmemory.face.ImageUtils.cropWithMargin(image, box, 0.2));
    }

    private BufferedImage loadImage(String filename) throws IOException {
        return ImageIO.read(FIXTURES.resolve(filename).toFile());
    }
}
