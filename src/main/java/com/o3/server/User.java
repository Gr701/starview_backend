package com.o3.server;

public class User {
    private String login;
    private String password;
    private String email;

    public User(String login, String password, String email) {
        this.login = login;
        this.password = password;
        this.email = email;
    }

    public String getlogin() {return login;} 
    public String getPassword() {return password;} 
    public String getEmail() {return email;} 
}
