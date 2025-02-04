package com.o3.server;

//package com.viikko1;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;


public class Server implements HttpHandler {

    private String messages = "No messages";

    private Server() {
    }

    /*
     * Https stuff, SSL 
     */
    private static SSLContext myServerSSLContext(String[] args) throws Exception {
        //char[] passphrase = "progr3key".toCharArray();
        char[] passphrase = args[1].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        //ks.load(new FileInputStream("keystore.jks"), passphrase);
        ks.load(new FileInputStream(args[0]), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }
    
    /*
     * GET
     */
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

    /*
     * POST
     */
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

    /*
     * OTHER
     */
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
        try {
            //create the http server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);

            UserAuthenticator userAuthenticator = new UserAuthenticator();

            SSLContext sslContext = myServerSSLContext(args);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            //create context that defines path for the resource, in this case a "help"
            //server.createContext("/help", new Server());
            HttpContext datarecordContext = server.createContext("/datarecord", new Server());
            datarecordContext.setAuthenticator(userAuthenticator);

            server.createContext("/registration", new RegistrationHandler(userAuthenticator)); 

            // creates a default executor
            server.setExecutor(null); 
            server.start(); 
            System.out.println("\nServer started");
        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}