package com.sharedmemory.rules.internal;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the KieContainer from src/main/resources/rules/sharing-rules.drl directly (KieFileSystem
 * + KieBuilder), no kmodule.xml needed and no kie-spring-boot-starter (that starter is stale
 * relative to core Drools).
 */
@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/sharing-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools build errors: " + kieBuilder.getResults());
        }
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }
}
