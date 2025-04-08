package com.example.controller;

import com.example.dto.OrderRequestDto;
import com.example.service.DzengiApiService;
import com.example.service.SymbolService;
import org.slf4j.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final DzengiApiService api;
    private final SymbolService symbols;

    public OrderController(DzengiApiService api, SymbolService symbols) {
        this.api = api;
        this.symbols = symbols;
    }

    @PostMapping("/place")
    public String place(@ModelAttribute OrderRequestDto dto, Model model) {
        model.addAttribute("symbols", symbols.getAvailableSymbols());
        try {
            var res = api.placeOrder(
                    dto.getApiKey(), dto.getSecretKey(),
                    dto.getSymbol(), dto.getSide(), dto.getType(),
                    dto.getQuantity(), dto.getPrice(), 5000L
            );
            if (res.has("error_code")) {
                model.addAttribute("orderErrorMessage", res.get("error_message").getAsString());
            } else {
                model.addAttribute("orderSuccessMessage", "Ордер успешно размещён, ID: " + res.get("orderId").getAsString());
            }
        } catch (Exception e) {
            logger.error("Order placement error", e);
            model.addAttribute("orderErrorMessage", e.getMessage());
        }
        return "realtime-ticker";
    }
}