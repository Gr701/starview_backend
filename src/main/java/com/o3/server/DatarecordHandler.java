package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class DatarecordHandler implements HttpHandler {

    //private String messages = "No messages";
    //private ArrayList<ObservationRecord> messages;
    private final UserAuthenticator userAuthenticator;
    private DatabaseManager db;

    public DatarecordHandler(UserAuthenticator ua) {
        userAuthenticator = ua;
        db = DatabaseManager.getInstance();
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        //if (messages.isEmpty()) {
        if (db.isRecordTableEmpty()) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

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


    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            Headers headers = exchange.getRequestHeaders();
            if (headers.containsKey("Content-Type")) {
                if (headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
                    //System.out.println("DatarecordHandler > handlePost > We got record post request");

                    InputStream stream = exchange.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    String newRecordText = reader.lines().collect(Collectors.joining("\n"));
                    reader.close();
                    stream.close();

                    if (newRecordText != null && newRecordText.length() != 0) {
                        try {
                            //BASICS
                            JSONObject newRecordJson = new JSONObject(newRecordText);

                            String identifier = newRecordJson.getString("recordIdentifier");
                            String description = newRecordJson.getString("recordDescription");
                            String payload = newRecordJson.getString("recordPayload");
                            String rightAscension = newRecordJson.getString("recordRightAscension");
                            String declination = newRecordJson.getString("recordDeclination");

                            //Integer viewCount = newRecordJson.getInt("recordViewCount");
                            //Double rating = newRecordJson.getDouble("recordRating");
                            //Interger ratingCount = newRecordJson.getInt("recordRatingCount");

                            //OWNER
                            String owner;
                            String ownerUsername = getUsernameFromAuth(exchange);
                            if (newRecordJson.has("recordOwner")) {
                                owner = newRecordJson.getString("recordOwner");
                            } else {
                                owner = userAuthenticator.getNickname(ownerUsername);
                            }

                            //OBSERVATORY
                            boolean isObservatoryPresent = false;
                            String observatoryName = null;
                            Double latitude = 0.0;
                            Double longitude = 0.0;
                            if (newRecordJson.has("observatory")) {
                                JSONArray observatoryArray = newRecordJson.getJSONArray("observatory");
                                if (observatoryArray.length() > 0) {
                                    isObservatoryPresent = true;
                                    JSONObject observatory = observatoryArray.getJSONObject(0);
                                    observatoryName = observatory.getString("observatoryName");
                                    latitude = observatory.getDouble("latitude");
                                    longitude = observatory.getDouble("longitude");
                                }
                            }

                            //TIME
                            ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                            String dateText = date.format(formatter);
                           
                            //WEATHER
                            JSONObject observatoryWeather = null;
                            boolean isWeatherPresent = false;
                            Double temperatureInKelvins = -1.0;
                            Double cloudinessPercentance = 0.0;
                            Double bagroundLightVolume = 0.0;
                            if (newRecordJson.has("observatoryWeather")) {
                                isWeatherPresent = true;
                                observatoryWeather = WeatherManager.getWeather(latitude, longitude, dateText);
                                temperatureInKelvins = observatoryWeather.getDouble("temperatureInKelvins");
                                cloudinessPercentance = observatoryWeather.getDouble("cloudinessPercentance");
                                bagroundLightVolume = observatoryWeather.getDouble("bagroundLightVolume");  
                            }
                             

                            //System.out.println("DatarecordHandler > handlePost > RightAscension = " + rightAscension);
                            if (!(identifier == null 
                                || identifier.isEmpty() 
                                || newRecordJson.get("recordIdentifier") instanceof JSONObject
                                || description == null 
                                || description.isEmpty() 
                                || newRecordJson.get("recordDescription") instanceof JSONObject
                                || payload == null 
                                || payload.isEmpty() 
                                || newRecordJson.get("recordPayload") instanceof JSONObject
                                || rightAscension == null 
                                || rightAscension.isEmpty() 
                                || newRecordJson.get("recordRightAscension") instanceof JSONObject
                                || declination == null 
                                || declination.isEmpty() 
                                || newRecordJson.get("recordDeclination") instanceof JSONObject))
                            {
                                //System.out.println("DatarecordHandler > handlePost > Adding the record " + identifier + " " + payload);
                                db.addRecord(new ObservationRecord(
                                    null, identifier, description, 
                                    payload, rightAscension, declination, dateText, owner, 
                                    isObservatoryPresent, observatoryName, latitude, 
                                    longitude, isWeatherPresent, temperatureInKelvins, 
                                    cloudinessPercentance, bagroundLightVolume, ownerUsername, 
                                    null, null, 0, 0.0, 0
                                ));
                                sendResponse(exchange, 200, "Record added");
                            } else {
                                sendResponse(exchange, 413, "No proper record information");
                            }
                        } catch (JSONException e) {
                            //System.out.println("DatarecordHandler > handlePost > Json parse error, faulty record json");
                            sendResponse(exchange, 413, "No proper record information");
                        } 
                    } else {
                        sendResponse(exchange, 412, "No record information");
                    }
                } else {
                    sendResponse(exchange, 407, "Content type is not application/json");
                }
            } else {
                sendResponse(exchange, 411, "No content type in request");
            }
        } catch (Exception e) {
            System.out.println("DatarecordHandler > handlePost > Something went wrong");
            sendResponse(exchange, 500, "handlePostException");
        }
    }

    private void checkContentType (HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("Content-Type")
        && headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
            return;
        } 
        throw new IllegalArgumentException("Conten type must be application/json");
    } 

    private Integer getIdFromRequest (HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();

        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Missing query");
        }

        if (query.split("=").length != 2 || !query.split("=")[0].equals("id")) {
            throw new IllegalArgumentException("Invalid query");
        } 

        try {
            return Integer.valueOf(query.split("=")[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid query, cannot parse id");
        }
    }

    private JSONObject getJsonFromRequest (HttpExchange exchange) {
        String newRecordText;
        try (
            InputStream stream = exchange.getRequestBody();
            BufferedReader reader = 
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        ) {
            newRecordText = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read request body");
        }

        if (newRecordText == null || newRecordText.isEmpty()) {
            throw new IllegalArgumentException("Missing json");
        }

        try {
            return new JSONObject(newRecordText);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid json");
        } 
    }

    private void checkJsonField (JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            throw new IllegalArgumentException("Json field " + key + " is missing or null");
        }
    }

    private String getStringFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getString(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a string");
        }
    }

    private Integer getIntegerFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getInt(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be an integer");
        }
    }

    private Double getDoubleFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getDouble(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a double");
        }
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

    private ObservationRecord updateRecordContent(
            HttpExchange exchange, 
            ObservationRecord record, 
            JSONObject json
    ) {
        String username = getUsernameFromAuth(exchange);
        if (!record.getOwnerUsername().equals(username)) {
            throw new IllegalArgumentException("Another owner owns the record");
        }
        
        //BASICS
        record.setIdentifier(getStringFromJson(json, "recordIdentifier"));
        record.setDescription(getStringFromJson(json, "recordDescription"));
        record.setPayload(getStringFromJson(json, "recordPayload"));
        record.setRightAscension(getStringFromJson(json, "recordRightAscension"));
        record.setDeclination(getStringFromJson(json, "recordDeclination"));

        //OBSERVATORY
        if (json.has("observatory")) {
            record = addObservatoryToRecord(record, json);
        }

        //UPDATE INFO
        ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String updateDateText = date.format(formatter);
        record.setModified(updateDateText);
        record.setUpdateReason(getStringFromJson(json, "updateReason"));

        return record;
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        checkContentType(exchange);
        Integer id = getIdFromRequest(exchange);

        //GET THE CURRENT RECORD
        ObservationRecord record = db.getRecordById(id);
        if (record == null) {
            sendResponse(exchange, 400, "wrong id, not found");
            return;
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

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
        OutputStream stream = exchange.getResponseBody();
        stream.write(message.getBytes());
        stream.flush();
        stream.close();
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //System.out.println("DatarecordHandler > handle > Request handled in thread " + Thread.currentThread().threadId());
        
        System.out.println("DatarecordHandler > handle > we got a request");
        String method = exchange.getRequestMethod().toUpperCase();
        try {
            //System.out.println("DatarecordHandler > handle > We got " + method + " request");
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
