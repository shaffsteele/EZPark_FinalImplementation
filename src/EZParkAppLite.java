import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.List;

public class EZParkAppLite extends Application {

    private WebEngine webEngine;
    private TextField startZipField = new TextField();
    private TextField destZipField = new TextField();
    private ObservableList<String> eventList = FXCollections.observableArrayList();
    private ListView<String> eventListView = new ListView<>(eventList);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EZ Park - Event Manager and Route Finder");

        // Initialize the SQLite database and populate sample data
        EZParkDatabaseLite.initializeDatabase();
        EZParkDatabaseLite.populateSampleData();
        EZParkDatabaseLite.deleteDuplicateEvents();

        // WebView for the map
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        try {
            webEngine.load(getClass().getResource("map.html").toExternalForm());
        } catch (Exception e) {
            showError("Failed to load map.html: " + e.getMessage());
        }

        // Route and Event Controls
        startZipField.setPromptText("Enter Start Zip Code");
        destZipField.setPromptText("Enter Destination Zip Code");
        Button findRouteButton = new Button("Find Route");
        findRouteButton.setOnAction(e -> findRoute());

        // Organize Route and Event UI elements
        HBox routeBox = new HBox(10, startZipField, destZipField, findRouteButton);
        routeBox.setPadding(new Insets(10));

        VBox eventBox = new VBox(10, new Label("Available Events:"), eventListView);
        eventBox.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setTop(routeBox);
        layout.setLeft(eventBox);
        layout.setCenter(webView);

        Scene scene = new Scene(layout, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Fetch all events from the database
        fetchEventsFromDatabase();
    }

    private void fetchEventsFromDatabase() {
        new Thread(() -> {
            try {
                // Fetch all events from the SQLite database
                List<String> allEvents = EZParkDatabaseLite.fetchAllEvents();
                System.out.println("All events fetched: " + allEvents);

                // Update the ObservableList on the JavaFX Application Thread
                Platform.runLater(() -> {
                    eventList.clear();
                    eventList.addAll(allEvents); // Show all events in the list
                    System.out.println("Available events updated: " + eventList);
                });
            } catch (Exception e) {
                showError("Error fetching events: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void findRoute() {
        String startZip = startZipField.getText();
        String destZip = destZipField.getText();

        if (startZip.isEmpty() || destZip.isEmpty()) {
            showError("Please enter both start and destination zip codes.");
            return;
        }

        // Placeholder logic for route-finding
        Platform.runLater(() -> showInfo("Route from " + startZip + " to " + destZip + " would be displayed here."));
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
