package com.o3.server;

//package com.viikko1;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class Server implements HttpHandler {

    String messages = "No messages";

    private Server() {
    }

    private String handleGetRequset(HttpExchange exchange) {
        return exchange.getRequestURI().toString();
    }

    private void handleGetResponse(HttpExchange exchange) throws IOException {
        byte [] bytes = messages.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);

        OutputStream stream = exchange.getResponseBody();
        stream.write(messages.getBytes());
        stream.flush();
        stream.close();
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        InputStream stream = exchange.getRequestBody();
        String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .lines().collect(Collectors.joining("\n"));

        if (messages.equals("No messages")) {
            messages = "";
        }
        messages += text;
        stream.close();
    }

    private void handlePostResponse(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, -1);
    }

    private void handleResponse(HttpExchange exchange) throws IOException {
        String message = "Not supported";
        exchange.sendResponseHeaders(400, message.getBytes("UTF-8").length);
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
            handleGetRequset(exchange);
            handleGetResponse(exchange);

        } else if ("POST".equals(method)) {
            System.out.println("we got post request");
            handlePostRequest(exchange);
            handlePostResponse(exchange);
        } else {
            System.out.println("we got other request");
            handleResponse(exchange);
        }
    }

    public static void main(String[] args) throws Exception {
        //create the http server to port 8001 with default logger
        HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
        //create context that defines path for the resource, in this case a "help"
        //server.createContext("/help", new Server());
        server.createContext("/datarecord", new Server());
        // creates a default executor
        server.setExecutor(null); 
        server.start(); 
        System.out.println("\nServer started");
    }
}