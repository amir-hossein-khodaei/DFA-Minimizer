package com.example.dfa_app;

import com.example.dfa_app.DFA.DFA;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DFASerializer {

    public static void saveDFA(DFA dfa, Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save DFA as DOT File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DOT files (*.dot)", "*.dot"));
        File file = fileChooser.showSaveDialog(ownerWindow);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(dfa.toDotString());
            } catch (IOException e) {
                e.printStackTrace();
                // Optionally show an alert to the user about the error
            }
        }
    }
} 