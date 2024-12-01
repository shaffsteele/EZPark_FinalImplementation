import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EZParkDatabase {

    // Connect to the database
    public static Connection connect() throws Exception {
        String url = "jdbc:mysql://localhost:3306/ezparkevents";
        String user = "root";
        String password = "Stcape01022022!";
        System.out.println("Connecting to the database...");
        Connection conn = DriverManager.getConnection(url, user, password);
        System.out.println("Database connection established.");
        return conn;
    }

    public static List<String> fetchAllEvents() throws SQLException {
        List<String> events = new ArrayList<>();
        String query = "SELECT event_name, location FROM events";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String event = rs.getString("event_name") + " (" + rs.getString("location") + ")";
                events.add(event); // Add event to the list
            }
            System.out.println("Fetched all events: " + events); // Debug log
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return events; // Return the list of all events
    }

}
