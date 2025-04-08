package com.example.controller;
import com.example.service.DzengiApiService;
import com.example.service.SymbolService;
import com.google.gson.Gson;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class TickerController {

    private final DzengiApiService dzengiApiService;
    private final SymbolService symbolService;

    public TickerController(DzengiApiService a, SymbolService s) { this.dzengiApiService=a; this.symbolService=s; }

    @GetMapping("/ticker")
    public String tickerPage(Model model){
        model.addAttribute("symbols", symbolService.getAvailableSymbols());
        return "ticker";
    }

    @PostMapping("/ticker")
    public String getTicker(@RequestParam String symbol, Model model){
        model.addAttribute("symbols", symbolService.getAvailableSymbols());
        try{
            var res = dzengiApiService.getTickerData(symbol);
            if(res.has("error_code"))
                model.addAttribute("tickerError", res.get("error_message").getAsString());
            else
                model.addAttribute("tickerData", new Gson().fromJson(res, Map.class));
        } catch(Exception e){
            model.addAttribute("tickerError", e.getMessage());
        }
        model.addAttribute("selectedSymbol", symbol);
        return "ticker";
    }
}