package com.o3.server.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.o3.server.managers.AuthenticationManager;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class RegistrationHandler implements HttpHandler {

    private final AuthenticationManager userAuthenticator;

    public RegistrationHandler(AuthenticationManager ua) {
        userAuthenticator = ua;
    }

    private void handlePost(HttpExchange exchange) {
        checkContentType(exchange);
        JSONObject json = getJsonFromRequest(exchange);

        String username = getStringFromJson(json, "username");
        String password = getStringFromJson(json, "password");
        String email = getStringFromJson(json, "email");
        String userNickname = getStringFromJson(json, "userNickname");

        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("password and username cannot be empty");
        }

        if (userNickname.isEmpty()) {
            userNickname = username;
        }

        if (userAuthenticator.addUser(username, password, email, userNickname)) {
            sendResponse(exchange, 200, "User registred"); 
        } else {
            sendResponse(exchange, 405, "User already exists"); 
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        addCorsHeaders(exchange);
        try {
            switch (method) {
                case "OPTIONS":
                    exchange.sendResponseHeaders(204, -1);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            sendResponse(exchange, 500, e.getMessage());
        } catch (Exception e) {
            System.out.println("RegistrationHandler > handle > Exception > " + method + " > " + e);
            e.printStackTrace(System.out); 
            sendResponse(exchange, 500, "Error handling the request");
        }
    }
}
