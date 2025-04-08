package com.example.websocket.model;

import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ScheduledFuture;

/** Информация по активной сессии */
public class SessionData {
    private final WebSocketSession session;
    private volatile ScheduledFuture<?> future;
    private volatile String currentSymbol;
    private volatile boolean isRunning;

    public SessionData(WebSocketSession session) {
        this.session = session;
    }

    // геттеры/сеттеры
    public WebSocketSession getSession() { return session; }
    public ScheduledFuture<?> getFuture() { return future; }
    public void setFuture(ScheduledFuture<?> future) { this.future = future; }

    public String getCurrentSymbol() { return currentSymbol; }
    public void setCurrentSymbol(String currentSymbol) { this.currentSymbol = currentSymbol; }

    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }
}