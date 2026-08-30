package com.bcb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera CORS pra chamar a API direto do browser sem passar pelo proxy do servidor Express
 * (ng serve com proxy.conf.json desligado, ou o "Try it out" do Swagger UI). Usa
 * allowedOriginPatterns (não allowedOrigins) porque o domínio público do Codespaces é dinâmico
 * por sessão (ex.: https://<nome-aleatório>-8080.app.github.dev) — não dá pra fixar um valor só.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:4200", "https://*.app.github.dev", "https://*.github.dev")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
