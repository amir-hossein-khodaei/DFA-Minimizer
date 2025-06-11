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
    public void start(Stage stage) {
        try {
            
            
            URL fxmlResource = getClass().getResource("Main_DFA.fxml");
            if (fxmlResource == null) {
                throw new IOException("FXML resource 'Main_DFA.fxml' not found.");
            }
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlResource);
            Parent root = fxmlLoader.load();
            Application_Controler controller = fxmlLoader.getController();
            
            Scene scene = new Scene(root, 1280, 720);
            stage.setTitle("DFA Minimizer");
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            showErrorDialog("Initialization Error", "Failed to load application components.", ex.getMessage());
            Platform.exit();
        }
    }

    
    
    private void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    
    public static void main(String[] args) {
        launch(args);
    }
}
    
