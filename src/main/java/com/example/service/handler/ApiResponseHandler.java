package com.example.service.handler;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiResponseHandler.class);

    public JsonObject handleResponse(JsonObject jsonResponse, int responseCode, String requestDescription) {
        if (responseCode >= 200 && responseCode < 300) {
            boolean isDzengiOk = jsonResponse.has("status") && "OK".equalsIgnoreCase(jsonResponse.get("status").getAsString());
            boolean isStandardOk = jsonResponse.has("orderId") && jsonResponse.has("symbol");

            if (isDzengiOk && jsonResponse.has("payload")) {
                if (jsonResponse.get("payload").isJsonObject()) {
                    logger.info("{} - Success (Dzengi format).", requestDescription);
                    return jsonResponse.getAsJsonObject("payload");
                } else {
                    logger.warn("{} - Success (Dzengi format), but payload is not an object: {}",
                            requestDescription, jsonResponse.get("payload").toString());
                    return jsonResponse;
                }
            } else if (isStandardOk) {
                logger.info("{} - Success (Standard format).", requestDescription);
                return jsonResponse;
            } else if (jsonResponse.has("code") && jsonResponse.has("msg")) {
                logger.warn("{} - API returned error with HTTP {}: {}",
                        requestDescription, responseCode, jsonResponse);
                standardizeErrorResponse(jsonResponse);
                return jsonResponse;
            } else {
                logger.warn("{} - Received HTTP {} but response format is unexpected: {}",
                        requestDescription, responseCode, jsonResponse);
                return jsonResponse;
            }
        } else {
            logger.warn("{} - Failed. Response Code: {}. Body: {}",
                    requestDescription, responseCode, jsonResponse);
            standardizeErrorResponse(jsonResponse);
            return jsonResponse;
        }
    }

    private void standardizeErrorResponse(JsonObject jsonResponse) {
        if (!jsonResponse.has("error_message") && jsonResponse.has("msg")) {
            jsonResponse.addProperty("error_message", jsonResponse.get("msg").getAsString());
        } else if (!jsonResponse.has("error_message")) {
            jsonResponse.addProperty("error_message", "API request failed");
        }

        if (!jsonResponse.has("error_code") && jsonResponse.has("code")) {
            jsonResponse.addProperty("error_code", String.valueOf(jsonResponse.get("code").getAsJsonPrimitive()));
        } else if (!jsonResponse.has("error_code")) {
            jsonResponse.addProperty("error_code", -1);
        }
    }
}