package com.o3.server.handlers;

import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;
import org.json.JSONArray;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class CommentHandler implements HttpHandler {
    
    private DatabaseManager db;

    public CommentHandler() {
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) {
        int recordId = getIdFromRequest(exchange);
        ArrayList<DatabaseManager.Comment> comments = db.getCommentsForRecord(recordId);

        JSONArray response = new JSONArray();
        for (DatabaseManager.Comment c : comments) {
            String nickname = db.getUserById(c.userId()).getNickname();
            JSONObject json = new JSONObject();
            json.put("nickname", nickname);
            json.put("text", c.text());
            response.put(json);
        } 
        sendResponse(exchange, 200, response.toString());
    }

    private void handlePost(HttpExchange exchange) {
        JSONObject json = getJsonFromRequest(exchange);

        String username = getUsernameFromAuth(exchange);
        int userId = db.getUser(username).getId();
        int recordId = getIntegerFromJson(json, "recordId");
        String text = getStringFromJson(json, "text");
        db.addComment(userId, recordId, text); 

        sendResponse(exchange, 200, "Comment added");
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
