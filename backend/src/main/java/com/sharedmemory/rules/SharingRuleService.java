package com.sharedmemory.rules;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharingRuleService {

    private final SharingRuleRepository sharingRuleRepository;

    public SharingRuleService(SharingRuleRepository sharingRuleRepository) {
        this.sharingRuleRepository = sharingRuleRepository;
    }

    /**
     * MVP requirement: the default rule plus at least two configurable rules (group/private
     * visibility) are enabled automatically so a new event works out of the box without manual
     * setup. PAIR_RULE/SUBGROUP/PHOTOGRAPHER_MODE/APPROVAL_MODE are seeded disabled — schema-ready
     * for a fast follow, but no DRL implements them yet.
     */
    @Transactional
    public void seedDefaultRules(UUID eventId) {
        for (RuleType type : RuleType.values()) {
            boolean enabledByDefault =
                    type == RuleType.DEFAULT_SELF || type == RuleType.GROUP_PHOTO || type == RuleType.PRIVATE_PHOTO;
            sharingRuleRepository.save(SharingRule.builder()
                    .eventId(eventId)
                    .ruleType(type)
                    .enabled(enabledByDefault)
                    .priority(0)
                    .configJson("{}")
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<SharingRule> list(UUID eventId) {
        return sharingRuleRepository.findByEventId(eventId);
    }

    @Transactional
    public SharingRule setEnabled(UUID eventId, RuleType ruleType, boolean enabled) {
        SharingRule rule = sharingRuleRepository.findByEventIdAndRuleType(eventId, ruleType)
                .orElseThrow(() -> new IllegalArgumentException("No such rule: " + ruleType));
        rule.setEnabled(enabled);
        return sharingRuleRepository.save(rule);
    }
}
