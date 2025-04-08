package com.example.websocket.scheduler;

import com.example.websocket.model.SessionData;
import com.example.service.DzengiApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.*;

@Component
public class TickerUpdater {

    private final DzengiApiService api;
    private final ScheduledExecutorService pool;

    @Value("${websocket.update-interval:5}")
    private int intervalSeconds;

    public TickerUpdater(DzengiApiService api) {
        this.api = api;
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.pool = Executors.newScheduledThreadPool(poolSize);
    }

    public ScheduledFuture<?> schedule(SessionData data, String symbol) {
        data.setCurrentSymbol(symbol);
        return pool.scheduleWithFixedDelay(() -> doUpdate(data), 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void doUpdate(SessionData data){
        if(data.isRunning()) return;
        data.setRunning(true);
        try{
            var resp = api.getTickerData(data.getCurrentSymbol());
            send(data.getSession(), resp.toString());
        }catch(Exception e){
            send(data.getSession(), "{\"error\":\""+e.getMessage()+"\"}");
        } finally { data.setRunning(false); }
    }

    private void send(WebSocketSession session, String msg){
        synchronized(session) {
            try {
                if(session.isOpen()) session.sendMessage(new TextMessage(msg));
            } catch(Exception ignored){}
        }
    }

    public void shutdown() { pool.shutdown(); }
}