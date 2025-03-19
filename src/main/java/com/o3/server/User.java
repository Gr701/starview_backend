package com.o3.server;

public class User {
    private String login;
    private String password;
    private String email;
    private String nickname;

    public User(String login, String password, String email, String nickname) {
        this.login = login;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
    }

    public String getLogin() {return login;} 
    public String getPassword() {return password;} 
    public String getEmail() {return email;} 
    public String getNickname() {return nickname;}
}
