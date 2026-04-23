package com.o3.server.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.json.JSONObject;

import org.apache.commons.codec.digest.Crypt;
import java.security.SecureRandom;
import java.util.Base64;

import com.o3.server.models.User;
import com.o3.server.models.ObservationRecord;

public class DatabaseManager {

    private Connection connection = null;
    private static DatabaseManager instance = null;
    private SecureRandom secureRandom;

    private DatabaseManager() {
        try {
            open("", "MessageDB");
        } catch (SQLException e) {
            System.out.println("DatabaseManager > constructor > SQLException");
        }

        secureRandom = new SecureRandom();
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private boolean init() throws SQLException {
        String name = "MessageDB";
        String database = "jdbc:sqlite:" + name;
        connection = DriverManager.getConnection(database);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        if (connection != null) {
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "login VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(250) NOT NULL, " 
                + "email VARCHAR(50) NOT NULL, " 
                + "nickname VARCHAR(50) NOT NULL UNIQUE)";
            
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
                + "modified VARCHAR(50), "
                + "viewCount INTEGER, " 
                + "rating DOUBLE, "
                + "ratingCount INTEGER)";

            String createCommentsTable = """
                CREATE TABLE IF NOT EXISTS comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    userId INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    recordId INTEGER NOT NULL REFERENCES records(id) ON DELETE CASCADE, 
                    text TEXT NOT NULL
                )
            """;

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createRecordsTable);
                stmt.executeUpdate(createCommentsTable);
                System.out.println("DatabaseManager > init > Database successfully created");
            } catch (SQLException e) {
                System.out.println("DatabaseManager > init > SQLException > " + e);
            }
            return true;
        } else {
            System.out.println("DatabaseManager > init > Database creation failed");
            return false;
        }
    }

    public void open(String path, String name) throws SQLException {
        File f = new File(name);
        boolean doesExist = f.exists() && f.isFile();
        if (doesExist) {
            System.out.println("DatabaseManager > open > Database does exist, getting connection");
            connection = DriverManager.getConnection("jdbc:sqlite:"+path+name);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
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

    private String securePassword(String password) {
        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        return Crypt.crypt(password, salt);
    }

    public void addUser(User user) {

        String hashedPassword = securePassword(user.getPassword());

        String insertUserRow = "INSERT INTO users (login, password, email, nickname) "
            + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertUserRow)) {
            stmt.setString(1, user.getLogin());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getNickname());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addUser > SQLException > \n" + e.getMessage());
            throw new IllegalStateException("User already exists");
        }
    }

    public User getUser(String login) {
        String getUserRow = "SELECT id, login, password, email, nickname FROM users WHERE login = ?";
        try (PreparedStatement stmt = connection.prepareStatement(getUserRow)) {
            stmt.setString(1, login);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) {
                    return new User(
                        results.getInt("id"),
                        results.getString("login"),
                        results.getString("password"),
                        results.getString("email"),
                        results.getString("nickname")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getUser > SQLException > " + e.getMessage());
        }
        return null;
    }

    public User getUserById(int id) {
        String getUserRow = "SELECT id, login, password, email, nickname FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(getUserRow)) {
            stmt.setInt(1, id);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) {
                    return new User(
                        results.getInt("id"),
                        results.getString("login"),
                        results.getString("password"),
                        results.getString("email"),
                        results.getString("nickname")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getUser > SQLException > " + e.getMessage());
        }
        return null;
    }

    public void addRecord(ObservationRecord record) {
        //System.out.println("DatabaseManager > addRecord > modified = " + record.getModified());
        String insertRecordRow = "INSERT INTO records " 
            + "(recordIdentifier, recordDescription, recordPayload, recordRightAscension, "
            + "recordDeclination, recordTimeReceived, recordOwner, observatoryName, "
            + "latitude, longitude, temperatureInKelvins, cloudinessPercentance, "
            + "bagroundLightVolume, ownerUsername, updateReason, modified, "
            + "viewCount, rating, ratingCount) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setInt(17, record.getViewCount());
            stmt.setDouble(18, record.getRating());
            stmt.setInt(19, record.getRatingCount());
        
            //System.out.println("DatabaseManager > addRecord > Record adding 2");
            if (stmt.executeUpdate() > 0) {
                //System.out.println("DatabaseManager > addRecord > Record successfully added");
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addRecord > SQLException");
        }
    }

    public void updateRecord(ObservationRecord record) {
        //System.out.println("DatabaseManager > updateRecord called with record identifier" + record.getIdentifier());
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
            + "modified = ? ,"
            + "viewCount = ? ,"
            + "rating = ? ,"
            + "ratingCount = ? "
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
            stmt.setInt(17, record.getViewCount());
            stmt.setDouble(18, record.getRating());
            stmt.setInt(19, record.getRatingCount());
            stmt.setInt(20, record.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DatabaseManager > upadateRecord > SQLException > " + e);
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
                    results.getString("modified"),
                    results.getInt("viewCount"),
                    results.getDouble("rating"),
                    results.getInt("ratingCount")
                );
                records.add(record);
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRecords > SQLException \n" + e.getMessage());
        }
        return records;
    }

    public ObservationRecord getRecordById(int id) {
        String query = "SELECT * FROM records WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) { 
                    boolean isObservatoryPresent = (results.getString("observatoryName") == null) ? false : true;
                    boolean isWeatherPresent = (results.getDouble("temperatureInKelvins") == -1.0) ? false : true;
                    return new ObservationRecord(
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
                        results.getString("modified"),
                        results.getInt("viewCount"),
                        results.getDouble("rating"),
                        results.getInt("ratingCount")
                    );
                } else {
                    throw new NoSuchElementException("No record with given id");
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRecordById > SQLException \n" + e.getMessage());
            throw new RuntimeException("Error getting record by id");
        }
    }

    public ArrayList<ObservationRecord> serchRecords(String identifier, String owner, String before, String after) {
        ArrayList<ObservationRecord> records = new ArrayList<ObservationRecord>();
        ArrayList<String> parameters = new ArrayList<>();
        String query = "SELECT * FROM records WHERE 1 = 1";
        if (identifier != null) {
            query += " AND recordIdentifier = ?";
            parameters.add(identifier);
        } 
        if (owner != null) {
            query += " AND recordOwner = ?";
            parameters.add(owner);
        }
        if (before != null) {
            query += " AND recordTimeReceived < ?";
            parameters.add(before);
        }
        if (after != null) {
            query += " AND recordTimeReceived > ?";
            parameters.add(after);
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
            }
            try (ResultSet results = stmt.executeQuery()) {
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
                        results.getString("modified"),
                        results.getInt("viewCount"),
                        results.getDouble("rating"),
                        results.getInt("ratingCount")
                    );
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > searchRecords > SQLException \n" + e.getMessage());
        }

        return records;
    }

    public void deleteRecord(int id) {
        String query = "DELETE FROM records WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            if (stmt.executeUpdate() == 0) {
                throw new NoSuchElementException("No record with given id");
            }
        } catch (SQLException e) {
            //what do we doo here????
            throw new RuntimeException("Error deleting the record");
        }
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

    public void addComment(int userId, int recordId, String text) {
        String insertUserRow = "INSERT INTO comments (userId, recordId, text) "
            + "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertUserRow)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, recordId);
            stmt.setString(3, text);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addComment > SQLException > \n" + e.getMessage());
            throw new NoSuchElementException("Adding comment failed");
        }
    }

    public record Comment (
        int userId,
        int recordId,
        String text
    ) {}

    public ArrayList<Comment> getCommentsForRecord(int recordId) {
        ArrayList<Comment> comments = new ArrayList<Comment>();
        String query = "SELECT * FROM comments WHERE recordId = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, recordId);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    comments.add(new Comment(
                        results.getInt("userId"),
                        results.getInt("recordId"),
                        results.getString("text")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new NoSuchElementException("Error getting comments for the record");
        }
        return comments;
    }
}
