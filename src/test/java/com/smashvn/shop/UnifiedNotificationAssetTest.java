package com.smashvn.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class UnifiedNotificationAssetTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources");

    @Test
    void commonLayoutsLoadTheUnifiedNotificationComponent() throws IOException {
        String publicHeader = read("templates/layout/header.html");
        String adminHeader = read("templates/admin/layout/header.html");

        assertTrue(publicHeader.contains("/js/notifications.js"));
        assertTrue(publicHeader.contains("data-smash-server-notification"));
        assertTrue(adminHeader.contains("/js/notifications.js"));
        assertTrue(adminHeader.contains("data-smash-server-notification"));
    }

    @Test
    void notificationRendererUsesSafeTextAndProvidesSharedConfirm() throws IOException {
        String notifications = read("static/js/notifications.js");

        assertTrue(notifications.contains("messageElement.textContent = message"));
        assertTrue(notifications.contains("confirm: confirmAction"));
        assertTrue(notifications.contains("window.showToast = legacyShowToast"));
        assertFalse(notifications.contains("window.alert ="));
    }

    @Test
    void uiSourcesDoNotUseNativeAlertsOrDiagnosticConsoleLogs() throws IOException {
        Path staticJs = RESOURCES.resolve("static/js");
        Path templates = RESOURCES.resolve("templates");

        try (Stream<Path> paths = Stream.concat(Files.walk(staticJs), Files.walk(templates))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".js") || path.toString().endsWith(".html"))
                    .forEach(path -> {
                        String content = readUnchecked(path);
                        assertFalse(content.matches("(?s).*\\balert\\s*\\(.*"),
                                () -> "Native alert còn tồn tại trong " + path);
                        assertFalse(content.contains("console.log("),
                                () -> "Console log chẩn đoán còn tồn tại trong " + path);
                    });
        }
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private String readUnchecked(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc " + path, exception);
        }
    }
}
