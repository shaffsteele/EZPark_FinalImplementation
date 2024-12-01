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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class EZParkApp extends Application {

    private WebEngine webEngine;
    private TextField startZipField = new TextField();
    private TextField destZipField = new TextField();
    private ObservableList<String> eventList = FXCollections.observableArrayList();
    private UserEvents userEvents = new UserEvents();
    private ListView<String> eventListView = new ListView<>(eventList);
    private ListView<String> savedEventsListView = new ListView<>();
    private User currentUser;

    private static final String API_KEY = "5b3ce3597851110001cf624871a7d96a0c9144f49a2175ed9ac49b86";
    private static final String GEOCODING_API_URL = "https://api.openrouteservice.org/geocode/search";
    private static final String ROUTING_API_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EZ Park - Event Manager and Route Finder");

        // Initialize User
        currentUser = new User(14000);

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

        Button saveEventButton = new Button("Save Event");
        saveEventButton.setOnAction(e -> saveSelectedEvent());

        Button showSavedEventsButton = new Button("Show Saved Events");
        showSavedEventsButton.setOnAction(e -> showSavedEvents());

        // Event ListView Double-Click Listener
        eventListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                String selectedEvent = eventListView.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    String zipCode = getZipCodeForEvent(selectedEvent);
                    centerMapOnZipCode(zipCode);
                }
            }
        });

        savedEventsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                String selectedEvent = savedEventsListView.getSelectionModel().getSelectedItem();
                if (selectedEvent != null) {
                    String zipCode = getZipCodeForEvent(selectedEvent);
                    centerMapOnZipCode(zipCode);
                }
            }
        });

        // Layout Configuration
        HBox routeBox = new HBox(10, startZipField, destZipField, findRouteButton);
        routeBox.setPadding(new Insets(10));

        VBox eventBox = new VBox(10, new Label("Available Events:"), eventListView, saveEventButton, showSavedEventsButton);
        eventBox.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setTop(routeBox);
        layout.setLeft(eventBox);
        layout.setCenter(webView);

        Scene scene = new Scene(layout, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Fetch Events from Database
        fetchEventsFromDatabase();
    }

    private void fetchEventsFromDatabase() {
        new Thread(() -> {
            try {
                // Fetch events from the database
                List<String> events = EZParkDatabase.fetchAllEvents();

                // Ensure the event list is updated on the JavaFX Application Thread
                Platform.runLater(() -> {
                    eventList.clear(); // Clear existing events
                    eventList.addAll(events); // Add fetched events
                });
            } catch (Exception e) {
                // Show an error for other unexpected issues
                showError("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }


    private void centerMapOnZipCode(String zipCode) {
        if (zipCode == null || zipCode.isEmpty()) {
            showError("Invalid zip code for the selected event.");
            return;
        }
        new Thread(() -> {
            double[] coordinates = getCoordinates(zipCode);
            if (coordinates != null) {
                Platform.runLater(() -> webEngine.executeScript("map.setView([" + coordinates[0] + ", " + coordinates[1] + "], 13);"));
            } else {
                showError("Could not find location for the zip code: " + zipCode);
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

        new Thread(() -> {
            try {
                double[] startCoords = getCoordinates(startZip);
                double[] destCoords = getCoordinates(destZip);

                if (startCoords == null || destCoords == null) {
                    showError("Invalid start or destination zip codes.");
                    return;
                }

                getAndDisplayRoute(startCoords, destCoords);
            } catch (Exception e) {
                showError("Error finding route: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private double[] getCoordinates(String zipCode) {
        try {
            String url = GEOCODING_API_URL + "?api_key=" + API_KEY + "&text=" + zipCode;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray features = json.getJSONArray("features");
            if (features.length() > 0) {
                JSONArray coordinates = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                return new double[]{coordinates.getDouble(1), coordinates.getDouble(0)};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getAndDisplayRoute(double[] startCoords, double[] destCoords) {
        try {
            String url = ROUTING_API_URL + "?api_key=" + API_KEY +
                    "&start=" + startCoords[1] + "," + startCoords[0] +
                    "&end=" + destCoords[1] + "," + destCoords[0];
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray coordinates = json.getJSONArray("features")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            Platform.runLater(() -> {
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    webEngine.executeScript("L.polyline([[" + coord.getDouble(1) + ", " + coord.getDouble(0) + "]], {color: 'blue'}).addTo(map);");
                }
            });

        } catch (Exception e) {
            showError("Error displaying route: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getZipCodeForEvent(String event) {
        return event.replaceAll(".*\\((\\d{5})\\)", "$1");
    }

    private void saveSelectedEvent() {
        String selectedEvent = eventListView.getSelectionModel().getSelectedItem();
        if (selectedEvent != null) {
            userEvents.addEvent(selectedEvent);
            showInfo("Event saved successfully!");
        } else {
            showError("Please select an event to save.");
        }
    }

    private void showSavedEvents() {
        savedEventsListView.setItems(FXCollections.observableArrayList(userEvents.getSavedEvents()));
        Stage savedEventsStage = new Stage();
        savedEventsStage.setTitle("My Saved Events");
        BorderPane savedLayout = new BorderPane();
        savedLayout.setCenter(savedEventsListView);

        Scene savedScene = new Scene(savedLayout, 300, 400);
        savedEventsStage.setScene(savedScene);
        savedEventsStage.show();
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
