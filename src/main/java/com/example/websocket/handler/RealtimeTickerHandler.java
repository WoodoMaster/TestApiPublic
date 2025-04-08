package com.example.websocket.handler;

import com.example.websocket.model.SessionData;
import com.example.websocket.scheduler.TickerUpdater;
import com.example.websocket.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledFuture;

@Component
public class RealtimeTickerHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeTickerHandler.class);

    private final SessionManager sessionManager;
    private final TickerUpdater tickerUpdater;

    @Autowired
    public RealtimeTickerHandler(SessionManager sessionManager, TickerUpdater tickerUpdater) {
        this.sessionManager = sessionManager;
        this.tickerUpdater = tickerUpdater;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (sessionManager.registerSession(session)) {
            logger.info("Connected: {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session);
        logger.info("Disconnected: {} Status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String symbol = message.getPayload().trim();
        String sessionId = session.getId();
        SessionData data = sessionManager.getSessionData(sessionId);

        if (data == null) {
            logger.warn("Non-existent session: {}", sessionId);
            closeSilently(session, CloseStatus.BAD_DATA.withReason("Session not found"));
            return;
        }
        if (symbol.isEmpty()) {
            sendError(data.getSession(), "Symbol cannot be empty");
            return;
        }

        logger.info("Subscription from {}: '{}'", sessionId, symbol);

        if (data.getFuture() != null) {
            data.getFuture().cancel(false);
        }
        ScheduledFuture<?> future = tickerUpdater.schedule(data, symbol);
        data.setFuture(future);
    }

    private void closeSilently(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {}
    }

    private void sendError(WebSocketSession session, String msg) {
        tickerUpdater.sendErrorMessage(session, msg);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Transport error on {}: {}", session.getId(), exception.getMessage());
        afterConnectionClosed(session, CloseStatus.PROTOCOL_ERROR);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ticker scheduler...");
        tickerUpdater.shutdown();
    }
}