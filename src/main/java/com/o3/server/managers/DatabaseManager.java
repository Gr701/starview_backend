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

    private String createUsersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT, 
            login TEXT NOT NULL UNIQUE, 
            password TEXT NOT NULL, 
            email TEXT NOT NULL, 
            nickname TEXT NOT NULL UNIQUE
        )
    """;
    
    private String createRecordsTable = """
        CREATE TABLE IF NOT EXISTS records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,  
            recordIdentifier TEXT NOT NULL, 
            recordDescription TEXT, 
            recordPayload TEXT, 
            recordRightAscension TEXT,
            recordDeclination TEXT,
            recordTimeReceived TEXT NOT NULL,
            recordOwner TEXT,
            isObservatoryPresent INTEGER CHECK(isObservatoryPresent IN (0,1)),
            observatoryName TEXT,
            latitude DOUBLE,
            longitude DOUBLE,
            isWeatherPresent INTEGER CHECK(isWeatherPresent IN (0,1)),
            temperatureInKelvins DOUBLE,
            cloudinessPercentance DOUBLE,
            bagroundLightVolume DOUBLE,
            ownerUsername TEXT,
            updateReason TEXT,
            modified TEXT,
            viewCount INTEGER
        )
    """;

    private String createCommentsTable = """
        CREATE TABLE IF NOT EXISTS comments (
            id INTEGER PRIMARY KEY AUTOINCREMENT, 
            userId INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            recordId INTEGER NOT NULL REFERENCES records(id) ON DELETE CASCADE, 
            text TEXT NOT NULL
        )
    """;

    private String createRatingsTable = """
        CREATE TABLE IF NOT EXISTS ratings (
            userId INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            recordId INTEGER NOT NULL REFERENCES records(id) ON DELETE CASCADE,
            rating INTEGER NOT NULL,
            PRIMARY KEY (userId, recordId)
        )
    """;

    private String createCollectionsTable = """
        CREATE TABLE IF NOT EXISTS collections (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ownerId INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            name TEXT NOT NULL,
            description TEXT NOT NULL
        )
    """;

    private String createCollectionRecordsTable = """
        CREATE TABLE IF NOT EXISTS collection_records (
            collectionId INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
            recordId INTEGER NOT NULL REFERENCES records(id) ON DELETE CASCADE,
            PRIMARY KEY (collectionId, recordId)
        )
    """;

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private DatabaseManager() {
        secureRandom = new SecureRandom();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:"+"MessageDB");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.executeUpdate(createUsersTable);
                stmt.executeUpdate(createRecordsTable);
                stmt.executeUpdate(createCommentsTable);
                stmt.executeUpdate(createCollectionsTable);
                stmt.executeUpdate(createCollectionRecordsTable);
                stmt.executeUpdate(createRatingsTable);
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > constructor > SQLException " + e.getMessage());
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            System.out.println("DatabaseManager > close > Closing database connection");
            connection = null;
        }
    }

    //USERS

    private String securePassword(String password) {
        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;
        return Crypt.crypt(password, salt);
    }

    private User getUserFromResults(ResultSet results) throws SQLException {
        return new User(
            results.getInt("id"),
            results.getString("login"),
            results.getString("password"),
            results.getString("email"),
            results.getString("nickname")
        );
    }

    public void addUser(User user) {
        String insertUserRow = "INSERT INTO users (login, password, email, nickname) "
            + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertUserRow)) {
            stmt.setString(1, user.getLogin());
            stmt.setString(2, securePassword(user.getPassword()));
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
                    return getUserFromResults(results);
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
                    return getUserFromResults(results);
                }
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getUser > SQLException > " + e.getMessage());
        }
        return null;
    }

    //RECORDS

    private String insertRecordRow = """
        INSERT INTO records (
            recordIdentifier, recordDescription, recordPayload, recordRightAscension,
            recordDeclination, recordTimeReceived, recordOwner, 
            isObservatoryPresent, observatoryName, latitude, longitude, 
            isWeatherPresent, temperatureInKelvins, cloudinessPercentance, bagroundLightVolume, 
            ownerUsername, updateReason, modified,
            viewCount
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private String updateRecordRow = """
        UPDATE records SET 
            recordIdentifier = ?, recordDescription = ?, recordPayload = ?, recordRightAscension = ?,
            recordDeclination = ?, recordTimeReceived = ?, recordOwner = ?,
            isObservatoryPresent = ?, observatoryName = ?, latitude = ?, longitude = ?,
            isWeatherPresent = ?, temperatureInKelvins = ?, cloudinessPercentance = ?, 
            bagroundLightVolume = ?,
            ownerUsername = ?, updateReason = ?, modified = ?,
            viewCount = ?
        WHERE id = ?
    """;

    private void fillStatementWithRecord(PreparedStatement stmt, ObservationRecord record) throws SQLException {
        stmt.setString(1, record.getIdentifier());
        stmt.setString(2, record.getDescription());
        stmt.setString(3, record.getPayload());
        stmt.setString(4, record.getRightAscension());
        stmt.setString(5, record.getDeclination());
        stmt.setString(6, record.getTimeReceived());
        stmt.setString(7, record.getOwner());
        stmt.setBoolean(8, record.getIsObservatoryPresent());
        stmt.setString(9, record.getObservatoryName());
        stmt.setDouble(10, record.getLatitude());
        stmt.setDouble(11, record.getLongitude());
        stmt.setBoolean(12, record.getIsWeatherPresent());
        stmt.setDouble(13, record.getTemperatureInKelvins());
        stmt.setDouble(14, record.getCloudinessPercentance());
        stmt.setDouble(15, record.getBagroundLightVolume());
        stmt.setString(16, record.getOwnerUsername());
        stmt.setString(17, record.getUpdateReason());
        stmt.setString(18, record.getModified());
        stmt.setInt(19, record.getViewCount());
    }

    private ObservationRecord getRecordFromResults(ResultSet results) throws SQLException {
        return new ObservationRecord(
            results.getInt("id"),
            results.getString("recordIdentifier"),
            results.getString("recordDescription"),
            results.getString("recordPayload"),
            results.getString("recordRightAscension"),
            results.getString("recordDeclination"),
            results.getString("recordTimeReceived"),
            results.getString("recordOwner"),
            results.getBoolean("isObservatoryPresent"),
            results.getString("observatoryName"),
            results.getDouble("latitude"),
            results.getDouble("longitude"),
            results.getBoolean("isWeatherPresent"),
            results.getDouble("temperatureInKelvins"),
            results.getDouble("cloudinessPercentance"),
            results.getDouble("bagroundLightVolume"),
            results.getString("ownerUsername"),
            results.getString("updateReason"),
            results.getString("modified"),
            results.getInt("viewCount")
        );
    }
    
    public void addRecord(ObservationRecord record) {
        try (PreparedStatement stmt = connection.prepareStatement(insertRecordRow)) {
            fillStatementWithRecord(stmt, record);
            stmt.executeUpdate(); //check if > 0
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addRecord > SQLException");
        }
    }

    public void updateRecord(ObservationRecord record) {
        try (PreparedStatement stmt = connection.prepareStatement(updateRecordRow)) {
            fillStatementWithRecord(stmt, record);
            stmt.setInt(20, record.getId());
            stmt.executeUpdate(); //check if > 0
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
                records.add(getRecordFromResults(results));
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
                    return getRecordFromResults(results);               
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
                    records.add(getRecordFromResults(results));
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

    public ArrayList<ObservationRecord> getMostViewedRecords() {
        ArrayList<ObservationRecord> records = new ArrayList<ObservationRecord>();
        String query = "SELECT * FROM records ORDER BY viewCount DESC LIMIT 5";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    records.add(getRecordFromResults(results));
                }   
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getMostViewedRecords > SQLException \n" + e.getMessage());
        }
        return records;
    }

    String activeQuery = """
        SELECT 
            r.*,
            COALESCE(c.comment_count, 0) +
            COALESCE(rt.rating_count, 0) AS activity
        FROM records r

        LEFT JOIN (
            SELECT recordId, COUNT(*) AS comment_count
            FROM comments
            GROUP BY recordId
        ) c on r.id = c.recordId

        LEFT JOIN (
            SELECT recordId, COUNT(*) AS rating_count
            FROM ratings
            GROUP BY recordId
        ) rt on r.id = rt.recordID

        ORDER BY activity DESC LIMIT 5
    """;

    public ArrayList<ObservationRecord> getMostActiveRecords() {
        ArrayList<ObservationRecord> records = new ArrayList<ObservationRecord>();
        try (PreparedStatement stmt = connection.prepareStatement(activeQuery)) {
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    records.add(getRecordFromResults(results));
                }   
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getMostViewedRecords > SQLException \n" + e.getMessage());
        }
        return records;
    }

    
    //COMMENTS
    
    public void addComment(int userId, int recordId, String text) {
        String insertCommentRow = "INSERT INTO comments (userId, recordId, text) "
            + "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertCommentRow)) {
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

    //RATINGS
    
    public void updateRating(int userId, int recordId, int rating) {
        String query = """
            INSERT INTO ratings (userId, recordId, rating)
            VALUES (?, ?, ?)
            ON CONFLICT (userId, recordId)
            DO UPDATE SET rating = excluded.rating
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, recordId);
            stmt.setInt(3, rating);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DatabaseManager > updateRating > SQLException > \n" + e.getMessage());
            throw new NoSuchElementException("Updating rating failed");
        }
    }

    public record RecordRating (
        double value,
        int count
    ) {} 

    public RecordRating getRating(int recordId) {
        double value = 0.0;
        int count = 0;
        try (PreparedStatement stmt 
            = connection.prepareStatement("SELECT COUNT(*) FROM ratings WHERE recordId = ?"))
        {
            stmt.setInt(1, recordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRating > SQLException > \n" + e.getMessage());
        }

        try (PreparedStatement stmt2 
            = connection.prepareStatement("SELECT AVG(rating) FROM ratings WHERE recordId = ?"))
        {
            stmt2.setInt(1, recordId);
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                value = rs2.getDouble(1);
            }
        } catch (SQLException e) {
            System.out.println("DatabaseManager > getRating > SQLException > \n" + e.getMessage());
        }

        return new RecordRating(value, count);
    }

    //COLLECTIONS
    
    public void addCollection(int ownerId, String name, String description) {
        String insertCollectionRow = "INSERT INTO collections (ownerId, name, description) "
            + "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertCollectionRow)) {
            stmt.setInt(1, ownerId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.executeUpdate(); //check > 0
        } catch (SQLException e) {
            System.out.println("DatabaseManager > addCollection > SQLException > \n" + e.getMessage());
        }
    }

    public void addRecordToCollection(int recordId, int collectionId) {
        String insertCollectionRecordRow = "INSERT INTO collection_records (collectionId, recordId) "
            + "VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertCollectionRecordRow)) {
            stmt.setInt(1, collectionId);
            stmt.setInt(2, recordId);
            stmt.executeUpdate(); //check > 0
        } catch (SQLException e) {
            //System.out.println("DatabaseManager > addRecordToCollection > SQLException > \n" + e.getMessage());
            throw new NoSuchElementException("No such record or collection");
        }
    }
    
    public record Collection (
        int id,
        int ownerId,
        String name,
        String description,
        ArrayList<Integer> recordIds
    ) {}

    public Collection getCollection(int collectionId) {
        
        //get records ids in the collection
        ArrayList<Integer> recordIds = new ArrayList<Integer>();
        String recordsQuery = "SELECT recordId FROM collection_records WHERE collectionId = ?";
        try (PreparedStatement stmt = connection.prepareStatement(recordsQuery)) {
            stmt.setInt(1, collectionId);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    recordIds.add(results.getInt("recordId"));
                }
            }
        } catch (SQLException e) {
            throw new NoSuchElementException("Error getting recordIds for the collection");
        }

        //get collection info 
        String collectionQuery = "SELECT * FROM collections WHERE id = ?";       
        try (PreparedStatement stmt = connection.prepareStatement(collectionQuery)) {
            stmt.setInt(1, collectionId);
            try (ResultSet results = stmt.executeQuery()) {
                if (results.next()) {
                    return new Collection(
                        results.getInt("id"),
                        results.getInt("ownerId"),
                        results.getString("name"),
                        results.getString("description"),
                        recordIds
                    );
                } else {
                    throw new NoSuchElementException("No collection found");
                }
            }
        } catch (SQLException e) {
            throw new NoSuchElementException("Error getting collection info");
        }
    }

    public ArrayList<Collection> getUserCollections(int userId) {
        ArrayList<Collection> collections = new ArrayList<Collection>();
        String collectionsQuery = "SELECT id FROM collections WHERE ownerId = ?";
        try (PreparedStatement stmt = connection.prepareStatement(collectionsQuery)) {
            stmt.setInt(1, userId);
            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    collections.add(getCollection(results.getInt("id")));
                }
                return collections;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            throw new NoSuchElementException("Error getting collections for the user");
        }
    }
}
