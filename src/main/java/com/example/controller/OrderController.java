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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.List;


@Controller
@RequestMapping("/order") // Base path for order related actions
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final DzengiApiService dzengiApiService;
    private final SymbolService symbolService; // Inject SymbolService
    private static final Gson gson = new Gson();

    @Autowired
    public OrderController(DzengiApiService dzengiApiService, SymbolService symbolService) {
        this.dzengiApiService = dzengiApiService;
        this.symbolService = symbolService;
    }

    @PostMapping("/place")
    public String placeOrder(
            @RequestParam("symbol") String symbol,
            @RequestParam("side") String side, // "BUY" or "SELL"
            @RequestParam("type") String type, // "MARKET" or "LIMIT"
            @RequestParam("quantity") String quantity,
            @RequestParam(value = "price", required = false) String price, // Optional for MARKET
            @RequestParam("apiKey") String apiKey,
            @RequestParam("secretKey") String secretKey,
            Model model, // Use Model directly instead of RedirectAttributes for simplicity here
            RedirectAttributes redirectAttributes) { // Keep RedirectAttributes if you prefer redirection

        logger.info("Received place order request: Symbol={}, Side={}, Type={}, Qty={}, Price={}, Key={}",
                symbol, side, type, quantity, price, apiKey.substring(0, Math.min(apiKey.length(), 8)) + "...");

        String viewName = "realtime-ticker"; // The JSP page to return to

        // --- Basic Server-Side Validation ---
        if (apiKey == null || apiKey.trim().isEmpty() || secretKey == null || secretKey.trim().isEmpty()) {
            model.addAttribute("orderErrorMessage", "API ключ и Секретный ключ не могут быть пустыми.");
            loadSymbolsIntoModel(model); // Reload symbols for the page
            return viewName;
        }
        if (!"BUY".equalsIgnoreCase(side) && !"SELL".equalsIgnoreCase(side)) {
            model.addAttribute("orderErrorMessage", "Неверная сторона ордера (BUY или SELL).");
            loadSymbolsIntoModel(model);
            return viewName;
        }
        if (!"MARKET".equalsIgnoreCase(type) && !"LIMIT".equalsIgnoreCase(type)) {
            model.addAttribute("orderErrorMessage", "Неверный тип ордера (MARKET или LIMIT).");
            loadSymbolsIntoModel(model);
            return viewName;
        }
        try {
            if (Double.parseDouble(quantity) <= 0) {
                model.addAttribute("orderErrorMessage", "Количество должно быть положительным.");
                loadSymbolsIntoModel(model);
                return viewName;
            }
        } catch (NumberFormatException e) {
            model.addAttribute("orderErrorMessage", "Неверный формат количества.");
            loadSymbolsIntoModel(model);
            return viewName;
        }
        if ("LIMIT".equalsIgnoreCase(type)) {
            if (price == null || price.trim().isEmpty()) {
                model.addAttribute("orderErrorMessage", "Цена обязательна для LIMIT ордера.");
                loadSymbolsIntoModel(model);
                return viewName;
            }
            try {
                if (Double.parseDouble(price) <= 0) {
                    model.addAttribute("orderErrorMessage", "Цена должна быть положительной для LIMIT ордера.");
                    loadSymbolsIntoModel(model);
                    return viewName;
                }
            } catch (NumberFormatException e) {
                model.addAttribute("orderErrorMessage", "Неверный формат цены.");
                loadSymbolsIntoModel(model);
                return viewName;
            }
        } else {
            price = null; // Ensure price is null for MARKET orders
        }
        // --- End Validation ---


        try {
            // Default recvWindow, can be made configurable
            Long recvWindow = 5000L;
            JsonObject responseJson = dzengiApiService.placeOrder(apiKey, secretKey, symbol, side, type, quantity, price, recvWindow);

            // Check if the service returned an error structure
            if (responseJson.has("error_code") || responseJson.has("code")) {
                String errorMsg = responseJson.has("error_message") ? responseJson.get("error_message").getAsString() :
                        responseJson.has("msg") ? responseJson.get("msg").getAsString() : "Неизвестная ошибка API при размещении ордера";
                logger.warn("API Error placing order: {}", responseJson.toString());
                model.addAttribute("orderErrorMessage", "Ошибка API: " + errorMsg);
                model.addAttribute("orderErrorCode", responseJson.has("error_code") ? responseJson.get("error_code").getAsJsonPrimitive() : responseJson.get("code").getAsJsonPrimitive());

            } else {
                // Success
                // Convert to Map for simpler JSP access
                Map<String, Object> orderDetails = gson.fromJson(responseJson, Map.class);
                String orderId = String.valueOf(orderDetails.getOrDefault("orderId", "N/A")); // Extract orderId
                String status = String.valueOf(orderDetails.getOrDefault("status", "UNKNOWN"));
                logger.info("Order placed successfully: ID={}, Status={}", orderId, status);
                model.addAttribute("orderSuccessMessage",
                        String.format("Ордер успешно размещен! ID: %s, Статус: %s", orderId, status));
                model.addAttribute("lastOrderDetails", orderDetails); // Pass full details if needed
            }

        } catch (Exception e) {
            logger.error("Exception placing order", e);
            model.addAttribute("orderErrorMessage", "Ошибка сервера при размещении ордера: " + e.getMessage());
        }

        loadSymbolsIntoModel(model); // Reload symbols before rendering the page again
        // Set the selected symbol so the dropdown remembers it (important!)
        model.addAttribute("selectedSymbol", symbol);
        return viewName; // Return to the ticker page with feedback
    }

    // Helper to load symbols (similar to TickerController)
    private void loadSymbolsIntoModel(Model model) {
        try {
            List<String> symbols = symbolService.getAvailableSymbols();
            if (symbols.isEmpty()) {
                model.addAttribute("symbolsError", "Не удалось загрузить список символов.");
            } else {
                model.addAttribute("symbols", symbols);
            }
        } catch (Exception e) {
            logger.error("Failed to load symbols", e);
            model.addAttribute("symbolsError", "Ошибка загрузки списка символов.");
        }
    }
}