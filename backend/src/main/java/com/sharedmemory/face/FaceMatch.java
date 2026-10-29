package com.sharedmemory.face;

import com.sharedmemory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "face_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FaceMatch extends BaseEntity {

    @Column(name = "photo_face_id", nullable = false)
    private UUID photoFaceId;

    @Column(name = "event_member_id", nullable = false)
    private UUID eventMemberId;

    @Column(name = "face_profile_id", nullable = false)
    private UUID faceProfileId;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    @Builder.Default
    private FaceMatchStatus matchStatus = FaceMatchStatus.CONFIRMED;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;
}
