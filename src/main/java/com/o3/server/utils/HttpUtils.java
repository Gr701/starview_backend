package com.o3.server.utils;

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
import java.util.NoSuchElementException;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {
    private HttpUtils () {}

    public record QueryParams (
        String identification,
        String nickname,
        String before,
        String after
    ) {}

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

        if (query.split("=").length != 2 || !query.split("=")[0].equals("recordId")) {
            throw new IllegalArgumentException("Invalid query");
        } 

        try {
            return Integer.valueOf(query.split("=")[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid query, cannot parse id");
        }
    }

    public static QueryParams getSearchParamsFromRequest(HttpExchange exchange) {
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        
        if (query == null) {
            throw new IllegalArgumentException("query is null");
        }

        String identification = null;
        String nickname = null;
        String before = null;
        String after = null;

        String[] paramPairs = query.split("&");
        for (String paramPair : paramPairs) {
            String key = paramPair.split("=")[0];
            String value = paramPair.split("=")[1];
            switch (key) {
                case "identification":
                    identification = value;
                    break;
                case "nickname":
                    nickname = value;
                    break;
                case "before":
                    before = value;
                    break;
                case "after":
                    after = value;
                    break;
                default:
                    break;
            }
        }

        return new QueryParams(identification, nickname, before, after);
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

    public static void addCorsHeaders(HttpExchange exchange) {
        //this method is only for contexts which do not require authenticaiton
        //there these headers added in authentication too so it would duplicate otherwise
        Headers h = exchange.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "http://localhost:8000");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    public static void sendResponse(HttpExchange exchange, int code, String message) {
        message += "\n";
        try (OutputStream stream = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
            stream.write(message.getBytes("UTF-8"));
            stream.flush();
        } catch (IOException e) {
            System.out.println("sendResponse > Exception > " + e.getMessage());
        }
    }

    public static void handleException(HttpExchange exchange, Exception e) {
        if (e instanceof IllegalArgumentException) {
            sendResponse(exchange, 400, e.getMessage());
        } else if (e instanceof NoSuchElementException) {
            sendResponse(exchange, 404, e.getMessage());
        } else if (e instanceof RuntimeException) {
            sendResponse(exchange, 500, e.getMessage());
        } else {
            System.out.println(e.getMessage());
            e.printStackTrace(System.out); 
            sendResponse(exchange, 500, "Error handling the request");
        }
    }
}
