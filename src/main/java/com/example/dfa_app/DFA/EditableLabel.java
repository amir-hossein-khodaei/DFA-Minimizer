package com.example.dfa_app.DFA;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;


public class EditableLabel extends Pane {

    private static final String DEFAULT_FONT_FAMILY = "Arial";
    private static final double DEFAULT_FONT_SIZE = 14;

    private final Label displayLabel = new Label("");
    private final TextField editor = new TextField("");


    public EditableLabel() {

        displayLabel.setFont(new Font(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE));
        editor.setFont(new Font(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE));
        editor.setVisible(false);
        displayLabel.setVisible(true);


        getChildren().addAll(displayLabel, editor);

        this.setPickOnBounds(false);


    }

    public TextField getEditor() {
        return editor;
    }


    public void setText(String text) {
        displayLabel.setText(text);
        editor.setText(text);
    }


    public String getText() {
        return editor.getText();
    }


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


    public void finalizeLabel() {
        displayLabel.setText(editor.getText());
        editor.setVisible(false);
        editor.setMouseTransparent(true);
        displayLabel.setVisible(true);
    }


    public void setLabelPosition(double x, double y) {
        displayLabel.setLayoutX(x);
        displayLabel.setLayoutY(y);
    }


    public void setEditorPosition(double x, double y) {
        editor.setLayoutX(x);
        editor.setLayoutY(y);
    }


    public double getLabelWidth() {
        return displayLabel.getBoundsInLocal().getWidth();
    }


    public double getLabelHeight() {
        return displayLabel.getBoundsInLocal().getHeight();
    }
}
