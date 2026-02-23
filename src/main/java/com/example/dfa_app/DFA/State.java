package com.example.dfa_app.DFA;

import com.example.dfa_app.Application_Controler;
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// - Represents a state node in a DFA with visual representation
// - Provides selection, editing, and transition management
// - Ensures unique naming and handles user interactions
public class State extends Group implements simularity {

    // - Static tracking for state selection and naming
    private static State selectedState = null;
    private static long idCounter = 0;
    public static final Set<String> stateNames = new HashSet<>();
    private static DFA dfaInstance; // Static reference to the DFA instance

    // - Method to set the DFA instance for all states
    public static void setDFAInstance(DFA dfa) {
        State.dfaInstance = dfa;
    }

    // - Method to clear all state names from the static set
    public static void clearAllStateNames() {
        // if (dfaInstance != null && dfaInstance.getControllerInstance() != null) {
        //     dfaInstance.getControllerInstance().log("Clearing all state names. Before clear: " + stateNames);
        // }
        stateNames.clear();
        // if (dfaInstance != null && dfaInstance.getControllerInstance() != null) {
        //     dfaInstance.getControllerInstance().log("After clear: " + stateNames);
        // }
    }

    // - Method to reset the static ID counter
    public static void resetIdCounter() {
        idCounter = 0;
    }

    // - Access currently selected state
    public static State getSelectedState() {
        return selectedState;
    }

    // - Set the currently selected state
    public static void setSelectedState(State state) {
        selectedState = state;
    }

    // - Instance fields for state properties and visuals
    private final long id;
    private String name;
    private boolean accepting = false; // Default to false
    private boolean isInitial = false; // Track if this is the initial state
    private final List<Transition> transitions = new ArrayList<>();
    private final Circle mainCircle;
    private Circle acceptingIndicator; // Visual for accepting state (double circle)
    private Polygon initialIndicator; // Visual for initial state (arrow)
    private final EditableLabel editableLabel;
    private boolean wasJustDragged = false; // Flag for drag state
    // - Fields for drag handling and event management
    private double dragDeltaX;
    private double dragDeltaY;
    private double offsetX;
    private double offsetY;
    private SelectionListener selectionListener;
    private Application_Controler controllerInstance; // Reference to the controller for table updates

    // - Create a state with basic properties
    public State(double centerX, double centerY, double radius, Color fillColor, Color strokeColor, double strokeWidth, Application_Controler controller) {
        this.id = idCounter++;
        setLayoutX(centerX);
        setLayoutY(centerY);
        this.controllerInstance = controller; // Store controller reference

        mainCircle = new Circle(0, 0, radius);
        mainCircle.setFill(fillColor);
        mainCircle.setStroke(strokeColor);
        mainCircle.setStrokeWidth(strokeWidth);
        mainCircle.setUserData(this);

        editableLabel = new EditableLabel();
        editableLabel.setMouseTransparent(true); 
        // NO initial positioning here - setLabelText handles it

        getChildren().add(mainCircle);
        createAcceptingIndicator();
        createInitialIndicator();
        getChildren().add(editableLabel); 

        setPickOnBounds(true);
        setFocusTraversable(true);

        this.name = ""; 
        setLabelText(this.name); // Set initial text & position
    }

    // Overloaded constructor for backward compatibility, uses default stroke
    public State(double centerX, double centerY, double radius, Color color, Application_Controler controller) {
        this(centerX, centerY, radius, color, Color.BLACK, 1.0, controller); // Default stroke
    }

    // - Create a state with name and basic properties
    // Overloaded constructor with name and all visual properties
    public State(double centerX, double centerY, double radius, Color fillColor, Color strokeColor, double strokeWidth, String name, Application_Controler controller) {
        this(centerX, centerY, radius, fillColor, strokeColor, strokeWidth, controller); // Call the main constructor
        setName(name); // Set the name using the validation logic
    }

    // Overloaded constructor with name, uses default stroke
    public State(double centerX, double centerY, double radius, Color color, String name, Application_Controler controller) {
        this(centerX, centerY, radius, color, Color.BLACK, 1.0, name, controller); // Default stroke
    }

    // - Register selection event listener
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // - Get state name
    public String getName() {
        return name;
    }

    // - Set state name with validation
    // - Rejects empty or duplicate names
    // - Notifies DFA instance of name change
    public void setName(String newName) {
        if (isNullOrEmpty(newName)) {
            // if (controllerInstance != null) {
            //     controllerInstance.log("[State] Attempted to set empty state name. Current names: " + stateNames);
            // }
            showAlert("Invalid State Name", "State name cannot be empty. Please choose a unique name.");
            Platform.runLater(editableLabel::startEditing);
            return;
        }

        String oldName = this.name; // Store old name before checking for equality

        if (newName.equals(oldName)) {
            return; // No actual change
        }

        if (stateNames.contains(newName)) {
            // if (controllerInstance != null) {
            //     controllerInstance.log("[State] Attempted to set duplicate state name: '" + newName + "'. Current names: " + stateNames);
            // }
            showAlert("Duplicate State Name", "A state with the name '" + newName + "' already exists. Please choose a unique name.");
            Platform.runLater(editableLabel::startEditing);
            return;
        }
        // - Remove old name and update
        if (!isNullOrEmpty(oldName)) {
            stateNames.remove(oldName);
            // if (controllerInstance != null) {
            //     controllerInstance.log("[State] Removed old state name: '" + oldName + "'. Current names after removal: " + stateNames);
            // }
        }
        this.name = newName;
        stateNames.add(newName);
        // if (controllerInstance != null) {
        //     controllerInstance.log("[State] Added new state name: '" + newName + "'. All current names: " + stateNames);
        // }
        setLabelText(newName);

        // - Notify DFA instance about the name change
        if (State.dfaInstance != null) {
            State.dfaInstance.stateNameChanged(this, oldName, newName);
        }
    }

    // - Check if state is an accepting state
    public boolean isAccepting() {
        return accepting;
    }

    // - Update accepting state status and visual indicator
    public void setAccepting(boolean accepting) {
        if (this.accepting == accepting) return; // No change
        this.accepting = accepting;
        updateAcceptingIndicatorVisuals();
        // DFA instance will handle notifying controller if needed
    }

    // - Check if state is the initial state
    public boolean isInitial() {
        return isInitial;
    }

    // - Update initial state status and visual indicator
    // - NOTE: This method ONLY updates the state's internal flag and visuals.
    // - The DFA class is responsible for ensuring only ONE state has isInitial=true.
    public void setAsInitial(boolean isInitial) {
        if (this.isInitial == isInitial) return; // No change
        this.isInitial = isInitial;
        updateInitialIndicatorVisuals();
        // DFA instance will handle notifying controller if needed
    }

    // - Create the visual indicator for accepting state (a double circle)
    private void createAcceptingIndicator() {
        acceptingIndicator = new Circle(0, 0, mainCircle.getRadius() + 4); // Slightly larger radius
        acceptingIndicator.setFill(Color.TRANSPARENT);
        acceptingIndicator.setStroke(Color.BLACK); // Use black for consistency
        acceptingIndicator.setStrokeWidth(mainCircle.getStrokeWidth()); // Match main circle stroke width
        acceptingIndicator.setMouseTransparent(true); // Ignore mouse events
        acceptingIndicator.setVisible(false); // Initially hidden
        getChildren().add(0, acceptingIndicator); // Add behind main circle
    }

    // - Update visibility of the accepting indicator
    private void updateAcceptingIndicatorVisuals() {
        Platform.runLater(() -> {
            if (acceptingIndicator != null) {
                 acceptingIndicator.setVisible(this.accepting);
            }
        });
    }

    // - Create the visual indicator for the initial state (an arrow)
    private void createInitialIndicator() {
        double radius = mainCircle.getRadius();
        double arrowSize = radius * 0.6; // Adjust size as needed
        double arrowX = -radius - arrowSize * 1.2; // Position left of the main circle
        double arrowY = 0; // Center vertically relative to the state center

        initialIndicator = new Polygon();
        // Define points for a simple triangle pointing right towards the state
        initialIndicator.getPoints().addAll(
            arrowX, arrowY,                     // Left point (base center)
            arrowX + arrowSize, arrowY - arrowSize / 2, // Top right point
            arrowX + arrowSize, arrowY + arrowSize / 2  // Bottom right point
        );
        initialIndicator.setFill(Color.DARKGREEN); // Use a distinct color
        initialIndicator.setMouseTransparent(true);
        initialIndicator.setVisible(false); // Initially hidden
        getChildren().add(initialIndicator); // Add to the group
    }

    // - Update visibility of the initial state indicator
    private void updateInitialIndicatorVisuals() {
        Platform.runLater(() -> {
            if (initialIndicator != null) {
                initialIndicator.setVisible(this.isInitial);
            }
        });
    }

    // - Remove a transition from this state
    public void removeTransition(Transition transition) {
        if (transition != null) {
            transitions.remove(transition);
            // Optionally remove transition visual from parent pane if needed
        }
    }

    // - Add a transition to this state
    public void addTransition(Transition transition) {
        if (transition != null && !transitions.contains(transition)) {
            transitions.add(transition);
        }
    }

    // - Remove transition by symbol and target state
    public void removeTransition(String symbol, State nextState) {
        transitions.removeIf(t -> t.getSymbol().equals(symbol) && Objects.equals(t.getNextState(), nextState));
        // Need to handle removing the visual transition element as well
    }

    // - Get all transitions (immutable list)
    public List<Transition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    // - Get transitions with specific symbol
    public List<Transition> getTransitions(String symbol) {
        return transitions.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .collect(Collectors.toList());
    }

    // - Get first transition with specific symbol
    public Transition getTransition(String symbol) {
        return transitions.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .findFirst().orElse(null);
    }

    // - Clears all transitions associated with this state (visual objects are still on pane)
    public void clearTransitions() {
        this.transitions.clear();
    }

    // - Removes transitions from this state that point to a specific target state
    // - Collects the removed visual transition objects into the provided list
    public void removeTransitionsToState(State targetState, List<Transition> collectedTransitions) {
        // Collect transitions that point to targetState and remove them from this state's list
        List<Transition> removed = new ArrayList<>();
        this.transitions.removeIf(t -> {
            boolean matches = Objects.equals(t.getNextState(), targetState);
            if (matches) {
                removed.add(t);
            }
            return matches;
        });
        collectedTransitions.addAll(removed);
    }

    // - Move state to new position
    public void moveState(double newX, double newY) {
        setLayoutX(newX);
        setLayoutY(newY);
        // Update outgoing/incoming transitions visuals?
    }

    // - Animate state movement with easing
    public void animateMoveState(double newX, double newY, double durationMillis) {
        Timeline timeline = new Timeline();
        KeyValue kvX = new KeyValue(layoutXProperty(), newX, Interpolator.EASE_BOTH);
        KeyValue kvY = new KeyValue(layoutYProperty(), newY, Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(durationMillis), kvX, kvY);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    // - Select this state (deselecting any previously selected state)
    public void select() {
        Platform.runLater(() -> {
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
                // System.err.println("Warning: Editor TextField not available immediately after startEditing in select()");
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
                    // Request focus on editor after a short delay
                    Platform.runLater(() -> {
                        if (editableLabel.getEditor() != null) {
                             editableLabel.getEditor().requestFocus();
                         }
                     });
                    e.consume();
                }
            });

            this.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                     double newX = e.getSceneX() - dragDeltaX;
                     double newY = e.getSceneY() - dragDeltaY;
                     moveState(newX, newY);
                     // Transitions should update automatically via listeners added in Transition constructor
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
        });
    }

    // - Deselect this state, validating name first
    public void deselect() {
        Platform.runLater(() -> {
            if (!finalizeName()) {
                keepInSelectionMode();
                return;
            }
            commitDeselection();
        });
    }

    // - Validate and save the current name
    private boolean finalizeName() {
        String proposedName = editableLabel.getText();
        if (isInvalidName(proposedName)) {
            showInvalidNameAlert(proposedName);
            return false;
        }
        // setName handles updating the model and notifying DFA
        setName(proposedName);
        editableLabel.finalizeLabel();
        // Remove Enter listener
        if(editableLabel.getEditor() != null) {
             editableLabel.getEditor().setOnAction(null);
         }
        return true;
    }

    // - Check if name is invalid
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

    // - Show alert for invalid name
    private void showInvalidNameAlert(String candidate) {
        if (isNullOrEmpty(candidate)) {
            showAlert("Invalid State Name", "State name cannot be empty. Please enter a valid name.");
        } else if (!candidate.equals(this.name) && stateNames.contains(candidate)) {
            showAlert("Duplicate State Name", "A state with the name '" + candidate + "' already exists. Please choose a unique name.");
        }
    }

    // - Prevent deselection and keep editing
    private void keepInSelectionMode() {
        // Ensure editing is restarted and editor is visible
        Platform.runLater(editableLabel::startEditing);
        mainCircle.setStroke(Color.BLUE); // Keep visual selection cue
    }

    // - Complete the deselection process
    private void commitDeselection() {
        mainCircle.setStroke(Color.BLACK);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), this);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.play();

        editableLabel.finalizeLabel(); // Make sure label is finalized visually

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
        
         // Ensure Enter key listener is removed
        if(editableLabel.getEditor() != null) {
             editableLabel.getEditor().setOnAction(null);
         }
    }

    // - Delete this state and its transitions
    public void deleteState() {
        // Deselect this state first to update UI before removal
        deselect(); // This will also finalize name and clear settings tab if selected

        // Notify DFA to handle removal from model and UI (including associated transitions)
        if (State.dfaInstance != null) {
            State.dfaInstance.removeState(this);
        }
        // UI removal (from pane) and static name set removal are now handled by DFA.removeState()
        // No need for direct pane.getChildren().remove(this) or stateNames.remove(name) here.
    }

    // - Update the label text and position
    private void setLabelText(String text) {
        editableLabel.setText(text);
        // Use Platform.runLater to ensure bounds are calculated after layout pass
        Platform.runLater(() -> {
            // Ensure label is temporarily visible for bounds calculation
            boolean wasVisible = editableLabel.isVisible();
            if (!wasVisible) editableLabel.setVisible(true);
            editableLabel.applyCss();
            editableLabel.layout();
            double labelWidth = editableLabel.getLabelWidth();
            double labelHeight = editableLabel.getLabelHeight();
            if (!wasVisible) editableLabel.setVisible(false);
            
            // Center the label within the state group (0,0)
            double labelX = -labelWidth / 2.0;
            double labelY = -labelHeight / 2.0;
            editableLabel.setLabelPosition(labelX, labelY); 
            editableLabel.setEditorPosition(labelX, labelY);
        });
    }

    // - Utility to check for null or empty string
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // - Display alert dialog with given title and content
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // - Override for object equality comparison based on unique ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State other = (State) o;
        return this.id == other.id;
    }

    // - Override for hash code calculation based on unique ID
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // - Get main circle for visual manipulation
    public Circle getMainCircle() {
        return mainCircle;
    }

    // - Get the circle shape representing this state
    public Circle getCircle() {
        return mainCircle;
    }

    // - Add transition directly without visual creation (used for model loading/rebuilding)
    public void addTransitionDirect(String symbol, State nextState) {
        // Check for duplicates before adding
        boolean exists = transitions.stream()
                                .anyMatch(t -> t.getSymbol().equals(symbol) && Objects.equals(t.getNextState(), nextState));
        if (!exists) {
            // Note: This direct addition does NOT create visual transition elements.
            // It's intended for scenarios like loading from a file or DFA minimization rebuild.
            // You might need a different approach if visual elements are required.
            Transition dummyTransition = new Transition(this, symbol, nextState); // Create a non-visual representation if needed
            this.transitions.add(dummyTransition);
        }
    }

    // - Alternative getter for name (legacy support, consider removing)
    public String getname() {
        return name;
    }

    // - Check if state is currently selected
    public boolean isSelected() {
        return selectedState == this;
    }

    // - Access the editable label for this state
    public EditableLabel getEditableLabel() {
        return editableLabel;
    }

    // - Check if state was recently dragged
    public boolean wasJustDragged() {
        return wasJustDragged;
    }
}
