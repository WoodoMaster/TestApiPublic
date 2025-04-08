package com.example.service;

import com.example.service.handler.ApiResponseHandler;
import com.example.service.http.HttpApiClient;
import com.example.util.SignatureUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DzengiApiServiceImpl implements DzengiApiService {
    private static final Logger logger = LoggerFactory.getLogger(DzengiApiServiceImpl.class);

    private final HttpApiClient httpClient;
    private final ApiResponseHandler responseHandler;

    private static final String ACCOUNT_ENDPOINT = "/api/v1/account";
    private static final String TICKER_ENDPOINT = "/api/v2/ticker/24hr";
    private static final String EXCHANGE_INFO_ENDPOINT = "/api/v2/exchangeInfo";
    private static final String ORDER_ENDPOINT = "/api/v1/order";

    public DzengiApiServiceImpl() {
        this.httpClient = new HttpApiClient("https://demo-api-adapter.dzengi.com");
        this.responseHandler = new ApiResponseHandler();
    }

    @Override
    public JsonObject getAccountInfo(String apiKey, String secretKey) throws Exception {
        logger.debug("Requesting account info for API Key: {}", maskApiKey(apiKey));

        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp;
        String signature = SignatureUtil.generateSignature(queryString, secretKey);
        String fullUrl = ACCOUNT_ENDPOINT + "?" + queryString + "&signature=" + signature;

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        JsonObject response = httpClient.executeGetRequest(fullUrl, apiKey, headers, 10000, 10000);
        return responseHandler.handleResponse(response, 200, "Account Info");
    }

    @Override
    public JsonObject getTickerData(String symbol) throws Exception {
        logger.debug("Requesting ticker data for symbol: {}", symbol);
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String encodedSymbol = urlEncode(symbol);
        String fullUrl = TICKER_ENDPOINT + "?symbol=" + encodedSymbol;

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        JsonObject response = httpClient.executeGetRequest(fullUrl, null, headers, 5000, 5000);
        return responseHandler.handleResponse(response, 200, "Ticker Data for " + symbol);
    }

    @Override
    public JsonObject getExchangeInfo() throws Exception {
        logger.debug("Requesting exchange info");

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        JsonObject response = httpClient.executeGetRequest(EXCHANGE_INFO_ENDPOINT, null, headers, 15000, 15000);
        return responseHandler.handleResponse(response, 200, "Exchange Info");
    }

    @Override
    public JsonObject placeOrder(String apiKey, String secretKey, String symbol, String side, String type,
                                 String quantity, String price, Long recvWindow) throws Exception {
        logger.info("Attempting to place {} {} order for {} quantity {} @ price {}",
                side, type, symbol, quantity, (price == null ? "MARKET" : price));

        long timestamp = System.currentTimeMillis();
        Map<String, String> params = buildOrderParams(symbol, side, type, quantity, price, recvWindow, timestamp);
        String queryString = buildQueryString(params);
        String signature = SignatureUtil.generateSignature(queryString, secretKey);

        String fullUrl = ORDER_ENDPOINT + "?" + queryString + "&signature=" + signature;
        logger.debug("Full Order URL: {}", fullUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        JsonObject response = httpClient.executePostRequest(fullUrl, apiKey, headers, 15000, 15000);
        return responseHandler.handleResponse(response, 200, String.format("Place Order (%s %s)", side, symbol));
    }

    private Map<String, String> buildOrderParams(String symbol, String side, String type, String quantity,
                                                 String price, Long recvWindow, long timestamp) {
        Map<String, String> params = new TreeMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("type", type);
        params.put("quantity", quantity);
        params.put("timestamp", String.valueOf(timestamp));

        if (price != null && ("LIMIT".equalsIgnoreCase(type) || "STOP_LIMIT".equalsIgnoreCase(type))) {
            params.put("price", price);
        }

        if (recvWindow != null) {
            params.put("recvWindow", String.valueOf(recvWindow));
        }

        return params;
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + value, e);
        }
    }

    private String maskApiKey(String apiKey) {
        return apiKey.substring(0, Math.min(apiKey.length(), 8)) + "...";
    }
}