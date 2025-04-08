package com.example.controller;

import com.example.service.DzengiApiService;
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

import java.util.Map;

@Controller
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final DzengiApiService dzengiApiService;
    private static final Gson gson = new Gson(); // Reusable Gson instance

    @Autowired
    public AccountController(DzengiApiService dzengiApiService) {
        this.dzengiApiService = dzengiApiService;
    }

    @GetMapping("/account")
    public String showAccountForm() {
        logger.debug("Displaying account form.");
        return "account"; // Renders account.jsp
    }

    @PostMapping("/account")
    public String getAccountInfo(@RequestParam("apiKey") String apiKey,
                                 @RequestParam("secretKey") String secretKey,
                                 Model model) {
        logger.info("Received account info request via POST.");
        if (apiKey == null || apiKey.trim().isEmpty() || secretKey == null || secretKey.trim().isEmpty()) {
            model.addAttribute("error", "API ключ и Секретный ключ не могут быть пустыми.");
            return "account";
        }

        try {
            JsonObject responseJson = dzengiApiService.getAccountInfo(apiKey, secretKey);

            // Check for error structure returned by the service
            if (responseJson.has("error_code") || responseJson.has("code")) {
                String errorMsg = responseJson.has("error_message") ? responseJson.get("error_message").getAsString() :
                        responseJson.has("msg") ? responseJson.get("msg").getAsString() : "Неизвестная ошибка API";
                logger.warn("API Error fetching account info: {}", responseJson.toString());
                model.addAttribute("status", "Ошибка API");
                model.addAttribute("payload", Map.of(
                        "code", responseJson.has("error_code") ? responseJson.get("error_code").getAsJsonPrimitive() : responseJson.get("code").getAsJsonPrimitive(),
                        "msg", errorMsg
                ));

            } else {
                // Success
                model.addAttribute("status", "OK");
                // Convert JsonObject payload to Map for easier JSP access
                Map<String, Object> payloadMap = gson.fromJson(responseJson, Map.class);
                model.addAttribute("payload", payloadMap);
                logger.info("Successfully processed account info request.");
            }

        } catch (Exception e) {
            logger.error("Exception processing account info request", e);
            model.addAttribute("error", "Ошибка при обработке запроса: " + e.getMessage());
        }

        return "account"; // Renders account.jsp with results or errors
    }
}