package com.o3.server.handlers;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.ObservationRecord;

import static com.o3.server.utils.HttpUtils.*;
import static com.o3.server.utils.JsonUtils.*;

public class SearchHandler implements HttpHandler {
       
    private DatabaseManager db;

    public SearchHandler() {
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) {
        QueryParams params = getSearchParamsFromRequest(exchange);
        System.out.println(params);
        ArrayList<ObservationRecord> messages = db.serchRecords(
            params.identification(), 
            params.nickname(), 
            params.before(), 
            params.after()
        );

        JSONArray responseMessages = new JSONArray();
        for (ObservationRecord r : messages) {
            responseMessages.put(r.getJson());
        }

        sendResponse(exchange, 200, responseMessages.toString());
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
        } catch (Exception e) {
            handleException(exchange, e);
        }   
    }
}
