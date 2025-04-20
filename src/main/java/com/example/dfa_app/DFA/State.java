package com.example.dfa_app.DFA;

import com.example.dfa_app.SelectionListener;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * State represents a node in a DFA.
 * It is selectable and editable via an in-place EditableLabel.
 * The state prevents deselection until a valid (non-empty and unique) name is entered.
 */
public class State extends Group implements simularity {

    // --- Static Fields ---
    private static State selectedState = null;
    private static long idCounter = 0;
    private static final Set<String> stateNames = new HashSet<>();

    // Static getter for the selected state
    public static State getSelectedState() {
        return selectedState;
    }

    // --- Instance Fields ---
    private final long id;
    private String name;
    private boolean accepting;
    private final List<Transition> transitions = new ArrayList<>();
    private final Circle mainCircle;
    private Circle acceptingIndicator;
    private final EditableLabel editableLabel;
    private boolean wasJustDragged = false; // Flag for drag state
    // Drag and event related fields.
    private double dragDeltaX;
    private double dragDeltaY;
    private double offsetX;
    private double offsetY;
    private SelectionListener selectionListener;

    // --- Constructors ---
    public State(double centerX, double centerY, double radius, Color color) {
        this.id = idCounter++;
        setLayoutX(centerX);
        setLayoutY(centerY);

        mainCircle = new Circle(0, 0, radius);
        mainCircle.setFill(color);
        mainCircle.setStroke(Color.BLACK);
        mainCircle.setUserData(this);

        editableLabel = new EditableLabel();
        editableLabel.setEditorPosition(radius, -1.5 * radius);

        // Add visual components to the Group.
        getChildren().addAll(mainCircle, editableLabel);
        setPickOnBounds(true);
        setFocusTraversable(true);

        this.accepting = false;
        // Start with an empty name.
        this.name = "";
    }

    public State(double centerX, double centerY, double radius, Color color, String name) {
        this(centerX, centerY, radius, color);
        setName(name);
    }

    // --- Selection Listener ---
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // --- Name Getters and Setters ---
    public String getName() {
        return name;
    }

    /**
     * Sets a new name if valid. If the proposed name is invalid (empty or not unique),
     * an alert is shown and editing is restarted.
     */
    public void setName(String newName) {
        if (isNullOrEmpty(newName)) {
            showAlert("Invalid State Name", "State name cannot be empty. Please choose a unique name.");
            Platform.runLater(editableLabel::startEditing);
            return;
        }
        if (newName.equals(this.name)) {
            return;
        }
        if (stateNames.contains(newName)) {
            showAlert("Duplicate State Name", "A state with the name '" + newName + "' already exists. Please choose a unique name.");
            Platform.runLater(editableLabel::startEditing);
            return;
        }
        // Remove old name and update.
        if (!isNullOrEmpty(this.name)) {
            stateNames.remove(this.name);
        }
        this.name = newName;
        stateNames.add(newName);
        setLabelText(newName);
    }

    // --- Accepting State Methods ---
    public boolean isAccepting() {
        return accepting;
    }

    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
        updateAcceptingIndicator();
    }

    // --- Transition Management ---
    public void removeTransition(Transition transition) {
        if (transition != null) {
            transitions.remove(transition);
        }
    }

    public void addTransition(Transition transition) {
        if (transition != null && !transitions.contains(transition)) {
            transitions.add(transition);
        }
    }

    public void removeTransition(String symbol, State nextState) {
        transitions.removeIf(t -> t.getSymbol().equals(symbol) && Objects.equals(t.getNextState(), nextState));
    }

    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    public List<Transition> getTransitions(String symbol) {
        return transitions.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .collect(Collectors.toList());
    }

    public Transition getTransition(String symbol) {
        return transitions.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .findFirst().orElse(null);
    }

    // --- Movement Methods ---
    public void moveState(double newX, double newY) {
        setLayoutX(newX);
        setLayoutY(newY);
    }

    public void animateMoveState(double newX, double newY, double durationMillis) {
        Timeline timeline = new Timeline();
        KeyValue kvX = new KeyValue(layoutXProperty(), newX, Interpolator.EASE_BOTH);
        KeyValue kvY = new KeyValue(layoutYProperty(), newY, Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(durationMillis), kvX, kvY);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    // --- Selection Methods ---
    public void select() {
        if (selectedState != null && selectedState != this) {
            selectedState.deselect();
        }
        selectedState = this;
        wasJustDragged = false; // Reset flag on selection
        mainCircle.setStroke(Color.BLUE);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), this);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);
        scaleUp.play();

        editableLabel.startEditing();

        if (editableLabel.getEditor() != null) {
            editableLabel.getEditor().setOnAction(event -> {
                this.deselect(); 
                event.consume();
            });
        } else {
             System.err.println("Warning: Editor TextField not available immediately after startEditing in select()");
        }

        if (selectionListener != null) {
            selectionListener.onSelected(this);
        }

        // --- Restore original press/drag/release handlers for movement & flag setting ---
        this.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                wasJustDragged = false; // Ensure flag is false when press starts
                dragDeltaX = e.getSceneX() - getLayoutX();
                dragDeltaY = e.getSceneY() - getLayoutY();
                requestFocus(); 
                editableLabel.getEditor().requestFocus(); 
                e.consume();
            }
        });

        this.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                 double newX = e.getSceneX() - dragDeltaX;
                 double newY = e.getSceneY() - dragDeltaY;
                 moveState(newX, newY);
                 e.consume();
            }
         });

        // Set flag on mouse release, reset it shortly after
        this.setOnMouseReleased(e -> {
             if (e.getButton() == MouseButton.PRIMARY) {
                 wasJustDragged = true;
                 // Reset the flag after the current event processing is done
                 Platform.runLater(() -> {
                     wasJustDragged = false;
                 });
                 e.consume(); 
             }
        });
    }

    /**
     * The deselection method will validate the current name (from the editable label).
     * If the name is invalid (empty, null, or duplicate) it shows an alert and keeps the state selected.
     */
    public void deselect() {
        if (!finalizeName()) {
            keepInSelectionMode();
            return;
        }
        commitDeselection();
    }

    // --- Name Finalization Helpers ---
    /**
     * Validates and commits the text from the editable label.
     * @return true if the name is valid and finalized, false otherwise.
     */
    private boolean finalizeName() {
        String proposedName = editableLabel.getText();
        if (isInvalidName(proposedName)) {
            showInvalidNameAlert(proposedName);
            return false;
        }
        updateName(proposedName);
        editableLabel.finalizeLabel();
        editableLabel.getEditor().setOnAction(null);
        return true;
    }

    /**
     * Checks if the name candidate is invalid.
     * A candidate is considered invalid if it is null, empty, or (if changed) already exists.
     */
    private boolean isInvalidName(String candidate) {
        if (isNullOrEmpty(candidate)) {
            return true;
        }
        // If the candidate is different from the current name, it must not already exist.
        if (!candidate.equals(this.name) && stateNames.contains(candidate)) {
            return true;
        }
        return false;
    }

    /**
     * Displays an alert corresponding to the detected name error.
     */
    private void showInvalidNameAlert(String candidate) {
        if (isNullOrEmpty(candidate)) {
            showAlert("Invalid State Name", "State name cannot be empty. Please enter a valid name.");
        } else if (!candidate.equals(this.name) && stateNames.contains(candidate)) {
            showAlert("Duplicate State Name", "A state with the name '" + candidate + "' already exists. Please choose a unique name.");
        }
    }

    /**
     * Updates the internal name and corresponding UI elements.
     */
    private void updateName(String newName) {
        // Only proceed if the name is actually different
        if (!newName.equals(this.name)) {
            String oldName = this.name; // Store old name for logging
            // Remove old name from global set if it existed
            if (!isNullOrEmpty(oldName)) {
                stateNames.remove(oldName);
            }
            // Set the new name
            this.name = newName;
            stateNames.add(newName);
            // Update the visual label
            setLabelText(newName);
            // Log the change
            System.out.println("State name changed from '" + (isNullOrEmpty(oldName) ? "<empty>" : oldName) + "' to '" + newName + "'");
        }
    }

    /**
     * Keeps the state in selection mode by restarting editing and maintaining the visual style.
     */
    private void keepInSelectionMode() {
        // Ensure editing is restarted and editor is visible
        Platform.runLater(editableLabel::startEditing);
        mainCircle.setStroke(Color.BLUE); // Keep visual selection cue
    }

    /**
     * Completes the deselection process, updating visual cues and clearing interaction handlers.
     */
    private void commitDeselection() {
        mainCircle.setStroke(Color.BLACK);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), this);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.play();

        editableLabel.finalizeLabel();

        if (selectedState == this) {
            selectedState = null;
        }
        if (selectionListener != null) {
            selectionListener.onDeselected(this);
        }
        // Remove interaction handlers
        this.setOnMousePressed(null);
        this.setOnMouseDragged(null); 
        this.setOnMouseReleased(null); // Make sure release handler is removed
        
        if(editableLabel.getEditor() != null) {
             editableLabel.getEditor().setOnAction(null);
         }
    }

    // --- Deletion ---
    public void deleteState() {
        if (!isNullOrEmpty(name)) {
            stateNames.remove(name);
        }
        transitions.clear();
        if (getParent() instanceof Group) {
            ((Group)getParent()).getChildren().remove(this);
        }
    }

    // --- Label Management ---
    private void setLabelText(String text) {
        editableLabel.setText(text);
        Platform.runLater(() -> {
            editableLabel.applyCss();
            editableLabel.layout();
            double labelWidth = editableLabel.getLabelWidth();
            double labelHeight = editableLabel.getLabelHeight();
            editableLabel.setLabelPosition(-labelWidth / 2, -labelHeight / 2);
        });
    }

    // --- Accepting Indicator ---
    private void updateAcceptingIndicator() {
        if (accepting) {
            if (acceptingIndicator == null) {
                acceptingIndicator = new Circle(0, 0, mainCircle.getRadius() + 4);
                acceptingIndicator.setFill(Color.TRANSPARENT);
                acceptingIndicator.setStroke(Color.GREEN);
                acceptingIndicator.getStrokeDashArray().addAll(4.0, 4.0);
                // Place the indicator behind the main circle.
                getChildren().add(0, acceptingIndicator);
            }
        } else if (acceptingIndicator != null) {
            getChildren().remove(acceptingIndicator);
            acceptingIndicator = null;
        }
    }

    // --- Utility Methods ---
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Equality Overrides ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State other = (State) o;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- Additional Getters and Methods ---
    public Circle getMainCircle() {
        return mainCircle;
    }

    public Circle getCircle() {
        return mainCircle;
    }

    public void addTransitionDirect(String symbol, State nextState) {
        // Implementation for adding a direct transition can be added here.
    }

    // In some cases you might need this alternative getter.
    public String getname() {
        return name;
    }

    public boolean isSelected() {
        return selectedState == this;
    }

    // Getter for the editable label
    public EditableLabel getEditableLabel() {
        return editableLabel;
    }

    // Getter for the temporary drag flag
    public boolean wasJustDragged() {
        return wasJustDragged;
    }
}
