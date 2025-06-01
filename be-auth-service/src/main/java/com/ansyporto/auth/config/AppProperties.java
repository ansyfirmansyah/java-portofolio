package com.ansyporto.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String verificationUrl;
    private String frontendBaseUrl;
    private String mailFrom;

    @PostConstruct
    public void validate() {
        if (verificationUrl == null || verificationUrl.isBlank()) {
            throw new IllegalStateException("Missing required property: app.verification-url");
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("Missing required property: app.mail-from");
        }
    }
}
