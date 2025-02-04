package com.o3.server;

import com.sun.net.httpserver.BasicAuthenticator;

import java.util.Map;
import java.util.Hashtable;

public class UserAuthenticator extends BasicAuthenticator {
    private Map<String, String> users = null;

    public UserAuthenticator () {
        super("datarecord");

        users = new Hashtable<String, String>();
        users.put("dummy", "passwd");
    }

    public boolean addUser(String login, String password) {
        if (users.get(login) == null) {
            users.put(login, password);
            return true;
        }
        return false;
    }

    public boolean checkCredentials(String login, String password) {
        if (users.get(login).equals(password)) {
            return true;
        }
        return false;
    }
}
