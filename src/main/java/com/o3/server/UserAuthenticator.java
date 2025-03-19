package com.o3.server;

import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends BasicAuthenticator {

    private DatabaseManager db;
    private SecureRandom secureRandom;

    public UserAuthenticator () {
        super("datarecord");
        secureRandom = new SecureRandom();
        db = DatabaseManager.getInstance();
        db.addUser(new User("dummy", "passwd", "dummail", "nikinimi"));
    }

    public boolean addUser(String login, String password, String email, String nickname) {
        if (db.getUser(login) == null) {
            String hashedPassword = securePassword(password);
            db.addUser(new User(login, hashedPassword, email, nickname));
            return true;
        }
        return false;
    }

    public boolean checkCredentials(String login, String password) {
        User user = db.getUser(login);
        String hashedPassword = user.getPassword();
        //if (user != null && user.getPassword().equals(password)) {
        if (user != null && hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
            return true;
        }
        return false;
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
