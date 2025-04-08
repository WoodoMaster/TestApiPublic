package com.example.service;

import com.google.gson.JsonArray;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SymbolServiceImpl implements SymbolService {

    private final DzengiApiService api;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile List<String> cached = List.of();
    private volatile long last = 0;

    public SymbolServiceImpl(DzengiApiService a){ api=a; }

    @Override
    public List<String> getAvailableSymbols() {
        long now = System.currentTimeMillis();
        if(now - last < 15*60*1000 && !cached.isEmpty()) return cached;
        if(lock.tryLock()){
            try{
                if(now - last < 15*60*1000 && !cached.isEmpty()) return cached;
                cached = load();
                last = System.currentTimeMillis();
            }finally{ lock.unlock(); }
        }
        return cached;
    }

    private List<String> load() {
        try{
            var res = api.getExchangeInfo();
            JsonArray arr = res.has("symbols") ? res.getAsJsonArray("symbols") : null;
            if(arr == null) return List.of();
            List<String> ls = new ArrayList<>();
            for(var el: arr){
                if(el.isJsonObject() && el.getAsJsonObject().has("symbol"))
                    ls.add(el.getAsJsonObject().get("symbol").getAsString());
            }
            Collections.sort(ls);
            return ls;
        } catch(Exception e){
            return List.of();
        }
    }
}