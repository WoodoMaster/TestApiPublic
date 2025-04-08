package com.example.config;

import com.example.websocket.handler.RealtimeTickerHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebMvc
@EnableWebSocket
@ComponentScan(basePackages = {
        "com.example.controller",
        "com.example.service",
        "com.example.util",
        "com.example.websocket"   // Включаем все websocket-related классы: handler, session, model, scheduler
})
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer {

    private final RealtimeTickerHandler realtimeTickerHandler;

    public WebConfig(RealtimeTickerHandler realtimeTickerHandler) {
        this.realtimeTickerHandler = realtimeTickerHandler;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        DefaultHandshakeHandler handshakeHandler =
                new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy());

        registry.addHandler(realtimeTickerHandler, "/ws/ticker")
                .setAllowedOrigins("*") // TODO: Ограничить конкретными origin для production
                .setHandshakeHandler(handshakeHandler);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(65536);
        container.setMaxBinaryMessageBufferSize(65536);
        container.setMaxSessionIdleTimeout(300_000L);
        return container;
    }
}