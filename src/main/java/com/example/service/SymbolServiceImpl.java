package com.example.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
// Optional: Add caching annotations if using Spring Cache
// import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SymbolServiceImpl implements SymbolService {

    private static final Logger logger = LoggerFactory.getLogger(SymbolServiceImpl.class);

    private final DzengiApiService dzengiApiService;

    // Simple volatile variable for caching symbols (or use Spring Cache)
    private volatile List<String> cachedSymbols = null;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile long lastRefreshTime = 0;
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

    @Autowired
    public SymbolServiceImpl(DzengiApiService dzengiApiService) {
        this.dzengiApiService = dzengiApiService;
    }

    // Optional: Add @Cacheable("symbols") if using Spring Cache
    @Override
    public List<String> getAvailableSymbols() {
        long now = System.currentTimeMillis();
        if (cachedSymbols != null && (now - lastRefreshTime < CACHE_TTL_MS)) {
            logger.debug("Returning cached symbols.");
            return cachedSymbols;
        }

        // Attempt to refresh cache, but only one thread should do it at a time
        if (refreshLock.tryLock()) {
            try {
                // Double-check after acquiring the lock
                if (cachedSymbols == null || (now - lastRefreshTime >= CACHE_TTL_MS)) {
                    logger.info("Refreshing symbols list from API...");
                    cachedSymbols = fetchSymbolsFromApi();
                    lastRefreshTime = System.currentTimeMillis();
                }
            } finally {
                refreshLock.unlock();
            }
        } else {
            logger.debug("Refresh already in progress, returning potentially stale cache.");
            // Another thread is refreshing, return current cache (might be slightly stale)
            // Or wait on the lock if immediate freshness is critical (adds latency potential)
        }

        return cachedSymbols != null ? cachedSymbols : Collections.emptyList();
    }

    private List<String> fetchSymbolsFromApi() {
        try {
            JsonObject exchangeInfo = dzengiApiService.getExchangeInfo();

            // Check if the API service returned an error structure
            if (exchangeInfo.has("error_code") || exchangeInfo.has("code")) {
                logger.error("Failed to fetch exchange info: {}", exchangeInfo.toString());
                return Collections.emptyList(); // Return empty on error
            }


            // Check common locations for the symbols array
            JsonArray symbolsArray = null;
            if (exchangeInfo.has("symbols") && exchangeInfo.get("symbols").isJsonArray()) {
                symbolsArray = exchangeInfo.getAsJsonArray("symbols");
            } else if (exchangeInfo.has("data") && exchangeInfo.get("data").isJsonArray()) {
                // Sometimes data might be wrapped differently
                symbolsArray = exchangeInfo.getAsJsonArray("data");
            } else {
                logger.warn("Exchange info response does not contain a known 'symbols' or 'data' array. Response: {}", exchangeInfo.toString());
                return Collections.emptyList();
            }


            List<String> symbolNames = new ArrayList<>();
            for (JsonElement element : symbolsArray) {
                if (element.isJsonObject()) {
                    JsonObject symbolObject = element.getAsJsonObject();
                    if (symbolObject.has("symbol") && symbolObject.get("symbol").isJsonPrimitive()) {
                        symbolNames.add(symbolObject.get("symbol").getAsString());
                    }
                } else {
                    logger.warn("Element in symbols array is not a JSON object: {}", element);
                }
            }
            Collections.sort(symbolNames);
            logger.info("Successfully fetched {} symbols from API.", symbolNames.size());
            return symbolNames;

        } catch (Exception e) {
            logger.error("Exception fetching available symbols", e);
            return Collections.emptyList();
        }
    }
}