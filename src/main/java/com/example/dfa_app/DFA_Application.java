package com.example.dfa_app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class DFA_Application extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Find the FXML file for the main user interface.
            URL fxmlFileUrl = getClass().getResource("Main_DFA.fxml");
            if (fxmlFileUrl == null) {
                throw new IOException("Cannot find FXML file: 'Main_DFA.fxml'");
            }

            // Load the UI layout from the FXML file.
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlFileUrl);
            Parent rootPane = fxmlLoader.load();

            // Create a new scene with the loaded UI layout.
            Scene mainScene = new Scene(rootPane, 1280, 720);

            // Configure and display the main application window.
            primaryStage.setTitle("DFA Minimizer");
            primaryStage.setScene(mainScene);
            primaryStage.show();

        } catch (IOException ioException) {
            // This is a critical failure, so we print the stack trace for debugging.
            System.err.println("Fatal Error: Failed to initialize the application UI.");
            ioException.printStackTrace();

            // Show a user-friendly error dialog.
            String errorHeader = "Could not load application components.";
            String errorContent = ioException.getMessage();
            showErrorDialog("Initialization Error", errorHeader, errorContent);

            // Exit the application since it cannot start properly.
            Platform.exit();
        }
    }

    /**
     * Displays a simple error pop-up window.
     * Must be run on the JavaFX Application thread.
     */
    private void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert errorAlert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            errorAlert.setTitle(title);
            errorAlert.setHeaderText(header);
            errorAlert.setContentText(content);
            errorAlert.showAndWait();
        });
    }

    /**
     * The main entry point for the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}