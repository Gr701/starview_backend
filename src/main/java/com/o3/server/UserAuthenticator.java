package com.o3.server;

import com.sun.net.httpserver.BasicAuthenticator;

import java.util.Map;
import java.util.Hashtable;

public class UserAuthenticator extends BasicAuthenticator {
    private Map<String, User> users = null;

    public UserAuthenticator () {
        super("datarecord");

        users = new Hashtable<String, User>();
        users.put("dummy", new User("dummy", "passwd", "dummail"));
    }

    public boolean addUser(String login, String password, String email) {
        if (users.get(login) == null) {
            users.put(login, new User(login, password, email));
            return true;
        }
        return false;
    }

    public boolean checkCredentials(String login, String password) {
        User user = users.get(login);
        if (user != null && user.getPassword().equals(password)) {
            return true;
        }
        return false;
    }
}
