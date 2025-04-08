package com.example.service.handler;

import com.google.gson.JsonObject;
import org.slf4j.*;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiResponseHandler.class);

    public JsonObject handleResponse(JsonObject json, String context){
        int code = json.has("_httpCode") ? json.get("_httpCode").getAsInt() : 200;
        logger.debug("{}: Response HTTP {}", context, code);

        if(code>=200 && code<300){
            if(json.has("code") && json.get("code").getAsInt()!=0){
                transformDzengiError(json);
            }
        } else {
            transformDzengiError(json);
        }
        return json;
    }

    private void transformDzengiError(JsonObject json){
        if(!json.has("error_message") && json.has("msg"))
            json.addProperty("error_message", json.get("msg").getAsString());
        else if(!json.has("error_message"))
            json.addProperty("error_message", "API Error");

        if(!json.has("error_code") && json.has("code"))
            json.addProperty("error_code", json.get("code").getAsInt());
        else if(!json.has("error_code"))
            json.addProperty("error_code", -1);
    }
}