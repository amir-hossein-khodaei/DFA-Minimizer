package com.example.dfa_app.DFA;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

// - Custom component that provides an editable text label
// - Switches between display (Label) and edit (TextField) modes
public class EditableLabel extends Pane {

    private static final String DEFAULT_FONT_FAMILY = "Arial";
    private static final double DEFAULT_FONT_SIZE = 14;

    private final Label displayLabel = new Label("");
    private final TextField editor = new TextField("");

    // - Initialize the component with default styling
    // - Set up display/edit controls and add to container
    public EditableLabel() {
        displayLabel.setFont(new Font(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE));
        editor.setFont(new Font(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE));
        editor.setVisible(false);
        displayLabel.setVisible(true);

        getChildren().addAll(displayLabel, editor);
        this.setPickOnBounds(false);
    }

    // - Access the editor component
    public TextField getEditor() {
        return editor;
    }

    // - Set text for both display and edit controls
    public void setText(String text) {
        displayLabel.setText(text);
        editor.setText(text);
    }

    // - Get the current text value
    public String getText() {
        return editor.getText();
    }

    // - Switch to edit mode
    // - Make editor visible and request focus
    public void startEditing() {
        editor.setText(displayLabel.getText());
        displayLabel.setVisible(false);
        editor.setVisible(true);
        editor.setMouseTransparent(false);
        
        Platform.runLater(() -> {
            editor.requestFocus();
            editor.selectAll();
        });
    }

    // - Apply changes and return to display mode
    public void finalizeLabel() {
        displayLabel.setText(editor.getText());
        editor.setVisible(false);
        editor.setMouseTransparent(true);
        displayLabel.setVisible(true);
    }

    // - Position the display label
    public void setLabelPosition(double x, double y) {
        displayLabel.setLayoutX(x);
        displayLabel.setLayoutY(y);
    }

    // - Position the editor field
    public void setEditorPosition(double x, double y) {
        editor.setLayoutX(x);
        editor.setLayoutY(y);
    }

    // - Get the display label's width
    public double getLabelWidth() {
        return displayLabel.getBoundsInLocal().getWidth();
    }

    // - Get the display label's height
    public double getLabelHeight() {
        return displayLabel.getBoundsInLocal().getHeight();
    }
}
