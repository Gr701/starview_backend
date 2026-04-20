package com.o3.server;

import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpExchange;

public class UserAuthenticator extends BasicAuthenticator {

    private DatabaseManager db;
    private SecureRandom secureRandom;

    public UserAuthenticator () {
        super("datarecord");
        secureRandom = new SecureRandom();
        db = DatabaseManager.getInstance();
        //wont work since password has to be hashed and salted
        db.addUser(new User("dummy", "passwd", "dummail", "nikinimi"));
    }

    public boolean addUser(String login, String password, String email, String nickname) {
        if (db.getUser(login) == null) {
            String hashedPassword = securePassword(password);
            return db.addUser(new User(login, hashedPassword, email, nickname));
        }
        return false;
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
        Headers h = exchange.getResponseHeaders();

        h.add("Access-Control-Allow-Origin", "http://localhost:8000");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod().toUpperCase())) {
            return new Success(new HttpPrincipal("preflight", "what_is_realm"));
        }
        return super.authenticate(exchange);
    }

    public String getNickname(String login) {
        return db.getUser(login).getNickname();
    }

    private String securePassword(String password) {
        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        return Crypt.crypt(password, salt);
    }
}
