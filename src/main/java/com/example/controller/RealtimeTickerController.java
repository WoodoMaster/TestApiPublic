package com.example.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RealtimeTickerController {
    private static final Logger logger = LoggerFactory.getLogger(RealtimeTickerController.class);
    private static final String BASE_API_URL = "https://demo-api-adapter.dzengi.com/api/v2";
    private static final String EXCHANGE_INFO_URL = BASE_API_URL + "/exchangeInfo";

    @GetMapping("/realtime-ticker")
    public String showRealtimeTicker(Model model) {
        logger.debug("Displaying real-time ticker form");
        loadSymbolsIntoModel(model);
        return "realtime-ticker";
    }

    private void loadSymbolsIntoModel(Model model) {
        List<String> symbols = getAvailableSymbols();
        if (symbols.isEmpty()) {
            model.addAttribute("symbolsError", "Не удалось загрузить список символов.");
        } else {
            model.addAttribute("symbols", symbols);
        }
    }

    // --- Метод получения списка символов (остается без изменений) ---
    private List<String> getAvailableSymbols() {
        try {
            logger.info("Fetching available symbols from exchange info...");
            URL url = new URL(EXCHANGE_INFO_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // Аутентификация для /exchangeInfo обычно не требуется для получения списка символов

            int responseCode = con.getResponseCode();
            logger.debug("Exchange Info Response Code: {}", responseCode);

            if (responseCode == 200) {
                String rawResponse = readResponse(con);
                // Уменьшим объем логгирования полного ответа, если он слишком большой
                if (logger.isTraceEnabled()) {
                    logger.trace("Raw Exchange Info Response: {}", rawResponse);
                } else if (rawResponse.length() > 1000) {
                    logger.debug("Raw Exchange Info Response (truncated): {}...", rawResponse.substring(0, 1000));
                } else {
                    logger.debug("Raw Exchange Info Response: {}", rawResponse);
                }

                JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();

                // Ищем массив 'symbols' непосредственно в корневом объекте
                if (jsonResponse.has("symbols") && jsonResponse.get("symbols").isJsonArray()) {
                    JsonArray symbolsArray = jsonResponse.getAsJsonArray("symbols");
                    List<String> symbolNames = new ArrayList<>();
                    for (JsonElement element : symbolsArray) {
                        if (element.isJsonObject()) {
                            JsonObject symbolObject = element.getAsJsonObject();
                            if (symbolObject.has("symbol")) {
                                symbolNames.add(symbolObject.get("symbol").getAsString());
                            }
                        } else {
                            logger.warn("Element in 'symbols' array is not a JSON object: {}", element);
                        }
                    }
                    Collections.sort(symbolNames); // Сортируем для удобства
                    logger.info("Successfully fetched {} symbols.", symbolNames.size());
                    return symbolNames;
                } else {
                    logger.warn("Exchange info response does not contain a top-level 'symbols' array or it's not an array. Response: {}", rawResponse);
                }
            } else {
                String errorResponse = "";
                if (con.getErrorStream() != null) {
                    errorResponse = readResponse(con);
                }
                logger.warn("Failed to fetch exchange info. Response code: {}. Error body: {}", responseCode, errorResponse);
            }
        } catch (Exception e) {
            logger.error("Exception fetching available symbols", e);
        }
        return Collections.emptyList();
    }

    // --- Вспомогательный метод для чтения ответа (остается без изменений) ---
    private String readResponse(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getResponseCode() >= 200 && connection.getResponseCode() < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        String response = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        reader.close();
        return response;
    }
}