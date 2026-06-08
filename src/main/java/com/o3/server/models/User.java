package com.o3.server.models;

import org.json.JSONObject;

public class User {
    private Integer id;
    private String login;
    private String password;
    private String email;
    private String nickname;

    public User(Integer id, String login, String password, String email, String nickname) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
    }

    public Integer getId() {return id;}
    public String getLogin() {return login;}
    public String getPassword() {return password;} 
    public String getEmail() {return email;} 
    public String getNickname() {return nickname;}

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("username", login);
        json.put("email", email);
        json.put("nickname", nickname);
        return json;
    }
}
