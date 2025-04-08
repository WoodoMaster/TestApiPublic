package com.example.controller;
import com.example.service.DzengiApiService;
import com.example.service.SymbolService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class TickerController {

    private static final Logger logger = LoggerFactory.getLogger(TickerController.class);
    private final DzengiApiService dzengiApiService;
    private final SymbolService symbolService;
    private static final Gson gson = new Gson();

    @Autowired
    public TickerController(DzengiApiService dzengiApiService, SymbolService symbolService) {
        this.dzengiApiService = dzengiApiService;
        this.symbolService = symbolService;
    }

    // --- Метод для отображения формы (GET) ---
    @GetMapping("/ticker")
    public String showTickerForm(Model model) {
        logger.debug("Displaying ticker form.");
        loadSymbolsIntoModel(model);
        model.addAttribute("selectedSymbol", ""); // Initialize selectedSymbol
        return "ticker"; // Renders ticker.jsp
    }

    // --- Метод для обработки выбора и получения данных (POST) ---
    @PostMapping("/ticker")
    public String getTickerData(@RequestParam(value = "symbol") String symbol, Model model) {
        logger.info("Received ticker request via POST for symbol: {}", symbol);
        loadSymbolsIntoModel(model); // Always load symbols for the dropdown
        model.addAttribute("selectedSymbol", symbol); // Keep the selected symbol

        if (symbol == null || symbol.trim().isEmpty()) {
            model.addAttribute("tickerError", "Символ не был выбран.");
            return "ticker";
        }

        try {
            JsonObject responseJson = dzengiApiService.getTickerData(symbol);

            // Check for error structure
            if (responseJson.has("error_code") || responseJson.has("code")) {
                String errorMsg = responseJson.has("error_message") ? responseJson.get("error_message").getAsString() :
                        responseJson.has("msg") ? responseJson.get("msg").getAsString() : "Неизвестная ошибка API";
                logger.warn("API Error fetching ticker data for {}: {}", symbol, responseJson.toString());
                model.addAttribute("tickerError", "Ошибка API: " + errorMsg);
                model.addAttribute("tickerErrorCode", responseJson.has("error_code") ? responseJson.get("error_code").getAsJsonPrimitive() : responseJson.get("code").getAsJsonPrimitive());

            } else {
                // Success - convert to Map
                Map<String, Object> tickerData = gson.fromJson(responseJson, Map.class);
                model.addAttribute("tickerData", tickerData);
                logger.info("Successfully processed ticker data request for {}", symbol);
            }

        } catch (IllegalArgumentException iae) {
            logger.warn("Invalid argument in ticker request: {}", iae.getMessage());
            model.addAttribute("tickerError", "Неверный аргумент: " + iae.getMessage());
        } catch (Exception e) {
            logger.error("Exception processing ticker data request for symbol: " + symbol, e);
            model.addAttribute("tickerError", "Ошибка при обработке запроса: " + e.getMessage());
        }

        return "ticker"; // Renders ticker.jsp with results or errors
    }

    // --- Helper to load symbols ---
    private void loadSymbolsIntoModel(Model model) {
        try {
            List<String> symbols = symbolService.getAvailableSymbols();
            if (symbols.isEmpty()) {
                model.addAttribute("symbolsError", "Не удалось загрузить список символов.");
                logger.warn("Symbol list is empty.");
            } else {
                model.addAttribute("symbols", symbols);
            }
        } catch (Exception e) {
            logger.error("Failed to load symbols for dropdown", e);
            model.addAttribute("symbolsError", "Ошибка загрузки списка символов.");
        }
    }
}