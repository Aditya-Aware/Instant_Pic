package com.sharedmemory.delivery;

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
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Delivery extends BaseEntity {

    @Column(name = "photo_id", nullable = false)
    private UUID photoId;

    @Column(name = "recipient_member_id", nullable = false)
    private UUID recipientMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(nullable = false)
    private String reason;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;
}
