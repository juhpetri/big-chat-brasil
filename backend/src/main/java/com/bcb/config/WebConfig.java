package com.bcb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera CORS pra chamar a API direto do browser sem passar pelo proxy do servidor Express
 * (ng serve com proxy.conf.json desligado, ou o "Try it out" do Swagger UI). Uma allowlist de
 * padrões de domínio (ex.: *.app.github.dev) se mostrou frágil na prática pra encaminhamento de
 * porta do Codespaces — libera geral em vez disso. Isso é seguro aqui porque a autenticação é
 * via Bearer token explícito por request (ver AuthTokenFilter), não cookie de sessão: não tem
 * credencial ambiente pro browser vazar automaticamente pra origem nenhuma.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
