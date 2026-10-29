package com.sharedmemory.rules.internal;

import com.sharedmemory.face.api.PhotoMatchResult;
import com.sharedmemory.media.PhotoService;
import com.sharedmemory.rules.SharingRule;
import com.sharedmemory.rules.SharingRuleRepository;
import com.sharedmemory.rules.api.RulesEngineFacade;
import com.sharedmemory.rules.api.SharingDecision;
import com.sharedmemory.rules.api.VisibilityDecision;
import com.sharedmemory.rules.internal.facts.MatchedMember;
import com.sharedmemory.rules.internal.facts.PhotoContext;
import com.sharedmemory.rules.internal.facts.SharingRuleConfig;
import java.util.ArrayList;
import java.util.List;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DroolsRulesEngineService implements RulesEngineFacade {

    private final KieContainer kieContainer;
    private final SharingRuleRepository sharingRuleRepository;
    private final PhotoService photoService;

    public DroolsRulesEngineService(
            KieContainer kieContainer, SharingRuleRepository sharingRuleRepository, PhotoService photoService) {
        this.kieContainer = kieContainer;
        this.sharingRuleRepository = sharingRuleRepository;
        this.photoService = photoService;
    }

    @Override
    @Transactional
    public List<SharingDecision> evaluate(PhotoMatchResult matchResult) {
        StatelessKieSession session = kieContainer.newStatelessKieSession();
        List<SharingDecision> decisions = new ArrayList<>();
        List<VisibilityDecision> visibilityDecisions = new ArrayList<>();
        session.setGlobal("decisions", decisions);
        session.setGlobal("visibilityDecisions", visibilityDecisions);

        List<Object> facts = new ArrayList<>();
        facts.add(toPhotoContext(matchResult));
        for (SharingRule rule : sharingRuleRepository.findByEventId(matchResult.eventId())) {
            facts.add(new SharingRuleConfig(rule.getEventId(), rule.getRuleType().name(), rule.isEnabled()));
        }

        session.execute(facts);

        applyVisibility(matchResult, visibilityDecisions);
        return decisions.stream().distinct().toList();
    }

    private PhotoContext toPhotoContext(PhotoMatchResult matchResult) {
        // Phase 5 (privacy controls) will wire per-category opt-out via PrivacyConsent; for now
        // every match reaching here already comes from an active member (see
        // FaceMatchingServiceImpl), so optedOut is always false.
        List<MatchedMember> matchedMembers = matchResult.matches().stream()
                .map(m -> new MatchedMember(m.eventMemberId(), m.confidence(), false))
                .toList();
        return new PhotoContext(matchResult.photoId(), matchResult.eventId(), matchedMembers);
    }

    private void applyVisibility(PhotoMatchResult matchResult, List<VisibilityDecision> visibilityDecisions) {
        boolean galleryVisible = visibilityDecisions.stream().anyMatch(VisibilityDecision::galleryVisible);
        photoService.setGalleryVisible(matchResult.photoId(), galleryVisible);
    }
}
