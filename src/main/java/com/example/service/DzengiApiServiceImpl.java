package com.example.service;

import com.example.service.api.HttpApiClient;
import com.example.service.handler.ApiResponseHandler;

import com.example.util.SignatureUtil;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
@Service
public class DzengiApiServiceImpl implements DzengiApiService {

    private final HttpApiClient http;
    private final ApiResponseHandler handler;

    private static final String ACC_EP = "/api/v1/account";
    private static final String TICKER_EP = "/api/v2/ticker/24hr";
    private static final String EXCH_EP = "/api/v2/exchangeInfo";
    private static final String ORDER_EP = "/api/v1/order";

    public DzengiApiServiceImpl(HttpApiClient http, ApiResponseHandler handler) {
        this.http = http;
        this.handler = handler;
    }

    @Override
    public JsonObject getAccountInfo(String apiKey, String secretKey) throws Exception {
        String ts = "timestamp=" + System.currentTimeMillis();
        String sig = SignatureUtil.generateSignature(ts, secretKey);
        String url = ACC_EP + "?" + ts + "&signature=" + sig;

        var res = http.get(url, apiKey, Map.of("Accept","application/json"));
        return handler.handleResponse(res, "AccountInfo");
    }

    @Override
    public JsonObject getTickerData(String symbol) throws Exception {
        String url = TICKER_EP + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        var res = http.get(url, null, Map.of("Accept","application/json"));
        return handler.handleResponse(res, "TickerData");
    }

    @Override
    public JsonObject getExchangeInfo() throws Exception {
        var res = http.get(EXCH_EP, null, Map.of("Accept","application/json"));
        return handler.handleResponse(res, "ExchangeInfo");
    }

    @Override
    public JsonObject placeOrder(String apiKey, String secretKey, String symbol, String side, String type,
                                 String qty, String price, Long rw) throws Exception {
        Map<String,String> params = new TreeMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", type);
        params.put("quantity", qty);
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        if(price != null && type.equalsIgnoreCase("LIMIT")) params.put("price", price);
        if(rw != null) params.put("recvWindow", rw.toString());

        String qs = params.entrySet().stream()
                .map(e -> e.getKey()+"="+URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        String sig = SignatureUtil.generateSignature(qs, secretKey);
        String url = ORDER_EP + "?" + qs + "&signature=" + sig;

        var res = http.post(url, apiKey, Map.of("Accept","application/json"));
        return handler.handleResponse(res, "PlaceOrder");
    }
}