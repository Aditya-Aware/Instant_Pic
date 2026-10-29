package com.sharedmemory.face;

import com.sharedmemory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "face_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FaceProfile extends BaseEntity {

    @Column(name = "event_member_id", nullable = false, unique = true)
    private UUID eventMemberId;

    @Convert(converter = FloatArrayConverter.class)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private float[] embedding;

    @Column(name = "embedding_model_version", nullable = false)
    private String embeddingModelVersion;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FaceProfileStatus status = FaceProfileStatus.ACTIVE;
}
