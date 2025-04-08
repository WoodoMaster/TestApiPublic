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
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final DzengiApiService apiService;

    public AccountController(DzengiApiService api) {
        this.apiService = api;
    }

    @GetMapping("/account")
    public String form() { return "account"; }

    @PostMapping("/account")
    public String submit(@RequestParam String apiKey, @RequestParam String secretKey, Model model){
        try {
            var resp = apiService.getAccountInfo(apiKey, secretKey);
            if(resp.has("error_code")) {
                model.addAttribute("error", resp.get("error_message").getAsString());
            } else {
                Map map = new Gson().fromJson(resp, Map.class);
                model.addAttribute("payload",map);
            }
        } catch(Exception e){
            log.error("Account error",e);
            model.addAttribute("error", e.getMessage());
        }
        return "account";
    }
}