package com.o3.server.handlers;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;
import com.o3.server.models.ObservationRecord;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class DatarecordHandler implements HttpHandler {
    private DatabaseManager db;

    public DatarecordHandler() {
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) {
        ArrayList<ObservationRecord> messages = db.getRecords(); 
        JSONArray responseMessages = new JSONArray();

        for (ObservationRecord r : messages) {
            responseMessages.put(r.getJson());
        }
        sendResponse(exchange, 200, responseMessages.toString());
    }

    private void handlePost(HttpExchange exchange) {
        checkContentType(exchange);
        ObservationRecord record = new ObservationRecord();

        String ownerUsername = getUsernameFromAuth(exchange);
        String owner = db.getUser(ownerUsername).getNickname();
        JSONObject json = getJsonFromRequest(exchange);

        record.setOwnerUsername(ownerUsername);
        record.setOwner(owner);
        record.setJson(json);
        record.setTimeReceived(getCurrentFormattedTime());
       
        db.addRecord(record);
        sendResponse(exchange, 200, "Record added");
    }

    private void handlePut(HttpExchange exchange) {
        checkContentType(exchange);
        Integer id = getIdFromRequest(exchange);

        //GET THE CURRENT RECORD
        ObservationRecord record = db.getRecordById(id);

        //GET REQUEST JSON
        JSONObject json = getJsonFromRequest(exchange);
 
        switch (getStringFromJson(json, "editType")) {
            case "addView":
                record.addView();
                break;
            case "updateRating":
                record.updateRating(getIntegerFromJson(json, "rating"));
                break;
            case "editRecord":
                String username = getUsernameFromAuth(exchange);
                if (!record.getOwnerUsername().equals(username)) {
                    throw new IllegalArgumentException("Another owner owns the record");
                }
                
                record.setModified(getCurrentFormattedTime());
                record.setUpdateReason(getStringFromJson(json, "updateReason"));
                record.setJson(json);
                break;
            default: 
                throw new IllegalArgumentException("Unsupported editType");
        }

        db.updateRecord(record);
        sendResponse(exchange, 200, "Record updated");
    }

    private void handleDelete(HttpExchange exchange) {
        Integer id = getIdFromRequest(exchange);
        ObservationRecord record = db.getRecordById(id);

        String username = getUsernameFromAuth(exchange);
        if (!record.getOwnerUsername().equals(username)) {
            throw new IllegalArgumentException("Another owner owns the record");
        }

        db.deleteRecord(id);
        sendResponse(exchange, 200, "Record deleted");
    }

    @Override
    public void handle(HttpExchange exchange) {
        //System.out.println("DatarecordHandler > handle > Request handled in 
        //thread " + Thread.currentThread().threadId());
        System.out.println("DatarecordHandler > handle > we got a request");

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
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            handleException(exchange, e);
        }
    }
}
