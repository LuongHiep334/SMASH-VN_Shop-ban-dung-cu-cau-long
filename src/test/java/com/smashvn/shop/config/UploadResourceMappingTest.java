package com.smashvn.shop.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadResourceMappingTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {
        "product/racket.png",
        "product/Vợt cầu lông/Yonex/Nanoflare 700 Pro/anh.png"
    })
    void servesImagesFromConfiguredUploadDirectory(String imagePath) throws Exception {
        Path uploadRoot = Files.createDirectories(tempDir.resolve("Ảnh sản phẩm"));
        Path imageFile = uploadRoot.resolve(imagePath);
        Files.createDirectories(imageFile.getParent());
        byte[] imageBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=");
        Files.write(imageFile, imageBytes);

        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "testUploadPath", Map.of("app.upload.path", uploadRoot.toString())));
            context.register(TestConfig.class);
            context.refresh();

            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
            mockMvc.perform(get("/uploads/" + imagePath))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(imageBytes));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(WebMvcConfig.class)
    static class TestConfig {
        @Bean
        AdminInterceptor adminInterceptor() {
            return mock(AdminInterceptor.class);
        }
    }
}
