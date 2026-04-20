package com.o3.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SearchHandler implements HttpHandler {
    
    
    private DatabaseManager db;

    public SearchHandler() {
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) {
        try {
            //getting the search parameters
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();
            
            if (query == null) {
                sendResponse(exchange, 400, "query is null");
                return;
            }

            String before = null;
            String after = null;
            String nickname = null;
            String identification = null;

            String[] paramPairs = query.split("&");
            for (String paramPair : paramPairs) {
                String key = paramPair.split("=")[0];
                String value = paramPair.split("=")[1];
                switch (key) {
                    case "before":
                        before = value;
                        break;
                    case "after":
                        after = value;
                        break;
                    case "nickname":
                        nickname = value;
                        break;
                    case "identification":
                        identification = value;
                        break;
                    default:
                        break;
                }
                
            }
            //System.out.printf("identification %s nickname %s before %s after %s \n", identification, nickname, before, after);
            
            //getting all records which fit to search from database
            ArrayList<ObservationRecord> messages = db.serchRecords(identification, nickname, before, after);

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

                if (r.getIsObservatoryPresent()) {
                    JSONObject observatory = new JSONObject();
                    observatory.put("observatoryName", r.getObservatoryName());
                    observatory.put("latitude", r.getLatitude());
                    observatory.put("longitude", r.getLongitude());

                    JSONArray observatoryArray = new JSONArray();
                    observatoryArray.put(observatory);
                    obj.put("observatory", observatoryArray);
                }

                if (r.getIsWeatherPresent()) {
                    JSONObject observatoryWeather = new JSONObject();
                    observatoryWeather.put("temperatureInKelvins", r.getTemperatureInKelvins());
                    observatoryWeather.put("cloudinessPercentance", r.getCloudinessPercentance());
                    observatoryWeather.put("bagroundLightVolume", r.getBagroundLightVolume());

                    JSONArray observatoryWeatherArray = new JSONArray();
                    observatoryWeatherArray.put(observatoryWeather);
                    obj.put("observatoryWeather", observatoryWeatherArray);
                }

                if (r.getModified() != null) {
                    obj.put("updateReason", r.getUpdateReason());
                    obj.put ("modified", r.getModified());
                }

                responseMessages.put(obj);
            }

            String responseString = responseMessages.toString();
            exchange.sendResponseHeaders(200, responseString.getBytes("UTF-8").length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(responseString.getBytes());
            stream.flush();
            stream.close();

        } catch (Exception e) {
            System.out.println("SearchHandler > handleGet > Something went wrong \n" + e.getMessage());
            sendResponse(exchange, 400, "handleGetException");
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String message) {
        try {
            exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(message.getBytes());
            stream.flush();
            stream.close();
        } catch (IOException e) {
            System.out.println("SearchHandler > sendResponse > Something went wrong \n" + e.getMessage());
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
