package com.o3.server.handlers;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.o3.server.managers.AuthenticationManager;
import com.o3.server.managers.DatabaseManager;
import com.o3.server.managers.WeatherManager;
import com.o3.server.models.User;
import com.o3.server.models.ObservationRecord;

import static com.o3.server.handlers.HandlerUtils.*;

public class DatarecordHandler implements HttpHandler {
    private final AuthenticationManager userAuthenticator;
    private DatabaseManager db;

    public DatarecordHandler(AuthenticationManager ua) {
        userAuthenticator = ua;
        db = DatabaseManager.getInstance();
    }

    private ObservationRecord addWeatherToRecord(ObservationRecord record) {
        JSONObject weatherJson = WeatherManager.getWeather(
            record.getLatitude(), 
            record.getLongitude(), 
            record.getTimeReceived()
        );
        
        if (weatherJson == null) {
            throw new RuntimeException("Error adding weather to the observation");
        }

        try {
            record.setIsWeatherPresent(true);
            record.setTemperatureInKelvins(weatherJson.getDouble("temperatureInKelvins"));
            record.setCloudinessPercentance(weatherJson.getDouble("cloudinessPercentance"));
            record.setBagroundLightVolume(weatherJson.getDouble("bagroundLightVolume"));
        } catch (JSONException e) {
            throw new RuntimeException("Error adding weather to the observation");
        }

        return record;
    }

    private ObservationRecord addObservatoryToRecord(ObservationRecord record, JSONObject json) {
        JSONObject observatoryJson;
        try { 
            observatoryJson = json.getJSONObject("observatory");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json observatory field must be a JSONObject");
        }

        record.setIsObservatoryPresent(true);
        record.setObservatoryName(getStringFromJson(observatoryJson, "observatoryName"));
        record.setLatitude(getDoubleFromJson(observatoryJson, "latitude"));
        record.setLongitude(getDoubleFromJson(observatoryJson, "longitude"));

        if (json.has("observatoryWeather")) {
            record = addWeatherToRecord(record);
        }

        return record;
    }

    private ObservationRecord fillRecordFromJson(ObservationRecord record, JSONObject json) {
        //BASICS
        record.setIdentifier(getStringFromJson(json, "recordIdentifier"));
        record.setDescription(getStringFromJson(json, "recordDescription"));
        record.setPayload(getStringFromJson(json, "recordPayload"));
        record.setRightAscension(getStringFromJson(json, "recordRightAscension"));
        record.setDeclination(getStringFromJson(json, "recordDeclination"));

        //OBSERVATORY AND WEATHER
        if (json.has("observatory")) {
            record = addObservatoryToRecord(record, json);
        }

        return record;
    }

    private ObservationRecord updateRecordContent(
        HttpExchange exchange, 
        ObservationRecord record, 
        JSONObject json
    ) {
        //check ownership
        String username = getUsernameFromAuth(exchange);
        if (!record.getOwnerUsername().equals(username)) {
            throw new IllegalArgumentException("Another owner owns the record");
        }
        
        //UPDATE INFO
        record.setModified(getCurrentFormattedTime());
        record.setUpdateReason(getStringFromJson(json, "updateReason"));
        record = fillRecordFromJson(record, json);

        return record;
    }

    private void handleGet(HttpExchange exchange) {
        ArrayList<ObservationRecord> messages = db.getRecords(); 
        JSONArray responseMessages = new JSONArray();

        for (ObservationRecord r : messages) {
            JSONObject obj = new JSONObject();

            obj.put("id", r.getId());
            obj.put("recordIdentifier", r.getIdentifier());
            obj.put("recordDescription", r.getDescription());
            obj.put("recordPayload", r.getPayload());
            obj.put("recordRightAscension", r.getRightAscension());
            obj.put("recordDeclination", r.getDeclination());
            obj.put("recordTimeReceived", r.getTimeReceived());
            obj.put("recordOwner", r.getOwner());
            obj.put("recordViewCount", r.getViewCount());
            obj.put("recordRating", r.getRating());
            obj.put("recordRatingCount", r.getRatingCount());

            if (r.getIsObservatoryPresent()) {
                JSONObject observatory = new JSONObject();
                observatory.put("observatoryName", r.getObservatoryName());
                observatory.put("latitude", r.getLatitude());
                observatory.put("longitude", r.getLongitude());
                obj.put("observatory", observatory);
            }

            if (r.getIsWeatherPresent()) {
                JSONObject observatoryWeather = new JSONObject();
                observatoryWeather.put("temperatureInKelvins", r.getTemperatureInKelvins());
                observatoryWeather.put("cloudinessPercentance", r.getCloudinessPercentance());
                observatoryWeather.put("bagroundLightVolume", r.getBagroundLightVolume());
                obj.put("observatoryWeather", observatoryWeather);
            }

            if (r.getModified() != null) {
                obj.put("updateReason", r.getUpdateReason());
                obj.put ("modified", r.getModified());
            }

            responseMessages.put(obj);
        }
        sendResponse(exchange, 200, responseMessages.toString());
    }

    private void handlePost(HttpExchange exchange) {
        checkContentType(exchange);

        JSONObject json = getJsonFromRequest(exchange);
        ObservationRecord record = fillRecordFromJson(new ObservationRecord(), json);

        String ownerUsername = getUsernameFromAuth(exchange);
        record.setOwnerUsername(ownerUsername);
        String owner = userAuthenticator.getNickname(ownerUsername);
        record.setOwner(owner);

        record.setTimeReceived(getCurrentFormattedTime());
       
        db.addRecord(record);
        sendResponse(exchange, 200, "Record added");
    }

    private void handlePut(HttpExchange exchange) {
        checkContentType(exchange);
        Integer id = getIdFromRequest(exchange);

        //GET THE CURRENT RECORD
        ObservationRecord record = db.getRecordById(id);
        if (record == null) {
            throw new RuntimeException("Id not found in the database");
        }

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
                record = updateRecordContent(exchange, record, json);
                break;
            default: 
                throw new IllegalArgumentException("Unsupported editType");
        }

        db.updateRecord(record);
        sendResponse(exchange, 200, "Record updated");
    }

    @Override
    public void handle(HttpExchange exchange) {
        //System.out.println("DatarecordHandler > handle > Request handled in 
        //thread " + Thread.currentThread().threadId());
        System.out.println("DatarecordHandler > handle > we got a request");

        String method = exchange.getRequestMethod().toUpperCase();
        try {
            switch (method) {
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
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            sendResponse(exchange, 500, e.getMessage());
        } catch (Exception e) {
            System.out.println("DatarecordHandler > handle > Exception > " + method + " > " + e);
            e.printStackTrace(System.out); 
            sendResponse(exchange, 500, "Error handling the request");
        }
    }
}
