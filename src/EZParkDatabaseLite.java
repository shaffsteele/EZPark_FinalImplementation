import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EZParkDatabaseLite {

    // SQLite database file path
    private static final String DB_URL = "jdbc:sqlite:events.db";

    // Connect to the SQLite database
    public static Connection connect() throws SQLException {
        System.out.println("Connecting to SQLite database...");
        return DriverManager.getConnection(DB_URL);
    }

    // Initialize the database and create the events table if it doesn't exist
    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS events (" +
                "event_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "event_name TEXT NOT NULL, " +
                "location TEXT NOT NULL)";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.execute();
            System.out.println("Database initialized and table ensured.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add sample data to the database
    public static void populateSampleData() {
        String insertSQL = "INSERT INTO events (event_name, location) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {

            // Sample data
            String[][] sampleEvents = {
                    {"Concert A", "12345"},
                    {"Festival B", "67890"},
                    {"Parade C", "54321"}
            };

            for (String[] event : sampleEvents) {
                stmt.setString(1, event[0]);
                stmt.setString(2, event[1]);
                stmt.addBatch();
            }

            stmt.executeBatch();
            System.out.println("Sample data inserted into database.");
        } catch (SQLException e) {
            System.err.println("Error populating sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deleteDuplicateEvents() {
        String deleteDuplicatesSQL = """
                DELETE FROM events
                WHERE event_id NOT IN (
                SELECT MIN(event_id)
                FROM events
                GROUP BY event_name, location
                )
                """;

        try (Connection conn = connect();
        PreparedStatement stmt = conn.prepareStatement(deleteDuplicatesSQL)) {
            int affectedRows = stmt.executeUpdate();
            System.out.println("Deleted duplicate rows. Rows affected: " + affectedRows);
        } catch (SQLException e) {
            System.err.println("Error deleting duplicate events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Fetch all events from the database
    public static List<String> fetchAllEvents() throws SQLException {
        List<String> events = new ArrayList<>();
        String query = "SELECT event_name, location FROM events";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String event = rs.getString("event_name") + " (" + rs.getString("location") + ")";
                events.add(event);
            }
            System.out.println("Fetched all events: " + events); // Debug log
        }
        return events;
    }
}
