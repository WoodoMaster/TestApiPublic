package com.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // Импортируем PostMapping
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class TickerController {

    private static final Logger logger = LoggerFactory.getLogger(TickerController.class);

    private static final String BASE_API_URL = "https://demo-api-adapter.dzengi.com/api/v2";
    private static final String EXCHANGE_INFO_URL = BASE_API_URL + "/exchangeInfo";
    private static final String TICKER_URL = BASE_API_URL + "/ticker/24hr";
    private static final Gson gson = new Gson();

    // --- Метод для отображения формы (GET) ---
    @GetMapping("/ticker")
    public String showTickerForm(Model model) {
        logger.debug("Displaying ticker form.");
        // Загружаем символы для выпадающего списка
        loadSymbolsIntoModel(model);
        // При GET запросе просто показываем форму, selectedSymbol не нужен или null
        model.addAttribute("selectedSymbol", ""); // Или можно не добавлять вовсе
        return "ticker"; // Имя JSP файла (ticker.jsp)
    }

    // --- Метод для обработки выбора и получения данных (POST) ---
    @PostMapping("/ticker")
    public String getTickerData(
            @RequestParam(value = "symbol") String symbol,
            Model model) {

        logger.info("Received ticker request via POST for symbol: {}", symbol);
        loadSymbolsIntoModel(model);
        model.addAttribute("selectedSymbol", symbol);

        if (symbol == null || symbol.isEmpty()) {
            model.addAttribute("tickerError", "Символ не был выбран.");
            return "ticker";
        }

        try {
            logger.info("Fetching ticker data for symbol: {}", symbol);
            String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8.toString());
            URL url = new URL(TICKER_URL + "?symbol=" + encodedSymbol);
            logger.debug("Ticker URL: {}", url);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            logger.debug("Ticker Response Code: {}", responseCode);

            String rawResponse = readResponse(con);
            logger.debug("Raw Ticker Response: {}", rawResponse);

            JsonObject jsonResponse = JsonParser.parseString(rawResponse).getAsJsonObject();

            if (responseCode == 200) {
                // Убираем проверку на payload, так как API возвращает данные напрямую
                Map<String, Object> tickerData = gson.fromJson(jsonResponse, Map.class);
                model.addAttribute("tickerData", tickerData);
                logger.info("Successfully fetched ticker data for {}", symbol);
            } else {
                String errorMessage = "Неизвестная ошибка";
                if (jsonResponse.has("code") && jsonResponse.has("msg")) {
                    errorMessage = "Код: " + jsonResponse.get("code").getAsString() +
                            ", Сообщение: " + jsonResponse.get("msg").getAsString();
                } else if (jsonResponse.has("status") && !jsonResponse.get("status").getAsString().equalsIgnoreCase("OK")) {
                    errorMessage = jsonResponse.toString();
                } else {
                    errorMessage = "Код ответа сервера: " + responseCode;
                }
                logger.warn("Failed to fetch ticker data for {}. API Response: {}", symbol, rawResponse);
                model.addAttribute("tickerError", "Ошибка при получении данных тикера: " + errorMessage);
            }

        } catch (Exception e) {
            logger.error("Exception fetching ticker data for symbol: " + symbol, e);
            model.addAttribute("tickerError", "Ошибка при запросе данных тикера: " + e.getMessage());
        }

        return "ticker";
    }

    // --- Вспомогательный метод загрузки символов ---
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