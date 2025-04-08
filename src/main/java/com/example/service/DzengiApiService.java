package com.example.service;
import com.google.gson.JsonObject; // Or specific DTOs if you prefer

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface DzengiApiService {

    /**
     * Fetches account information from the Dzengi API.
     *
     * @param apiKey    The user's API key.
     * @param secretKey The user's secret key.
     * @return JsonObject containing the account information or an error structure.
     * @throws Exception If an error occurs during the API call.
     */
    JsonObject getAccountInfo(String apiKey, String secretKey) throws Exception;

    /**
     * Fetches 24hr ticker data for a specific symbol.
     *
     * @param symbol The trading symbol (e.g., "BTC/USD_LEVERAGE").
     * @return JsonObject containing the ticker data or an error structure.
     * @throws Exception If an error occurs during the API call.
     */
    JsonObject getTickerData(String symbol) throws Exception;

    /**
     * Fetches exchange information, including available symbols.
     * This typically doesn't require authentication for symbol listing.
     *
     * @return JsonObject containing the exchange information.
     * @throws Exception If an error occurs during the API call.
     */
    JsonObject getExchangeInfo() throws Exception;

    /**
     * Places an order on the Dzengi exchange.
     *
     * @param apiKey    The user's API key.
     * @param secretKey The user's secret key.
     * @param symbol    The trading symbol (e.g., "BTC/USD_LEVERAGE").
     * @param side      "BUY" or "SELL".
     * @param type      Order type (e.g., "LIMIT", "MARKET").
     * @param quantity  The amount to buy/sell.
     * @param price     The price for LIMIT orders (can be null for MARKET).
     * @param recvWindow Optional receive window.
     * @return JsonObject containing the order response or an error structure.
     * @throws Exception If an error occurs during the API call or signature generation.
     */
    JsonObject placeOrder(String apiKey, String secretKey, String symbol, String side, String type,
                          String quantity, String price, Long recvWindow) throws Exception;
}
