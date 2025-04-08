package com.example.websocket.handler;

import com.example.websocket.scheduler.TickerUpdater;
import com.example.websocket.session.SessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ScheduledFuture;
@Component
public class RealtimeTickerHandler extends TextWebSocketHandler {

    private final SessionManager manager;
    private final TickerUpdater updater;

    public RealtimeTickerHandler(SessionManager m, TickerUpdater u){ this.manager=m; this.updater=u; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        manager.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        manager.removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String symbol = message.getPayload().trim();
        var data = manager.getSessionData(session.getId());
        if(data==null || symbol.isEmpty()) return;
        ScheduledFuture<?> oldFuture = data.getFuture();
        if(oldFuture != null) oldFuture.cancel(false);
        var fut = updater.schedule(data, symbol);
        data.setFuture(fut);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable ex){
        manager.removeSession(session);
    }
}