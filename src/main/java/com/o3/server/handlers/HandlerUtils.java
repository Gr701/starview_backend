package com.o3.server.handlers;

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
import java.util.Base64;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class HandlerUtils {
    private HandlerUtils () {}

    public static String getCurrentFormattedTime() {
        ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return date.format(formatter);
    }

    public static void checkContentType (HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("Content-Type")
        && headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
            return;
        } 
        throw new IllegalArgumentException("Conten type must be application/json");
    } 

    public static String getUsernameFromAuth(HttpExchange exchange) {
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

    public static Integer getIdFromRequest (HttpExchange exchange) {
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

    public static JSONObject getJsonFromRequest (HttpExchange exchange) {
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

    public static void checkJsonField (JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            throw new IllegalArgumentException("Json field " + key + " is missing or null");
        }
    }

    public static String getStringFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getString(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a string");
        }
    }

    public static Integer getIntegerFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getInt(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be an integer");
        }
    }

    public static Double getDoubleFromJson(JSONObject json, String key) {
        checkJsonField(json, key);
        try {
            return json.getDouble(key);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Json value at " + key + " must be a double");
        }
    }

    public static void sendResponse(HttpExchange exchange, int code, String message) {
        message += "\n";
        try (OutputStream stream = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
            stream.write(message.getBytes("UTF-8"));
            stream.flush();
        } catch (IOException e) {
            System.out.println("DatarecordHandler > sendResponse > " + e.getMessage());
        }
    }
}
