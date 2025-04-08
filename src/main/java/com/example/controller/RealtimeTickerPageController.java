package com.example.controller;

import com.example.service.SymbolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RealtimeTickerPageController {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeTickerPageController.class);
    private final SymbolService symbolService;

    @Autowired
    public RealtimeTickerPageController(SymbolService symbolService) {
        this.symbolService = symbolService;
    }

    @GetMapping("/realtime-ticker")
    public String showRealtimeTickerPage(Model model) {
        logger.debug("Displaying real-time ticker page.");
        try {
            List<String> symbols = symbolService.getAvailableSymbols();
            if (symbols.isEmpty()) {
                model.addAttribute("symbolsError", "Не удалось загрузить список символов.");
                logger.warn("Symbol list is empty for real-time ticker page.");
            } else {
                model.addAttribute("symbols", symbols);
            }
        } catch (Exception e) {
            logger.error("Failed to load symbols for real-time ticker page", e);
            model.addAttribute("symbolsError", "Ошибка загрузки списка символов.");
        }
        return "realtime-ticker"; // Renders realtime-ticker.jsp
    }
}
