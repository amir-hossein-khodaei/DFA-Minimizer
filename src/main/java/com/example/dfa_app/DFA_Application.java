package com.example.dfa_app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

// - Main application class for the DFA Minimizer
// - Initializes JavaFX components and handles application startup
public class DFA_Application extends Application {

    // Log level constants for improved clarity
    private static final String LOG_INFO = "[INFO] ";
    private static final String LOG_ERROR = "[ERROR] ";
    private static final String LOG_WARN = "[WARN] ";
    
    // Date formatter for log timestamps
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    
    // - Format a timestamp for the log message
    private String getTimestamp() {
        return dateFormat.format(new Date());
    }
    
    // - Log a message to the console with timestamp
    private void log(String level, String message) {
        String timestamp = getTimestamp();
        String logMessage = timestamp + " " + level + message;
        System.out.println(logMessage);
    }
    
    // - Log an error message with timestamp
    private void logError(String message) {
        String timestamp = getTimestamp();
        String logMessage = timestamp + " " + LOG_ERROR + message;
        System.err.println(logMessage);
    }

    @Override
    public void start(Stage stage) {
        log(LOG_INFO, "Starting DFA Minimizer application...");
        try {
            // - Load the main FXML interface file
            // - Configure and display the primary application window
            URL fxmlResource = getClass().getResource("Main_DFA.fxml");
            if (fxmlResource == null) {
                throw new IOException("FXML resource 'Main_DFA.fxml' not found.");
            }
            Parent root = FXMLLoader.load(fxmlResource);
            Scene scene = new Scene(root, 1280, 720);
            stage.setTitle("DFA Minimizer");
            stage.setScene(scene);
            stage.show();
            log(LOG_INFO, "Application started successfully");

        } catch (IOException ex) {
            logError("Failed to load application components: " + ex.getMessage());
            showErrorDialog("Initialization Error", "Failed to load application components.", ex.getMessage());
            Platform.exit();
        }
    }

    // - Display error dialog with provided details
    // - Uses Platform.runLater to ensure UI thread compatibility
    private void showErrorDialog(String title, String header, String content) {
        log(LOG_WARN, "Showing error dialog: " + title + " - " + content);
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // - Application entry point
    public static void main(String[] args) {
        launch(args);
    }
}
