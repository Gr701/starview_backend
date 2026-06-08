package com.o3.server.handlers;

import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;
import org.json.JSONArray;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;
import com.o3.server.models.ObservationRecord;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class CollectionHandler implements HttpHandler {
    
    private DatabaseManager db;

    public CollectionHandler() {
        db = DatabaseManager.getInstance();
    }

    private void returnAllCollections(HttpExchange exchange) {
        String username = getUsernameFromAuth(exchange);
        int userId = db.getUser(username).getId();
        
        JSONArray response = new JSONArray();

        ArrayList<DatabaseManager.Collection> collections = db.getUserCollections(userId);
        for (DatabaseManager.Collection collection : collections) {
            JSONObject collectionJson = new JSONObject();
            collectionJson.put("id", collection.id());
            collectionJson.put("name", collection.name());
            collectionJson.put("description", collection.description());

            JSONArray recordIds = new JSONArray();
            for (int id : collection.recordIds()) {
                recordIds.put(id);
            }
            collectionJson.put("recordIds", recordIds);
            response.put(collectionJson);
        }

        sendResponse(exchange, 200, response.toString());
    }

    private void handleGet(HttpExchange exchange) {

        Integer collectionId = getIdFromRequest(exchange);

        //return all users collections
        if (collectionId == -1) { 
            returnAllCollections(exchange);
            return;
        }  

        //return one collection fully
        DatabaseManager.Collection collection = db.getCollection(collectionId);

        JSONObject collectionJson = new JSONObject();
        collectionJson.put("id", collection.id());
        collectionJson.put("name", collection.name());
        collectionJson.put("description", collection.description());

        JSONArray recordsArray = new JSONArray();
        for (int id : collection.recordIds()) {
            recordsArray.put(db.getRecordById(id).getJson());
        }
        collectionJson.put("records", recordsArray);

        sendResponse(exchange, 200, collectionJson.toString());
    }

    private void handlePost(HttpExchange exchange) {
        JSONObject json = getJsonFromRequest(exchange);

        String username = getUsernameFromAuth(exchange);
        int userId = db.getUser(username).getId();
        String name = getStringFromJson(json, "name");
        String description = getStringFromJson(json, "description");

        db.addCollection(userId, name, description); 
        sendResponse(exchange, 200, "Collection added");
    }

    private void handlePut(HttpExchange exchange) {
        JSONObject json = getJsonFromRequest(exchange);

        String username = getUsernameFromAuth(exchange);
        int userId = db.getUser(username).getId();
        int collectionId = getIntegerFromJson(json, "collectionId");
        int recordId = getIntegerFromJson(json, "recordId");

        int ownerId = db.getCollection(collectionId).ownerId();       
        if (userId != ownerId) {
            throw new IllegalArgumentException("Another user owns the collection");
        }
        
        db.addRecordToCollection(recordId, collectionId); 
        sendResponse(exchange, 200, "Record added to collection");
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
                case "PUT":
                    handlePut(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            handleException(exchange, e);
        }   
    }
}
