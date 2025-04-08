package com.example.websocket.scheduler;

import com.example.websocket.model.SessionData;
import com.example.service.DzengiApiService;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.*;

@Component
public class TickerUpdater {

    private static final Logger logger = LoggerFactory.getLogger(TickerUpdater.class);
    private static final int UPDATE_INTERVAL_SECONDS = 5;

    private final ScheduledExecutorService scheduler;
    private final DzengiApiService dzengiApiService;

    public TickerUpdater(DzengiApiService apiService) {
        this.dzengiApiService = apiService;
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.scheduler = Executors.newScheduledThreadPool(corePoolSize);
    }

    public ScheduledFuture<?> schedule(SessionData sessionData, String symbol) {
        sessionData.setCurrentSymbol(symbol);
        return scheduler.scheduleWithFixedDelay(
                () -> updateTickerData(sessionData),
                0, UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void sendErrorMessage(WebSocketSession session, String errorMessage) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("error", errorMessage);
        sendMessage(session, errorJson.toString());
    }

    private void updateTickerData(SessionData data) {
        if (data.isRunning()) return;
        data.setRunning(true);

        WebSocketSession session = data.getSession();
        String symbol = data.getCurrentSymbol();

        if (symbol == null || !session.isOpen()) {
            if (data.getFuture() != null && !data.getFuture().isDone()) {
                data.getFuture().cancel(true);
            }
            data.setRunning(false);
            return;
        }

        try {
            JsonObject tickerJson = dzengiApiService.getTickerData(symbol);

            if (tickerJson.has("error_code") || tickerJson.has("code")) {
                String errMsg = tickerJson.has("error_message") ? tickerJson.get("error_message").getAsString()
                        : tickerJson.has("msg") ? tickerJson.get("msg").getAsString() : "Unknown API error";
                sendErrorMessage(session, "API Error: " + errMsg);
            } else {
                sendMessage(session, tickerJson.toString());
            }
        } catch (Exception e) {
            logger.error("Error updating ticker for {}: {}", symbol, e.getMessage());
            sendErrorMessage(session, "Failed to fetch ticker data");
        } finally {
            data.setRunning(false);
        }
    }

    private void sendMessage(WebSocketSession session, String payload) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    logger.error("Failed to send message", e);
                }
            }
        }
    }

}