package com.o3.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;

import static com.o3.server.utils.HttpUtils.*;

public class ProfileHandler implements HttpHandler {
    
    private DatabaseManager db;

    public ProfileHandler() {
        db = DatabaseManager.getInstance();
    }

    @Override 
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod().toUpperCase();
        
        try {
            switch (method) {
                case "OPTIONS":
                    exchange.sendResponseHeaders(204, -1);
                    break;
                case "GET":
                    String username = getUsernameFromAuth(exchange);
                    User user = db.getUser(username);
                    sendResponse(exchange, 200, user.getJson().toString());
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            handleException(exchange, e);
        }   
    }
}
