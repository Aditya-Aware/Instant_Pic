package com.sharedmemory.face;

import com.sharedmemory.face.dto.FaceEnrollmentResponse;
import com.sharedmemory.identity.security.CurrentUserId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/events/{eventId}/members/me/face-enrollment")
public class FaceProfileController {

    private final FaceProfileService faceProfileService;

    public FaceProfileController(FaceProfileService faceProfileService) {
        this.faceProfileService = faceProfileService;
    }

    @PostMapping
    public ResponseEntity<FaceEnrollmentResponse> enroll(
            @PathVariable UUID eventId, @RequestParam("selfies") List<MultipartFile> selfies) {
        FaceProfile profile = faceProfileService.enroll(eventId, CurrentUserId.get(), selfies);
        return ResponseEntity.ok(FaceEnrollmentResponse.from(profile));
    }

    @GetMapping
    public ResponseEntity<FaceEnrollmentResponse> status(@PathVariable UUID eventId) {
        return ResponseEntity.ok(faceProfileService.getStatus(eventId, CurrentUserId.get())
                .map(FaceEnrollmentResponse::from)
                .orElseGet(FaceEnrollmentResponse::notEnrolled));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID eventId) {
        faceProfileService.deleteProfile(eventId, CurrentUserId.get());
        return ResponseEntity.noContent().build();
    }
}
