package com.sharedmemory.face;

import com.sharedmemory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "photo_faces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PhotoFace extends BaseEntity {

    @Column(name = "photo_id", nullable = false)
    private UUID photoId;

    @Column(name = "bounding_box_x", nullable = false)
    private int boundingBoxX;

    @Column(name = "bounding_box_y", nullable = false)
    private int boundingBoxY;

    @Column(name = "bounding_box_w", nullable = false)
    private int boundingBoxW;

    @Column(name = "bounding_box_h", nullable = false)
    private int boundingBoxH;

    @Column(name = "detection_confidence", nullable = false)
    private double detectionConfidence;

    @Convert(converter = FloatArrayConverter.class)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private float[] embedding;
}
