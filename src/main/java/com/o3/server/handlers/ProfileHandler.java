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

public class ProfileHandler implements HttpHandler {
    
    private DatabaseManager db;

    public ProfileHandler() {
        db = DatabaseManager.getInstance();
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();

        h.add("Access-Control-Allow-Origin", "http://localhost:8000");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        //addCorsHeaders(exchange);

        exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.flush();
        os.close();
    }

    private static String getUsernameFromAuth(HttpExchange exchange) {
        // Get the Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Extract the base64-encoded credentials
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes);

            // Split username and password
            String[] parts = credentials.split(":", 2);
            if (parts.length == 2) {
                return parts[0]; // Return the username
            }
        }
        return null; // Return null if no valid auth data found
    }

    @Override 
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        
        if ("OPTIONS".equals(method)) {
            //addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
        } else if ("GET".equals(method)) {
            String username = getUsernameFromAuth(exchange);
            User user = db.getUser(username);

            JSONObject obj = new JSONObject();
            obj.put("username", user.getLogin());
            obj.put("email", user.getEmail());
            obj.put("nickname", user.getNickname());

            String responseString = obj.toString();
            sendResponse(exchange, 200, responseString);
        } else {
            sendResponse(exchange, 400, "Not supported");
        }
    }
}
