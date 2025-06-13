package com.example.dfa_app;

import com.example.dfa_app.DFA.DFA;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// This class handles saving a DFA object to a file.
public class DFAI_O {

    /**
     * Opens a "Save" dialog and writes the DFA's string representation to a file.
     * The file format is a .dot file, used for graph visualization.
     */
    public static void saveDFA(DFA dfa, Window ownerWindow) {
        // Create a file chooser dialog window.
        FileChooser saveFileChooser = new FileChooser();
        saveFileChooser.setTitle("Save DFA as DOT File");

        // Set a filter to only show and save files with the .dot extension.
        FileChooser.ExtensionFilter dotFileFilter = new FileChooser.ExtensionFilter("DOT files (*.dot)", "*.dot");
        saveFileChooser.getExtensionFilters().add(dotFileFilter);

        // Open the dialog and wait for the user to select a file.
        File destinationFile = saveFileChooser.showSaveDialog(ownerWindow);

        // Proceed only if the user actually chose a file.
        if (destinationFile != null) {
            // Use a try-with-resources block to ensure the FileWriter is closed automatically.
            try (FileWriter fileWriter = new FileWriter(destinationFile)) {
                // Get the DFA's content in DOT format and write it to the file.
                String dfaDotString = dfa.toDotString();
                fileWriter.write(dfaDotString);
                
                // Optional: Print a success message for debugging.
                System.out.println("DFA successfully saved to: " + destinationFile.getAbsolutePath());

            } catch (IOException ioException) {
                // If an error occurs during writing, print the error details.
                System.err.println("Failed to write DFA to file: " + destinationFile.getAbsolutePath());
                ioException.printStackTrace();
            }
        } else {
            // This block runs if the user closes the dialog without selecting a file.
            System.out.println("Save operation was cancelled.");
        }
    }
}