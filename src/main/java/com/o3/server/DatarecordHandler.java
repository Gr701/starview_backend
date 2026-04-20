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
        //messages = new ArrayList<ObservationRecord>();
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


    private void handlePut(HttpExchange exchange) throws IOException {
        try {
            Headers headers = exchange.getRequestHeaders();
            if (headers.containsKey("Content-Type")) {
                if (headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
                    //System.out.println("DatarecordHandler > handlePost > We got record post request");

                    //GET THE ID
                    Integer id = null;
                    try {
                        URI requestURI = exchange.getRequestURI();
                        String query = requestURI.getQuery();
                        id = Integer.valueOf(query.split("=")[1]);
                        if (!query.split("=")[0].equals("id")) {
                            sendResponse(exchange, 400, "wrong id");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("DatarecordHandler > handlePut > cannot parse id \n" + e.getMessage());
                    }
                    //
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

                            //OWNER
                            String owner;
                            String ownerUsername = getUsernameFromAuth(exchange);
                            if (newRecordJson.has("recordOwner")) {
                                owner = newRecordJson.getString("recordOwner");
                            } else {
                                owner = userAuthenticator.getNickname(ownerUsername);
                            }

                            //CHECK THE RECORD IN DB
                            ObservationRecord r = db.getRecordById(id);
                            //System.out.println(newRecordJson);
                            if (r == null) {
                                sendResponse(exchange, 400, "wrong id");
                                return;
                            }
                            if (!r.getOwnerUsername().equals(ownerUsername)) {
                                sendResponse(exchange, 400, "wrong user");
                                //System.out.println("DatarecordHandler > handlePutt > wrong user " + ownerUsername + " " + r.getOwnerUsername());
                                return;
                            }

                            //STATISTICS
                            Integer viewCount = r.getViewCount();
                            if (newRecordJson.has("addView")) {
                                viewCount += 1;
                            }
                            Integer ratingCount = r.getRatingCount();
                            Double rating = r.getRating();
                            if (newRecordJson.has("recordRating") 
                                    && !newRecordJson.isNull("recordRating")) {
                                Double incomingRating = newRecordJson.getDouble("recordRating");
                                rating = (rating * ratingCount + incomingRating) / (ratingCount + 1);
                                ratingCount += 1;
                            }
                            
                            //INITIAL TIME
                            String dateText = r.getTimeReceived();
                            //newRecordJson.getString("recordTimeReceived");

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

                            //UPDATE INFO
                            String updateReason = r.getUpdateReason();
                            String updateDateText = r.getModified();
                            if (newRecordJson.has("userEdit")) {
                                ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                                updateDateText = date.format(formatter);

                                updateReason = "N/A";
                                //System.out.println(newRecordJson);
                                if (newRecordJson.has("updateReason")) {
                                    updateReason = newRecordJson.getString("updateReason");
                                }
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
                                db.updateRecord(new ObservationRecord(
                                    id, identifier, description, payload, rightAscension, 
                                    declination, dateText, owner, isObservatoryPresent, 
                                    observatoryName, latitude, longitude, isWeatherPresent,
                                    temperatureInKelvins, cloudinessPercentance, 
                                    bagroundLightVolume, ownerUsername, updateReason, updateDateText,
                                    viewCount, rating, ratingCount
                                ));
                                sendResponse(exchange, 200, "Record updated");
                            } else {
                                sendResponse(exchange, 413, "No proper record information");
                            }
                        } catch (JSONException e) {
                            System.out.println(e.getMessage());
                            System.out.println("DatarecordHandler > handlePut > Json parse error, faulty record json \n" + newRecordText);
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
            System.out.println("DatarecordHandler > handlePut > Something went wrong \n" + e.getMessage());
            sendResponse(exchange, 400, "handlePutException");
        }
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
            if ("GET".equals(method)) {
                //System.out.println("DatarecordHandler > handle > We got get request");
                handleGet(exchange);
            } else if ("POST".equals(method)) {
                //System.out.println("DatarecordHandler > handle > We got post request");
                handlePost(exchange);
            } else if ("PUT".equals(method)) {
                //System.out.println("DatarecordHandler > handle > We got put request");
                handlePut(exchange);
            } else {
                //System.out.println("DatarecordHandler > handle > We got other request");
                sendResponse(exchange, 400, "Not supported");
            }
        } catch (Exception e) {
            System.out.println("DatarecordHandler > handle > Exception > " + e);
            e.printStackTrace(System.out); 
        }
    }
}
