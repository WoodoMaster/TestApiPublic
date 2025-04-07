package com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class RealtimeTickerHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(RealtimeTickerHandler.class);
    private static final String BASE_API_URL = "https://demo-api-adapter.dzengi.com/api/v2";
    private static final String TICKER_URL = BASE_API_URL + "/ticker/24hr";
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int UPDATE_INTERVAL_SECONDS = 5;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int MAX_SESSIONS = 100;

    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>(MAX_SESSIONS);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
            .version(HttpClient.Version.HTTP_2)
            .build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static class SessionData {
        final WebSocketSession session;
        ScheduledFuture<?> future;
        String currentSymbol;

        SessionData(WebSocketSession session) {
            this.session = session;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (activeSessions.size() >= MAX_SESSIONS) {
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
                logger.warn("Rejected new connection - maximum sessions reached");
                return;
            } catch (Exception e) {
                logger.error("Error closing session", e);
            }
        }

        String sessionId = session.getId();
        activeSessions.put(sessionId, new SessionData(session));
        logger.info("New WebSocket connection established: {}", sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        SessionData sessionData = activeSessions.remove(sessionId);
        if (sessionData != null && sessionData.future != null) {
            sessionData.future.cancel(false);
        }
        logger.info("WebSocket connection closed: {}, status: {}", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String symbol = message.getPayload().trim();
        String sessionId = session.getId();
        SessionData sessionData = activeSessions.get(sessionId);

        if (sessionData == null || !session.isOpen()) {
            logger.warn("Received message for non-existent or closed session: {}", sessionId);
            return;
        }

        logger.info("Received symbol subscription: {} from session: {}", symbol, sessionId);

        // Cancel any existing task for this session
        if (sessionData.future != null) {
            sessionData.future.cancel(false);
        }

        // Start new scheduled task for this symbol
        sessionData.currentSymbol = symbol;
        sessionData.future = scheduler.scheduleAtFixedRate(
                () -> updateTickerData(sessionData),
                0, UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS
        );
    }

    private void updateTickerData(SessionData sessionData) {
        if (!sessionData.session.isOpen()) {
            logger.warn("Session {} is closed, stopping updates", sessionData.session.getId());
            return;
        }

        try {
            String tickerData = fetchTickerDataWithRetry(sessionData.currentSymbol);
            if (tickerData != null) {
                synchronized (sessionData.session) {
                    if (sessionData.session.isOpen()) {
                        sessionData.session.sendMessage(new TextMessage(tickerData));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update ticker data for {} (session: {})",
                    sessionData.currentSymbol, sessionData.session.getId(), e);
        }
    }

    private String fetchTickerDataWithRetry(String symbol) {
        int attempt = 0;
        Exception lastError = null;

        while (attempt < MAX_RETRIES) {
            try {
                return fetchTickerData(symbol);
            } catch (Exception e) {
                lastError = e;
                attempt++;
                logger.warn("Attempt {} failed for symbol {}: {}", attempt, symbol, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }

        String errorMsg = "Failed after " + MAX_RETRIES + " attempts for symbol: " + symbol;
        logger.error(errorMsg, lastError);
        return "{\"error\":\"" + errorMsg + "\"}";
    }

    private String fetchTickerData(String symbol) throws Exception {
        String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TICKER_URL + "?symbol=" + encodedSymbol))
                .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", "TickerWebSocketHandler/1.0")
                .build();

        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - startTime;

        logger.debug("API call for {} took {} ms", symbol, duration);

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            // Consider logging the symbol along with the error details
            logger.error("API request failed for symbol {}. Status: {}, Body: {}",
                    symbol, response.statusCode(), response.body());
            throw new RuntimeException("API request failed. Status: " +
                    response.statusCode() + ", Body: " + response.body());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Transport error for session {}: {}", session.getId(), exception.getMessage());
        SessionData sessionData = activeSessions.get(session.getId());
        if (sessionData != null && sessionData.future != null) {
            sessionData.future.cancel(false);
        }
    }
}