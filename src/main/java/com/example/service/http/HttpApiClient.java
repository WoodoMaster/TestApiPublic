package com.example.service.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpApiClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpApiClient.class);

    private final String baseUrl;

    public HttpApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonObject executeGetRequest(String endpoint, String apiKey, Map<String, String> headers,
                                        int connectTimeout, int readTimeout) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (apiKey != null) {
                con.setRequestProperty("X-MBX-APIKEY", apiKey);
            }

            if (headers != null) {
                headers.forEach(con::setRequestProperty);
            }

            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);

            return handleResponse(con, endpoint);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public JsonObject executePostRequest(String endpoint, String apiKey, Map<String, String> headers,
                                         int connectTimeout, int readTimeout) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            if (apiKey != null) {
                con.setRequestProperty("X-MBX-APIKEY", apiKey);
            }

            if (headers != null) {
                headers.forEach(con::setRequestProperty);
            }

            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);

            return handleResponse(con, endpoint);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private JsonObject handleResponse(HttpURLConnection con, String requestDescription) throws Exception {
        int responseCode = con.getResponseCode();
        logger.debug("{} - Response Code: {}", requestDescription, responseCode);

        String rawResponse = readResponse(con);
        logger.debug("{} - Raw Response: {}", requestDescription,
                rawResponse.length() > 500 ? rawResponse.substring(0, 500) + "..." : rawResponse);

        try {
            return JsonParser.parseString(rawResponse).getAsJsonObject();
        } catch (Exception parseException) {
            logger.error("{} - Failed to parse JSON response. Code: {}. Raw: {}",
                    requestDescription, responseCode, rawResponse, parseException);

            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error_code", responseCode);
            errorResponse.addProperty("error_message", "Failed to parse API response: " + parseException.getMessage());
            errorResponse.addProperty("raw_response", rawResponse);
            return errorResponse;
        }
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        boolean isError = connection.getResponseCode() >= 400;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                isError ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (NullPointerException e) {
            if (isError) {
                return "{\"error_message\":\"Failed to read error stream, response code: " +
                        connection.getResponseCode() + "\", \"error_code\": "+ connection.getResponseCode() +"}";
            }
            throw e;
        }
    }
}