package com.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Controller
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String API_URL = "https://demo-api-adapter.dzengi.com/api/v1/account";

    @GetMapping("/account")
    public String showAccountForm() {
        return "account";
    }

    @PostMapping("/account")
    public String getAccountInfo(@RequestParam("apiKey") String apiKey,
                                 @RequestParam("secretKey") String secretKey,
                                 Model model) {
        try {
            logger.debug("Starting account info request");
            logger.debug("API Key: {}", apiKey);

            long timestamp = System.currentTimeMillis();
            String query = "timestamp=" + timestamp;
            logger.debug("Query: {}", query);

            String signature = generateSignature(query, secretKey);
            logger.debug("Signature: {}", signature);

            URL url = new URL(API_URL + "?" + query + "&signature=" + signature);
            logger.debug("Full URL: {}", url);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-MBX-APIKEY", apiKey);

            int responseCode = con.getResponseCode();
            logger.debug("Response Code: {}", responseCode);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode == 200 ? con.getInputStream() : con.getErrorStream()));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            logger.debug("Raw Response: {}", response);

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            logger.debug("Parsed JSON: {}", json);

            if (json.has("code") && json.has("msg")) {
                // Это ошибка от API
                model.addAttribute("status", "Ошибка");
                model.addAttribute("payload", Map.of(
                        "code", json.get("code").getAsDouble(),
                        "msg", json.get("msg").getAsString()
                ));
            } else {
                // Успешный ответ
                model.addAttribute("status", "OK");
                model.addAttribute("payload", new Gson().fromJson(json, Map.class));
            }

        } catch (Exception e) {
            logger.error("Exception during account info request", e);
            model.addAttribute("error", "Ошибка при запросе: " + e.getMessage());
        }

        return "account";
    }

    private String generateSignature(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        hmac.init(secretKey);
        byte[] signatureBytes = hmac.doFinal(data.getBytes());
        return bytesToHex(signatureBytes).toLowerCase();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}