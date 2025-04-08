package com.example.service.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HttpApiClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiClient.class);

    private final String baseUrl;
    private final int connectTimeout;
    private final int readTimeout;

    public HttpApiClient(
            @Value("${dzengi.api.base-url}") String baseUrl,
            @Value("${dzengi.api.connect-timeout}") int connectTimeout,
            @Value("${dzengi.api.read-timeout}") int readTimeout
    ) {
        this.baseUrl = baseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public JsonObject get(String endpoint, String apiKey, Map<String,String> headers) throws Exception {
        return execute("GET", endpoint, apiKey, headers);
    }

    public JsonObject post(String endpoint, String apiKey, Map<String,String> headers) throws Exception {
        return execute("POST", endpoint, apiKey, headers);
    }

    private JsonObject execute(String method, String endpoint, String apiKey, Map<String,String> headers) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);

            if(apiKey != null)
                con.setRequestProperty("X-MBX-APIKEY", apiKey);
            if(headers != null)
                headers.forEach(con::setRequestProperty);

            int code = con.getResponseCode();
            String resp = readResponse(con);
            logger.debug("{} {} -> {}: {}", method, endpoint, code, resp.length()>300?resp.substring(0,300)+"...":resp);
            var json = JsonParser.parseString(resp).getAsJsonObject();

            // вложим код в json для дальнейшей обработки
            json.addProperty("_httpCode", code);
            return json;
        } catch(Exception e){
            logger.error("HTTP {} error: {}", method, e.getMessage());
            throw e;
        } finally {
            if(con != null) con.disconnect();
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        boolean error = conn.getResponseCode() >= 400;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                error ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch(NullPointerException ex){
            return "{}";
        }
    }
}