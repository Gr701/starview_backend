package com.o3.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class RegistrationHandler implements HttpHandler {

    private DatabaseManager db;

    public RegistrationHandler() {
        db = DatabaseManager.getInstance();
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

        db.addUser(new User(null, username, password, email, userNickname));
        sendResponse(exchange, 200, "User registred"); 
    }

    @Override
    public void handle(HttpExchange exchange) {
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
        } catch (Exception e) {
            handleException(exchange, e);
        }
    }
}
