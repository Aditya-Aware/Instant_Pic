package com.sharedmemory.photoprocessing;

import com.sharedmemory.delivery.Delivery;
import com.sharedmemory.delivery.DeliveryService;
import com.sharedmemory.face.api.FaceMatchingFacade;
import com.sharedmemory.face.api.PhotoMatchResult;
import com.sharedmemory.media.PhotoProcessingStatus;
import com.sharedmemory.media.PhotoService;
import com.sharedmemory.rules.api.RulesEngineFacade;
import com.sharedmemory.rules.api.SharingDecision;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The ONLY class allowed to depend on both face.api and rules.api (enforced by
 * ArchitectureTest). Wires face matching -> sharing rules -> delivery for one photo. Face
 * matching produces participant matches only; the rules engine alone decides distribution — this
 * class does not make sharing decisions itself, it only sequences the two.
 */
@Service
public class PhotoProcessingOrchestrator {

    private final FaceMatchingFacade faceMatchingFacade;
    private final RulesEngineFacade rulesEngineFacade;
    private final DeliveryService deliveryService;
    private final PhotoService photoService;

    public PhotoProcessingOrchestrator(
            FaceMatchingFacade faceMatchingFacade,
            RulesEngineFacade rulesEngineFacade,
            DeliveryService deliveryService,
            PhotoService photoService) {
        this.faceMatchingFacade = faceMatchingFacade;
        this.rulesEngineFacade = rulesEngineFacade;
        this.deliveryService = deliveryService;
        this.photoService = photoService;
    }

    public List<Delivery> process(UUID photoId) {
        try {
            photoService.updateProcessingStatus(photoId, PhotoProcessingStatus.DETECTING);
            PhotoMatchResult matchResult = faceMatchingFacade.detectAndMatch(photoId);

            photoService.updateProcessingStatus(photoId, PhotoProcessingStatus.MATCHING);
            List<SharingDecision> decisions = rulesEngineFacade.evaluate(matchResult);

            List<Delivery> deliveries = deliveryService.applyDecisions(decisions);
            photoService.updateProcessingStatus(photoId, PhotoProcessingStatus.RULES_EVALUATED);
            return deliveries;
        } catch (RuntimeException e) {
            photoService.updateProcessingStatus(photoId, PhotoProcessingStatus.FAILED);
            throw e;
        }
    }
}
