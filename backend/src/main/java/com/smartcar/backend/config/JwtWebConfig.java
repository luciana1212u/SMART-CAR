package com.smartcar.backend.config;

import com.smartcar.backend.services.IAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Toda la API exige JWT salvo el alta y la entrada de usuarios. */
@Configuration
public class JwtWebConfig implements WebMvcConfigurer {
    private final IAuthService auth;

    public JwtWebConfig(IAuthService auth) {
        this.auth = auth;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(auth)).addPathPatterns("/api/**").excludePathPatterns(
                "/api/auth/registro", "/api/auth/login", "/api/auth/refresh", "/api/auth/usuario-disponible");
    }
}
