package com.sharedmemory.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

/**
 * Enforces the product's core architectural principle: face matching must not contain sharing
 * rules, and the rules engine must not depend on face-matching internals. Only
 * photoprocessing.PhotoProcessingOrchestrator is allowed to depend on both face.api and
 * rules.api. See README.md.
 */
class ArchitectureTest {

    private static final com.tngtech.archunit.core.domain.JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.sharedmemory");

    @Test
    void faceInternalsMustNotDependOnRules() {
        // allowEmptyShould: face.detection/embedding/matching don't exist until Phase 3 (face
        // pipeline). The rule still runs (and will bite) as soon as those packages gain classes.
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "..face.detection..", "..face.embedding..", "..face.matching..")
                .should().dependOnClassesThat().resideInAPackage("..rules..")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void rulesInternalsMustNotDependOnFaceInternals() {
        // allowEmptyShould: rules.internal doesn't exist until Phase 4 (Drools wiring).
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rules.internal..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..face.detection..", "..face.embedding..", "..face.matching..")
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    void onlyOrchestratorMayDependOnBothFacadeInterfaces() {
        // Deliberately narrower than "depends on anything in face.api and anything in rules.api":
        // RulesEngineFacade's own method signature takes a face.api.PhotoMatchResult, so any
        // implementation of it (e.g. DroolsRulesEngineService) legitimately references that type
        // — that's the contract, not orchestration. The actual smell to catch is a class wiring
        // together BOTH facade *interfaces* (holding/consuming both FaceMatchingFacade and
        // RulesEngineFacade) — that's what only the orchestrator should do.
        ArchRule rule = classes()
                .that().resideOutsideOfPackage("..photoprocessing..")
                .should(new ArchCondition<JavaClass>("not depend on both FaceMatchingFacade and RulesEngineFacade") {
                    @Override
                    public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                        boolean dependsOnFaceFacade = javaClass.getDirectDependenciesFromSelf().stream()
                                .anyMatch(dep -> dep.getTargetClass().getFullName()
                                        .equals("com.sharedmemory.face.api.FaceMatchingFacade"));
                        boolean dependsOnRulesFacade = javaClass.getDirectDependenciesFromSelf().stream()
                                .anyMatch(dep -> dep.getTargetClass().getFullName()
                                        .equals("com.sharedmemory.rules.api.RulesEngineFacade"));
                        if (dependsOnFaceFacade && dependsOnRulesFacade) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    javaClass.getFullName() + " depends on both FaceMatchingFacade and RulesEngineFacade"));
                        }
                    }
                });
        rule.check(CLASSES);
    }
}
