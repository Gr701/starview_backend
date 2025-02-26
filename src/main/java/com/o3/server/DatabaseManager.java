package com.o3.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONObject;

public class DatabaseManager {

    private Connection connection = null;
    private static DatabaseManager instance = null;

    private DatabaseManager() {
        try {
            init();
        } catch (SQLException e) {
            System.out.println("Log - SQL exception at constructor");
        }
    }
    
    private boolean init() throws SQLException {
        String name = "MessageDB";
        String database = "jdbc:sqlite:" + name;
        connection = DriverManager.getConnection(database);

        if (connection != null) {
            String createBasicDB = "create table data (user vaerchar(50) NOT NULL, usermessage varchar(500) NOT NULL)";
            Statement createStatement = connection.createStatement();    
            createStatement.executeUpdate(createBasicDB);
            createStatement.close();
            System.out.println("Database successfully created");
            return true;
        } else {
            System.out.println("DB creation failed");
            return false;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void open(String path, String name) throws SQLException {
        File f = new File(name);
        boolean doesExist = f.exists() && f.isFile();
        if (doesExist) {
            connection = DriverManager.getConnection("jdbc:sqlite:"+path+name);
        } else {
            init();
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            System.out.println("Closing database connection");
            connection = null;
        }
    }

    public void setMessage(JSONObject message) throws SQLException {
        String setMessageString = "insert into data " + 
                    "VALUES('" + message.getString("user") + "','" + message.getString("message") + "')";
        Statement createStatement;
        createStatement = connection.createStatement();
        createStatement.executeUpdate(setMessageString);
        createStatement.close();
    }

    public JSONObject getMessage() throws SQLException {
        Statement queryStatement = null;
        JSONObject obj = new JSONObject();

        String getMessageString = "select rowid, user, usermessage from data";

        queryStatement = connection.createStatement();
        ResultSet rs = queryStatement.executeQuery(getMessageString);

        while (rs.next()) {
            obj.put("id", rs.getInt("rowid"));
            obj.put("user", rs.getString("user"));
            obj.put("usermessage", rs.getString("usermessage"));
        }
        return obj;
    }

}
