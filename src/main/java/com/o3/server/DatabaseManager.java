package com.o3.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.JSONObject;

public class DatabaseManager {

    private Connection connection = null;
    private static DatabaseManager instance = null;

    private DatabaseManager() {
        try {
            open("", "MessageDB");
        } catch (SQLException e) {
            System.out.println("DatabaseManager > constructor > SQLException");
        }
    }
    
    private boolean init() throws SQLException {
        String name = "MessageDB";
        String database = "jdbc:sqlite:" + name;
        connection = DriverManager.getConnection(database);

        if (connection != null) {
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "login VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(250) NOT NULL, " 
                + "email VARCHAR(50) NOT NULL, " 
                + "nickname VARCHAR(50) NOT NULL)";
            
            String createRecordsTable = "CREATE TABLE IF NOT EXISTS records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " 
                + "recordIdentifier VARCHAR(50) NOT NULL, "
                + "recordDescription VARCHAR(50), " 
                + "recordPayload VARCHAR(4000), "
                + "recordRightAscension VARCHAR(50), "
                + "recordDeclination VARCHAR(50), "
                + "recordTimeReceived VARCHAR(50) NOT NULL, "
                + "recordOwner VARCHAR(50), "
                + "observatoryName VARCHAR(50), "
                + "latitude DOUBLE, "
                + "longitude DOUBLE, "
                + "temperatureInKelvins DOUBLE, "
                + "cloudinessPercentance DOUBLE, "
                + "bagroundLightVolume DOUBLE, "
                + "ownerUsername VARCHAR(50), "
                + "updateReason VARCHAR(50), "
                + "modified VARCHAR(50))";

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createRecordsTable);
                System.out.println("DatabaseManager > init > Database successfully created");
            }
            return true;
        } else {
            System.out.println("DatabaseManager > init > Database creation failed");
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
            System.out.println("DatabaseManager > open > Database does exist, getting connection");
            connection = DriverManager.getConnection("jdbc:sqlite:"+path+name);
        } else {
            System.out.println("DatabaseManager > open > Database does not exist, initializing");
            init();
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            System.out.println("DatabaseManager > close > Closing database connection");
            connection = null;
        }
    }

    public void addUser(User user) {

        String insertUserRow = "INSERT INTO users (login, password, email, nickname) "
            + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertUserRow)) {
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getNickname());
            
            if (stmt.executeUpdate() > 0) {
                //System.out.println("DatabaseManager > addUser > User successfully added");
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addUser > SQLException > \n" + e.getMessage());
        }
    }

    public User getUser(String login) {
        String getUserRow = "SELECT login, password, email, nickname FROM users WHERE login = ?";
        try (PreparedStatement stmt = connection.prepareStatement(getUserRow)) {
            stmt.setString(1, login);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) {
                    return new User(
                        results.getString("login"),
                        results.getString("password"),
                        results.getString("email"),
                        results.getString("nickname")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getUser > SQLException");
        }
        return null;
    }

    public void addRecord(ObservationRecord record) {
        //System.out.println("DatabaseManager > addRecord > Record adding");
        String insertRecordRow = "INSERT INTO records " 
            + "(recordIdentifier, recordDescription, recordPayload, recordRightAscension, "
            + "recordDeclination, recordTimeReceived, recordOwner, observatoryName, "
            + "latitude, longitude, temperatureInKelvins, cloudinessPercentance, "
            + "bagroundLightVolume, ownerUsername, updateReason, modified) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertRecordRow)) {
            stmt.setString(1, record.getIdentifier());
            stmt.setString(2, record.getDescription());
            stmt.setString(3, record.getPayload());
            stmt.setString(4, record.getRightAscension());
            stmt.setString(5, record.getDeclination());
            stmt.setString(6, record.getTimeReceived());
            stmt.setString(7, record.getOwner());
            stmt.setString(8, record.getObservatoryName());
            stmt.setDouble(9, record.getLatitude());
            stmt.setDouble(10, record.getLongitude());
            stmt.setDouble(11, record.getTemperatureInKelvins());
            stmt.setDouble(12, record.getCloudinessPercentance());
            stmt.setDouble(13, record.getBagroundLightVolume());
            stmt.setString(14, record.getOwnerUsername());
            stmt.setString(15, record.getUpdateReason());
            stmt.setString(16, record.getModified());
        
            //System.out.println("DatabaseManager > addRecord > Record adding 2");
            if (stmt.executeUpdate() > 0) {
                //System.out.println("DatabaseManager > addRecord > Record successfully added");
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addRecord > SQLException");
        }
    }

    public void updateRecord(ObservationRecord record) {
        String updateRecordRow = "UPDATE records SET "
            + "recordIdentifier = ?, "
            + "recordDescription = ?, "
            + "recordPayload = ?, "
            + "recordRightAscension = ?, "
            + "recordDeclination = ?, "
            + "recordTimeReceived = ?, "
            + "recordOwner = ?, "
            + "observatoryName = ?, "
            + "latitude = ?, "
            + "longitude = ?, "
            + "temperatureInKelvins = ?, "
            + "cloudinessPercentance = ?, "
            + "bagroundLightVolume = ?, "
            + "ownerUsername = ?, "
            + "updateReason = ?, "
            + "modified = ? "
            + "WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateRecordRow)) {
            stmt.setString(1, record.getIdentifier());
            stmt.setString(2, record.getDescription());
            stmt.setString(3, record.getPayload());
            stmt.setString(4, record.getRightAscension());
            stmt.setString(5, record.getDeclination());
            stmt.setString(6, record.getTimeReceived());
            stmt.setString(7, record.getOwner());
            stmt.setString(8, record.getObservatoryName());
            stmt.setDouble(9, record.getLatitude());
            stmt.setDouble(10, record.getLongitude());
            stmt.setDouble(11, record.getTemperatureInKelvins());
            stmt.setDouble(12, record.getCloudinessPercentance());
            stmt.setDouble(13, record.getBagroundLightVolume());
            stmt.setString(14, record.getOwnerUsername());
            stmt.setString(15, record.getUpdateReason());
            stmt.setString(16, record.getModified());
            stmt.setInt(17, record.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DatabaseManager > upadateRecord > SQLException");
        }
    }

    public ArrayList<ObservationRecord> getRecords() {
        ArrayList<ObservationRecord> records = new ArrayList<ObservationRecord>();
        try (
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM records");
            ResultSet results = stmt.executeQuery();
        ) {
            while (results.next()) {
                boolean isObservatoryPresent = (results.getString("observatoryName") == null) ? false : true;
                boolean isWeatherPresent = (results.getDouble("temperatureInKelvins") == -1.0) ? false : true;
                ObservationRecord record = new ObservationRecord(
                    results.getInt("id"),
                    results.getString("recordIdentifier"),
                    results.getString("recordDescription"),
                    results.getString("recordPayload"),
                    results.getString("recordRightAscension"),
                    results.getString("recordDeclination"),
                    results.getString("recordTimeReceived"),
                    results.getString("recordOwner"),
                    isObservatoryPresent,
                    results.getString("observatoryName"),
                    results.getDouble("latitude"),
                    results.getDouble("longitude"),
                    isWeatherPresent,
                    results.getDouble("temperatureInKelvins"),
                    results.getDouble("cloudinessPercentance"),
                    results.getDouble("bagroundLightVolume"),
                    results.getString("ownerUsername"),
                    results.getString("updateReason"),
                    results.getString("modified")
                );
                records.add(record);
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRecords > SQLException \n" + e.getMessage());
        }
        return records;
    }

    public ObservationRecord getRecordById(int id) {
        ObservationRecord record = null;
        String query = "SELECT * FROM records WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) { 
                    boolean isObservatoryPresent = (results.getString("observatoryName") == null) ? false : true;
                    boolean isWeatherPresent = (results.getDouble("temperatureInKelvins") == -1.0) ? false : true;
                    record = new ObservationRecord(
                        results.getInt("id"),
                        results.getString("recordIdentifier"),
                        results.getString("recordDescription"),
                        results.getString("recordPayload"),
                        results.getString("recordRightAscension"),
                        results.getString("recordDeclination"),
                        results.getString("recordTimeReceived"),
                        results.getString("recordOwner"),
                        isObservatoryPresent,
                        results.getString("observatoryName"),
                        results.getDouble("latitude"),
                        results.getDouble("longitude"),
                        isWeatherPresent,
                        results.getDouble("temperatureInKelvins"),
                        results.getDouble("cloudinessPercentance"),
                        results.getDouble("bagroundLightVolume"),
                        results.getString("ownerUsername"),
                        results.getString("updateReason"),
                        results.getString("modified")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRecordById > SQLException \n" + e.getMessage());
        }
        return record; 
    }

    public boolean isRecordTableEmpty() {
        try (
            PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM records");
            ResultSet results = stmt.executeQuery();
        ) {
            if (results.next()) {
                return results.getInt(1) == 0;
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > isRecordTableEmpty > SQLException \n" + e.getMessage());
        }
        return true;
    }
}
