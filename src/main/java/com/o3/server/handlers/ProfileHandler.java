package com.o3.server.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import org.json.JSONObject;
import org.json.JSONException;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;

import static com.o3.server.utils.HttpUtils.*;

public class ProfileHandler implements HttpHandler {
    
    private DatabaseManager db;

    public ProfileHandler() {
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) {
        String username = getUsernameFromAuth(exchange);
        User user = db.getUser(username);

        JSONObject obj = new JSONObject();
        obj.put("username", user.getLogin());
        obj.put("email", user.getEmail());
        obj.put("nickname", user.getNickname());

        String responseString = obj.toString();
        sendResponse(exchange, 200, responseString);
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
                    handleGet(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            sendResponse(exchange, 500, e.getMessage());
        } catch (Exception e) {
            System.out.println("ProfileHandler > handle > Exception > " + method + " > " + e);
            e.printStackTrace(System.out); 
            sendResponse(exchange, 500, "Error handling the request");
        }   
    }
}
