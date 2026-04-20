package com.o3.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;


public class RegistrationHandler implements HttpHandler {

    private final UserAuthenticator userAuthenticator;

    public RegistrationHandler(UserAuthenticator ua) {
        userAuthenticator = ua;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();

        h.add("Access-Control-Allow-Origin", "http://localhost:8000");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        addCorsHeaders(exchange);

        exchange.sendResponseHeaders(code, message.getBytes("UTF-8").length);
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.flush();
        os.close();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
        } else if ("POST".equals(method)) {
            Headers headers = exchange.getRequestHeaders();
            if (headers.containsKey("Content-Type")) {
                if (headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
                    //System.out.println("RegistrationHandler > handle > We got registration post request");

                    InputStream stream = exchange.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                    String newUserText = reader.lines().collect(Collectors.joining("\n"));
                    reader.close();
                    stream.close();

                    if (newUserText != null && newUserText.length() != 0) {
                        try {
                            JSONObject newUserJson = new JSONObject(newUserText);
                            String username = newUserJson.getString("username");
                            String password = newUserJson.getString("password");
                            String email = newUserJson.getString("email");
                            String userNickname = newUserJson.getString("userNickname");

                            if (username.length() != 0 && password.length() != 0 && userNickname.length() != 0) {
                                //System.out.println("RegistrationHandler > handle > Registering user " + username + " " + password);
                                if (userAuthenticator.addUser(username, password, email, userNickname)) {
                                    sendResponse(exchange, 200, "User registred");
                                } else {
                                    sendResponse(exchange, 405, "User already exists");
                                }
                            } else {
                                sendResponse(exchange, 413, "No proper user credentials");
                            }
                        } catch (JSONException e) {
                            //System.out.println("RegistrationHandler > handle > Json parse error, faulty user json");
                            sendResponse(exchange, 413, "No proper user credentials");
                        } 
                    } else {
                        sendResponse(exchange, 412, "No user credentials");
                    }
                } else {
                    sendResponse(exchange, 407, "Content type is not application/json");
                }
            } else {
                sendResponse(exchange, 411, "No content type in request");
            }
        } else {
            sendResponse(exchange, 400, "Not supported");
        }
    }
}
