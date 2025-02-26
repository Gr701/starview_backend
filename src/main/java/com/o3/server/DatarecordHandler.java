package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    private ArrayList<ObservationRecord> messages;
    private UserAuthenticator userAuthenticator;

    public DatarecordHandler(UserAuthenticator ua) {
        messages = new ArrayList<ObservationRecord>();

        userAuthenticator = ua;
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        if (messages.isEmpty()) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        JSONArray responseMessages = new JSONArray();
        for (ObservationRecord r : messages) {
            JSONObject obj = new JSONObject();
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

            //System.out.println(r.getTimeReceived());
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
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("Content-Type")) {
            if (headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
                System.out.println("we got record post request");

                InputStream stream = exchange.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                String newRecordText = reader.lines().collect(Collectors.joining("\n"));
                reader.close();
                stream.close();

                if (newRecordText != null && newRecordText.length() != 0) {
                    try {
                        JSONObject newRecordJson = new JSONObject(newRecordText);
                        String identifier = newRecordJson.getString("recordIdentifier");
                        String description = newRecordJson.getString("recordDescription");

                        String payload = newRecordJson.getString("recordPayload");

                        String rightAscension = newRecordJson.getString("recordRightAscension");
                        String declination = newRecordJson.getString("recordDeclination");

                        String owner;
                        if (newRecordJson.has("recordOwner")) {
                            owner = newRecordJson.getString("recordOwner");
                        } else {
                            String username = getUsernameFromAuth(exchange);
                            owner = userAuthenticator.getNickname(username);
                        }


        
                        
                        boolean isObservatoryPresent = false;
                        String observatoryName = null;
                        String latitude = null;
                        String longitude = null;
                        if (newRecordJson.has("observatory")) {
                            JSONArray observatoryArray = newRecordJson.getJSONArray("observatory");
                            if (observatoryArray.length() > 0) {
                                isObservatoryPresent = true;
                                JSONObject observatory = observatoryArray.getJSONObject(0);
                                System.out.println(observatory.toString());

                                System.out.println("payload1");
                                observatoryName = observatory.getString("observatoryName");
                                System.out.println("payload3");
                                latitude = String.valueOf(observatory.getFloat("latitude"));
                                System.out.println("payload2");

                                longitude = String.valueOf(observatory.getFloat("longitude"));
                                

                            }
                        }
                        

                        System.out.println("payload4");

                        ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                        String dateText = date.format(formatter);

                        if (identifier.length() != 0 && description.length() != 0 && payload.length() != 0 
                            && rightAscension.length() != 0 && declination.length() != 0) 
                        {
                            System.out.println("adding the record " + identifier + " " + payload);
                            messages.add(new ObservationRecord(identifier, description, payload, rightAscension, 
                                                                declination, dateText, owner, isObservatoryPresent, 
                                                                observatoryName, latitude, longitude));
                            sendResponse(exchange, 200, "Record added");
                        } else {
                            sendResponse(exchange, 413, "No proper record information");
                        }
                    } catch (JSONException e) {
                        System.out.println("json parse error, faulty user json");
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
        String method = exchange.getRequestMethod().toUpperCase();

        if ("GET".equals(method)) {
            System.out.println("we got get request");
            handleGet(exchange);

        } else if ("POST".equals(method)) {
            System.out.println("we got post request");
            handlePost(exchange);
        } else {
            System.out.println("we got other request");
            sendResponse(exchange, 400, "Not supported");
        }
    }
}
