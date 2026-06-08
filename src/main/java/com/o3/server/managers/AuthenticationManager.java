package com.o3.server.managers;

import org.apache.commons.codec.digest.Crypt;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpExchange;

import com.o3.server.managers.DatabaseManager;
import com.o3.server.models.User;

public class AuthenticationManager extends BasicAuthenticator {

    private DatabaseManager db;

    public AuthenticationManager () {
        super("datarecord");
        db = DatabaseManager.getInstance();
    }

    @Override
    public boolean checkCredentials(String login, String password) {
        //System.out.println("UserAuthenticator > checkCredentials called");
        User user = db.getUser(login);
        if (user == null) {
            return false;
        }
        String hashedPassword = user.getPassword();
        //if (user != null && user.getPassword().equals(password)) {
        if (user != null && hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
            return true;
        }
        return false;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        //change in http utils as well
        Headers h = exchange.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "http://localhost:8000");
        h.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        addCorsHeaders(exchange);
        System.out.println("we are authenticating");
        if ("OPTIONS".equals(exchange.getRequestMethod().toUpperCase())) {
            System.out.println("we are authenticating OPTIONS");
            return new Success(new HttpPrincipal("preflight", "what_is_realm"));
        }
        return super.authenticate(exchange);
    }
}
