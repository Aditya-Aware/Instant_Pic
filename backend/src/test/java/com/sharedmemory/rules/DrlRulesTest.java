package com.sharedmemory.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharedmemory.rules.api.SharingDecision;
import com.sharedmemory.rules.api.VisibilityDecision;
import com.sharedmemory.rules.internal.DroolsConfig;
import com.sharedmemory.rules.internal.facts.MatchedMember;
import com.sharedmemory.rules.internal.facts.PhotoContext;
import com.sharedmemory.rules.internal.facts.SharingRuleConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;

/**
 * Proves the Drools dependency set actually resolves and the DRL compiles/executes (the "hello
 * rule" sanity step from the plan) AND that the three real MVP rules produce the right decisions
 * — entirely standalone, no Spring context, no database.
 */
class DrlRulesTest {

    private static KieContainer kieContainer;

    @BeforeAll
    static void buildContainer() {
        kieContainer = new DroolsConfig().kieContainer();
    }

    @Test
    void privatePhotoDeliversToTheSoleMemberAndHidesFromGallery() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        Result result = run(
                new PhotoContext(photoId, eventId, List.of(new MatchedMember(memberId, 0.9, false))),
                new SharingRuleConfig(eventId, "PRIVATE_PHOTO", true),
                new SharingRuleConfig(eventId, "GROUP_PHOTO", true));

        assertThat(result.decisions).hasSize(1);
        assertThat(result.decisions.get(0).recipientMemberId()).isEqualTo(memberId);
        assertThat(result.decisions.get(0).reason()).isEqualTo("DEFAULT_SELF");
        assertThat(result.visibility).hasSize(1);
        assertThat(result.visibility.get(0).galleryVisible()).isFalse();
        assertThat(result.visibility.get(0).ruleName()).isEqualTo("private-photo-rule");
    }

    @Test
    void groupPhotoDeliversToEveryoneAndIsVisibleInGallery() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        UUID m3 = UUID.randomUUID();

        Result result = run(
                new PhotoContext(photoId, eventId, List.of(
                        new MatchedMember(m1, 0.9, false),
                        new MatchedMember(m2, 0.85, false),
                        new MatchedMember(m3, 0.8, false))),
                new SharingRuleConfig(eventId, "PRIVATE_PHOTO", true),
                new SharingRuleConfig(eventId, "GROUP_PHOTO", true));

        assertThat(result.decisions).hasSize(3);
        assertThat(result.visibility).hasSize(1);
        assertThat(result.visibility.get(0).galleryVisible()).isTrue();
        assertThat(result.visibility.get(0).ruleName()).isEqualTo("group-photo-rule");
    }

    @Test
    void twoMatchedMembersFallsBackToDefaultPersonalVisibility() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();

        Result result = run(
                new PhotoContext(photoId, eventId, List.of(
                        new MatchedMember(m1, 0.9, false), new MatchedMember(m2, 0.85, false))),
                new SharingRuleConfig(eventId, "PRIVATE_PHOTO", true),
                new SharingRuleConfig(eventId, "GROUP_PHOTO", true));

        assertThat(result.decisions).hasSize(2);
        assertThat(result.visibility).hasSize(1);
        assertThat(result.visibility.get(0).galleryVisible()).isFalse();
        assertThat(result.visibility.get(0).ruleName()).isEqualTo("default-visibility");
    }

    @Test
    void optedOutMemberIsExcludedFromDelivery() {
        UUID eventId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        Result result = run(
                new PhotoContext(photoId, eventId, List.of(new MatchedMember(member, 0.9, true))),
                new SharingRuleConfig(eventId, "PRIVATE_PHOTO", true));

        assertThat(result.decisions).isEmpty();
    }

    private Result run(PhotoContext photoContext, SharingRuleConfig... ruleConfigs) {
        StatelessKieSession session = kieContainer.newStatelessKieSession();
        List<SharingDecision> decisions = new ArrayList<>();
        List<VisibilityDecision> visibility = new ArrayList<>();
        session.setGlobal("decisions", decisions);
        session.setGlobal("visibilityDecisions", visibility);

        List<Object> facts = new ArrayList<>();
        facts.add(photoContext);
        facts.addAll(List.of(ruleConfigs));
        session.execute(facts);

        return new Result(decisions, visibility);
    }

    private record Result(List<SharingDecision> decisions, List<VisibilityDecision> visibility) {
    }
}
