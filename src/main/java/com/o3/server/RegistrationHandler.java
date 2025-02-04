package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class RegistrationHandler implements HttpHandler {

    UserAuthenticator userAuthenticator;

    public RegistrationHandler(UserAuthenticator ua) {
        userAuthenticator = ua;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("POST".equals(method)) {
            System.out.println("we got registration post request");
            InputStream is = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

            String[] creds = text.split(":");
            if (creds.length != 2) {
                String message = "Wrong data format";
                exchange.sendResponseHeaders(400, message.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(message.getBytes());
                os.flush();
                os.close();
            } else if (!userAuthenticator.addUser(creds[0], creds[1])) {
                String message = "User already exists";
                exchange.sendResponseHeaders(403, message.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(message.getBytes());
                os.flush();
                os.close();
            } else {
                exchange.sendResponseHeaders(200, -1);
            }
            
        } else {
            String message = "Not supported";
            exchange.sendResponseHeaders(400, message.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes());
            os.flush();
            os.close();
        }
    }
    
}
