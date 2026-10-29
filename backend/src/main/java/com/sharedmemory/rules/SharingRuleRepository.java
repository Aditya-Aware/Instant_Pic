package com.sharedmemory.rules;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharingRuleRepository extends JpaRepository<SharingRule, UUID> {
    List<SharingRule> findByEventId(UUID eventId);

    Optional<SharingRule> findByEventIdAndRuleType(UUID eventId, RuleType ruleType);

    boolean existsByEventId(UUID eventId);
}
