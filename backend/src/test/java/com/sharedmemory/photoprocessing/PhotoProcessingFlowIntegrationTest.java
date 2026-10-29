package com.sharedmemory.photoprocessing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharedmemory.BaseIntegrationTest;
import com.sharedmemory.event.dto.CreateEventRequest;
import com.sharedmemory.event.dto.EventResponse;
import com.sharedmemory.identity.dto.AuthResponse;
import com.sharedmemory.identity.dto.RegisterRequest;
import com.sharedmemory.media.dto.PhotoResponse;
import com.sharedmemory.membership.dto.EventMemberResponse;
import com.sharedmemory.membership.dto.InvitationResponse;
import com.sharedmemory.membership.dto.JoinEventRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

/**
 * Full-stack proof of the MVP loop with REAL photos (no synthetic matches): register two users
 * with genuinely different faces (the same Obama/Biden fixtures from FaceRecognitionPipelineTest,
 * MIT-licensed ageitgey/face_recognition demo set), enroll each from real selfies, upload real
 * photos, and verify detection -> per-recipient matching -> Drools rules -> Delivery -> gallery
 * visibility -> My Moments all wire together correctly through real HTTP + Postgres + ONNX +
 * Drools.
 *
 * <p>The >=3-matched-member "group photo" branch is proven at the rules-engine level in
 * DrlRulesTest — this fixture set only has two distinct real identities available, so it isn't
 * re-proven with a real photo here. What this test uniquely proves that DrlRulesTest can't: that
 * a real photo containing two different real faces gets each face matched to the *correct*
 * enrolled profile (not just counted).
 */
class PhotoProcessingFlowIntegrationTest extends BaseIntegrationTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures/faces");

    @Test
    void privatePhotoOfOneEnrolledMemberIsDeliveredButHiddenFromGallery() throws Exception {
        AuthResponse alice = register("alice");
        EventResponse event = createEvent(alice);
        enrollFace(event.id(), alice, "obama.jpg", "obama-480p.jpg", "obama-720p.jpg");

        PhotoResponse photo = uploadPhoto(event.id(), alice, "obama2.jpg");

        assertThat(photo.processingStatus()).isEqualTo("RULES_EVALUATED");
        assertThat(photo.galleryVisible()).isFalse();

        List<PhotoResponse> myMoments = getMyMoments(event.id(), alice);
        assertThat(myMoments).extracting(PhotoResponse::id).contains(photo.id());

        List<PhotoResponse> gallery = getGallery(event.id(), alice);
        assertThat(gallery).extracting(PhotoResponse::id).doesNotContain(photo.id());
    }

    @Test
    void twoDifferentRealFacesEachMatchTheCorrectRecipientAndFallToDefaultVisibility() throws Exception {
        AuthResponse alice = register("alice");
        EventResponse event = createEvent(alice);
        enrollFace(event.id(), alice, "obama.jpg", "obama-480p.jpg");

        AuthResponse bob = register("bob");
        joinEvent(alice, bob, event.id());
        enrollFace(event.id(), bob, "biden.jpg");

        PhotoResponse photo = uploadPhoto(event.id(), alice, "two_people.jpg");

        assertThat(photo.processingStatus()).isEqualTo("RULES_EVALUATED");
        // 2 matched members hits neither the private(==1) nor group(>=3) rule -> default: hidden.
        assertThat(photo.galleryVisible()).isFalse();

        List<PhotoResponse> aliceMoments = getMyMoments(event.id(), alice);
        List<PhotoResponse> bobMoments = getMyMoments(event.id(), bob);
        assertThat(aliceMoments).extracting(PhotoResponse::id).contains(photo.id());
        assertThat(bobMoments).extracting(PhotoResponse::id).contains(photo.id());
    }

    private AuthResponse register(String namePrefix) throws IOException, InterruptedException {
        String email = namePrefix + ThreadLocalRandom.current().nextInt(1_000_000) + "@test.com";
        var response = post("/api/auth/register", null, new RegisterRequest(email, "password123", namePrefix),
                AuthResponse.class);
        assertThat(response.status()).isEqualTo(201);
        return response.body();
    }

    private EventResponse createEvent(AuthResponse user) throws IOException, InterruptedException {
        var response = post("/api/events", user.token(),
                new CreateEventRequest("Face Pipeline Test Event", null, null, null), EventResponse.class);
        assertThat(response.status()).isEqualTo(201);
        return response.body();
    }

    private void joinEvent(AuthResponse organizer, AuthResponse joiner, UUID eventId)
            throws IOException, InterruptedException {
        var invitation = post("/api/events/" + eventId + "/invitations", organizer.token(), null,
                InvitationResponse.class);
        assertThat(invitation.status()).isEqualTo(201);
        var joinResponse = post("/api/events/join", joiner.token(),
                new JoinEventRequest(invitation.body().code(), "joiner"), EventMemberResponse.class);
        assertThat(joinResponse.status()).isEqualTo(200);
    }

    private void enrollFace(UUID eventId, AuthResponse user, String... selfieFilenames) throws Exception {
        String boundary = "----boundary" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, "selfies", selfieFilenames);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/events/" + eventId + "/members/me/face-enrollment"))
                .header("Authorization", "Bearer " + user.token())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    }

    private PhotoResponse uploadPhoto(UUID eventId, AuthResponse user, String filename) throws Exception {
        String boundary = "----boundary" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, "photo", filename);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/events/" + eventId + "/photos"))
                .header("Authorization", "Bearer " + user.token())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return objectMapper.readValue(response.body(), PhotoResponse.class);
    }

    private List<PhotoResponse> getMyMoments(UUID eventId, AuthResponse user) throws Exception {
        var response = get("/api/events/" + eventId + "/members/me/my-moments", user.token(), PhotoResponse[].class);
        assertThat(response.status()).isEqualTo(200);
        return List.of(response.body());
    }

    private List<PhotoResponse> getGallery(UUID eventId, AuthResponse user) throws Exception {
        var response = get("/api/events/" + eventId + "/gallery", user.token(), PhotoResponse[].class);
        assertThat(response.status()).isEqualTo(200);
        return List.of(response.body());
    }

    private byte[] multipartBody(String boundary, String fieldName, String... filenames) throws IOException {
        var out = new java.io.ByteArrayOutputStream();
        for (String filename : filenames) {
            out.writeBytes(("--" + boundary + "\r\n").getBytes());
            out.writeBytes(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename
                    + "\"\r\n").getBytes());
            out.writeBytes("Content-Type: image/jpeg\r\n\r\n".getBytes());
            out.writeBytes(Files.readAllBytes(FIXTURES.resolve(filename)));
            out.writeBytes("\r\n".getBytes());
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes());
        return out.toByteArray();
    }
}
