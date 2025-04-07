package com.example.config;

import com.example.controller.RealtimeTickerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket; // Добавлен импорт
import org.springframework.web.socket.config.annotation.WebSocketConfigurer; // Добавлен импорт
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry; // Добавлен импорт
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebMvc // От WebConfig
@EnableWebSocket // От WebSocketConfig
@ComponentScan(basePackages = "com.example.controller") // От WebConfig
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer { // Реализуем оба интерфейса

    // --- Конфигурация MVC (из старого WebConfig) ---

    // Настройка разрешения представлений для JSP
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    // Настройка статических ресурсов
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Разрешаем доступ к статическим ресурсам в папке /webapp/resources/
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Autowired
    private RealtimeTickerHandler tickerHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tickerHandler, "/ws/ticker")
                .setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler(
                        new TomcatRequestUpgradeStrategy()));
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(65536);
        container.setMaxBinaryMessageBufferSize(65536);
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes
        return container;
    }



}