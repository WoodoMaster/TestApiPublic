package com.example.websocket.session;

import com.example.websocket.model.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Управление всеми сессиями и лимит */
@Component
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final int MAX_SESSIONS = 100;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>(MAX_SESSIONS);

    public boolean registerSession(WebSocketSession session) {
        if (sessions.size() >= MAX_SESSIONS) {
            try {
                logger.warn("Max sessions reached. Rejecting connection: {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Max sessions reached"));
            } catch (IOException e) {
                logger.error("Error closing session {} due to max limit", session.getId(), e);
            }
            return false;
        }
        sessions.put(session.getId(), new SessionData(session));
        return true;
    }

    public void removeSession(WebSocketSession session) {
        SessionData data = sessions.remove(session.getId());
        if (data != null && data.getFuture() != null) {
            data.getFuture().cancel(true);
        }
    }

    public SessionData getSessionData(String sessionId) {
        return sessions.get(sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}