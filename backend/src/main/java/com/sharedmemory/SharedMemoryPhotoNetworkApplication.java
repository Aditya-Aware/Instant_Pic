package com.sharedmemory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SharedMemoryPhotoNetworkApplication {

    public static void main(String[] args) {
        // Set before anything else touches AWT: the face pipeline uses BufferedImage/Graphics2D
        // for image resizing (ImageUtils, FaceDetector, FaceEmbedder), which on macOS otherwise
        // registers this server process with the WindowServer and bounces a Java Dock icon.
        // Spring Boot sets this itself too, but only once ApplicationContext startup begins —
        // setting it explicitly here removes any ordering dependency on that.
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(SharedMemoryPhotoNetworkApplication.class, args);
    }
}
