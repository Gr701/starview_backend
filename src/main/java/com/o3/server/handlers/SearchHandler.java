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
        try {
            QueryParams params = getSearchParamsFromRequest(exchange);
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

        } catch (Exception e) {
            System.out.println("SearchHandler > handleGet > Something went wrong \n" + e.getMessage());
            sendResponse(exchange, 400, "handleGetException");
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            handleGet(exchange);
        } else {
            sendResponse(exchange, 400, "Not supported");
        }
    }
}
