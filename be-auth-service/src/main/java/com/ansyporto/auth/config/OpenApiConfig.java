package com.ansyporto.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Dokumentasi endpoint untuk autentikasi dan otorisasi pengguna")
                        .version("1.0.0")
                )
                .tags(List.of(
                        new Tag().name("Auth").description("Endpoint untuk registrasi, login, logout, verifikasi"),
                        new Tag().name("User").description("Endpoint untuk info pengguna dan manajemen akun")
                ));
    }
}
