package com.example.dfa_app;

import com.example.dfa_app.DFA.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.event.EventHandler;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert.AlertType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

// Imports needed for TableView update
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.stream.Collectors;
import java.util.Objects;

// - Main controller for the DFA application
// - Manages UI interactions, DFA model, and element creation/editing
public class Application_Controler implements SelectionListener {
    // Log level constants for improved clarity
    private static final String LOG_INFO = "[INFO] ";
    private static final String LOG_DEBUG = "[DEBUG] ";
    private static final String LOG_WARN = "[WARN] ";
    private static final String LOG_ERROR = "[ERROR] ";
    private static final String LOG_SUCCESS = "[SUCCESS] ";
    
    // Date formatter for log timestamps
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    
    @FXML
    private TabPane TabPane;
    @FXML
    private Tab transitionSettingsTab, stateSettingsTab;
    @FXML
    private BorderPane BorderPane;
    @FXML
    private TextField stateNameTextField;
    @FXML
    private CheckBox startStateCheck, acceptingStateCheck;
    @FXML
    private ComboBox<String> fromStateCombo, toStateCombo, transitionNameCombo;
    @FXML
    private TableView<State> dfaTransitionTable;
    @FXML
    private TableColumn<State, String> stateColumn;
    @FXML
    private TableColumn<State, String> transitionsParentColumn;
    @FXML
    private Pane pane;
    @FXML
    private Button startProcessButton;
    @FXML
    private TextArea logTextArea;
    @FXML
    private Button clearLogButton;
    @FXML
    private Button openButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button newPageButton;
    @FXML
    private Button newStateButton;
    @FXML
    private Button newTransitionButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button redoButton;

    // - References to model and current interaction state
    private DFA dfa;
    private Transition currentTransition;
    private boolean waitingForSecondClick = false;
    private boolean isPlacingNewState = false; // Flag for new state placement

    // - Format a timestamp for the log message
    private String getTimestamp() {
        return dateFormat.format(new Date());
    }
    
    // - Log a message to the console with timestamp
    private void log(String level, String message) {
        String timestamp = getTimestamp();
        String logMessage = timestamp + " " + level + message;
        System.out.println(logMessage);
        // Removed writing to logTextArea
    }
    
    // - Log an error message with timestamp
    private void logError(String message) {
        String timestamp = getTimestamp();
        String logMessage = timestamp + " " + LOG_ERROR + message;
        System.err.println(logMessage);
        // Removed writing to logTextArea
    }
    
    // - Clear the log text area (no longer used)
    private void clearLog() {
        // Method kept for compatibility but no longer does anything
        log(LOG_INFO, "Clear log requested");
    }
    
    // - Clear log button handler (no longer used)
    @FXML
    private void handleClearLogButton() {
        // Method kept for compatibility but no longer does anything
    }

    // - Initialize application components and event handlers
    @FXML
    public void initialize() {
        log(LOG_INFO, "Initializing Application Controller...");
        
        // Set up clear log button if available (no longer does anything)
        if (clearLogButton != null) {
            clearLogButton.setOnAction(event -> {/* No operation */});
        }
        
        // - Create the DFA model instance
        log(LOG_INFO, "Creating new DFA model instance.");
        dfa = new DFA(this);

        // - Configure the transition table
        log(LOG_DEBUG, "Setting up transition table structure.");
        setupTransitionTable();
        // - Initialize the transition table with current data
        log(LOG_DEBUG, "Populating initial transition table.");
        updateTransitionTable();

        final AtomicReference<EventHandler<MouseEvent>> transitionMouseMoveHandlerRef = new AtomicReference<>();

        // - Main event handler for clicks on the drawing pane
        pane.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            // - Skip if currently in state creation mode
            if (isPlacingNewState) {
                log(LOG_DEBUG, "Click ignored: Currently placing new state.");
                return;
            }

            Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
            String targetInfo = (clickedNode == null) ? "Pane" : clickedNode.getClass().getSimpleName();
            if (clickedNode != null && clickedNode.getUserData() != null) {
                targetInfo += " (userData: " + clickedNode.getUserData().getClass().getSimpleName() + ")";
            }
            log(LOG_DEBUG, "MOUSE_CLICKED event on: " + targetInfo);
            
            State currentlySelected = State.getSelectedState();

            // - Log click target for debugging
            String clickedInfo = "Clicked on: " + (clickedNode == null ? "null" : clickedNode.getClass().getSimpleName());
            if (clickedNode != null && clickedNode.getUserData() != null) {
                 clickedInfo += " (userData: " + clickedNode.getUserData().getClass().getSimpleName() + ")";
            }

            // - Handle transition completion (second click of transition creation)
            if (waitingForSecondClick) {
                log(LOG_DEBUG, "Handling transition completion (second click)...");

                pane.setOnMouseMoved(null); // - Stop arrow following mouse
                transitionMouseMoveHandlerRef.set(null);

                // - Ignore clicks on empty space during transition completion
                if (clickedNode == pane) {
                    log(LOG_DEBUG, "Transition completion ignored: Clicked on Pane.");
                    mouseEvent.consume();
                    return;
                }

                // - Find target state by traversing node hierarchy
                log(LOG_DEBUG, "Finding target state for transition completion...");
                State targetState = null;
                Node current = clickedNode;
                while (current != null && !(current instanceof State)) {
                     current = current.getParent();
                }
                if (current instanceof State) {
                     targetState = (State) current;
                     log(LOG_DEBUG, "Target state found: " + targetState.getName());
                } else {
                     log(LOG_WARN, "Target state not found for transition completion.");
                }

                // - Complete the transition if both source and target states exist
                if (currentTransition != null && targetState != null) {
                    // - Finalize the transition connection
                    log(LOG_INFO, "Completing transition from '" + currentTransition.getFromState().getName() + "' to '" + targetState.getName() + "'.");
                    currentTransition.completeTransition(targetState);
                    currentTransition.getFromState().addTransition(currentTransition);

                    // - Update the DFA model
                    log(LOG_DEBUG, "Ensuring transition states ('" + currentTransition.getFromState().getName() + "', '" + targetState.getName() + "') are known to the DFA model.");
                    dfa.addState(currentTransition.getFromState());
                    dfa.addState(targetState);

                    updateTransitionTable(); // - Update table after adding transition

                } else {
                     log(LOG_WARN, "Transition cancelled: Target state not resolved or no transition pending.");
                    if (currentTransition != null) {
                        log(LOG_DEBUG, "Removing incomplete transition graphics.");
                        pane.getChildren().remove(currentTransition);
                    }
                }

                // - Reset transition creation state
                log(LOG_DEBUG, "Resetting transition completion state.");
                waitingForSecondClick = false;
                currentTransition = null;
                mouseEvent.consume();
                return;
            }
            
            // NEW: 2. Handle Transition Selection (Right-Click on Arrow)
            if (!waitingForSecondClick && mouseEvent.getButton() == MouseButton.SECONDARY) {
                log(LOG_DEBUG, "Handling transition selection (Right-Click)...");
                Object source = mouseEvent.getTarget();

                // Check if the click was on a component of a CurvedArrow
                // and try to get the Transition object from its user data.
                Transition clickedTransition = null;
                if (source instanceof Node) {
                    Node clickedNodeInScene = (Node) source;
                    // Walk up the parent hierarchy to find the CurvedArrow or Transition group
                    Node parent = clickedNodeInScene;
                    while (parent != null && !(parent.getUserData() instanceof Transition)) {
                        parent = parent.getParent();
                    }
                    if (parent != null && parent.getUserData() instanceof Transition) {
                        clickedTransition = (Transition) parent.getUserData();
                        log(LOG_DEBUG, "Found transition via UserData: " + clickedTransition);
                    }
                }

                if (clickedTransition != null) {
                    log(LOG_INFO, "Selecting transition: " + clickedTransition);
                    clickedTransition.select(); // Call the transition's select method
                    mouseEvent.consume();
                    return; // Finished handling transition selection
                }
            }

            // OLD: 2. Handle Transition Initiation (Ctrl+Click on state) - CHECK BEFORE GENERAL STATE CLICK
            // RENUMBERED to 3
            if (!waitingForSecondClick && mouseEvent.isControlDown() && mouseEvent.getButton() == MouseButton.PRIMARY &&
                clickedNode != null && clickedNode.getUserData() instanceof State) {
                 log(LOG_INFO, "Initiating new transition...");
                State fromState = (State) clickedNode.getUserData();
                log(LOG_DEBUG, "Transition source state: " + fromState.getName());
                // Pass DFA and Controller references to Transition constructor
                log(LOG_DEBUG, "Creating new Transition object, passing current DFA instance.");
                currentTransition = new Transition(fromState, this.dfa, this);
                transitionMouseMoveHandlerRef.set(moveEvent -> {
                     if (currentTransition != null) {
                         currentTransition.setTempEnd(moveEvent.getX(), moveEvent.getY());
                     }
                     moveEvent.consume();
                });
                pane.setOnMouseMoved(transitionMouseMoveHandlerRef.get());
                waitingForSecondClick = true;
                log(LOG_DEBUG, "Waiting for second click to complete transition.");
                mouseEvent.consume();
                return; // Finished handling initiation
            }

            // OLD: 3. Handle State Selection/Deselection (No Ctrl, Click on state)
            // RENUMBERED to 4
            if (clickedNode != null && clickedNode.getUserData() instanceof State) {
                 log(LOG_DEBUG, "Handling state selection/deselection...");
                 State state = (State) clickedNode.getUserData();
                 // Keep right-click selection for states too?
                 if (mouseEvent.getButton() == MouseButton.SECONDARY || mouseEvent.getButton() == MouseButton.PRIMARY) {
                    if (currentlySelected == state && !currentlySelected.wasJustDragged() && mouseEvent.getButton() == MouseButton.PRIMARY) {
                        log(LOG_INFO, "Deselecting state: " + currentlySelected.getName());
                        currentlySelected.deselect();
                    } else {
                        log(LOG_INFO, "Selecting state: " + state.getName());
                        state.select();
                    }
                 }
                 mouseEvent.consume();
                 return; // Finished handling state click
            }

            // OLD: 4. Handle Pane Click Deselection (if none of the above)
            // RENUMBERED to 5
            if (!mouseEvent.isConsumed() && clickedNode == pane) {
                 log(LOG_DEBUG, "Handling pane click (deselect)...");
                 // Deselect currently selected State OR Transition
                if (currentlySelected != null && !currentlySelected.wasJustDragged()) {
                    log(LOG_INFO, "Deselecting state (pane click): " + currentlySelected.getName());
                    currentlySelected.deselect();
                } else if (Transition.getSelectedTransition() != null) {
                     // Add check for dragging if needed for transitions too
                     log(LOG_INFO, "Deselecting transition (pane click): " + Transition.getSelectedTransition());
                     Transition.getSelectedTransition().deselect();
                }
                mouseEvent.consume();
                 return; // Finished handling pane click
            }

            // OLD: 5. Fall through - Log if click wasn't handled
            // RENUMBERED to 6
             if (!mouseEvent.isConsumed()) {
                 log(LOG_WARN, "Click event on " + targetInfo + " was not consumed by any specific logic.");
             }

        });
         // End of MOUSE_CLICKED filter

        // Button action for minimization process
        startProcessButton.setOnAction(actionEvent -> {
            log(LOG_INFO, "Starting DFA minimization process...");
            log(LOG_DEBUG, "Building DFA representation from pane elements.");
            buildDFAFromPane();
            log(LOG_DEBUG, "Calling dfa.removeUnreachableStates() on DFA model.");
            dfa.removeUnreachableStates();
            log(LOG_DEBUG, "Calling dfa.minimizeDFA() on DFA model.");
            dfa.minimizeDFA();
            log(LOG_DEBUG, "Calling dfa.printMinimizedDFA() (prints to console).");
            dfa.printMinimizedDFA();
            log(LOG_SUCCESS, "DFA minimization process complete.");
        });

        // Global key handlers
        BorderPane.setOnKeyPressed(event -> {
            // Temporary listeners need to be declared outside the handlers to be removable using array holders
            // Use AtomicReference for handlers to allow modification within lambda expressions
            final AtomicReference<EventHandler<MouseEvent>> firstClickHandlerRef = new AtomicReference<>();
            final AtomicReference<EventHandler<MouseEvent>> mouseMoveHandlerRef = new AtomicReference<>();
            final AtomicReference<EventHandler<ActionEvent>> enterPlacementHandlerRef = new AtomicReference<>();
            final AtomicReference<EventHandler<MouseEvent>> secondClickHandlerRef = new AtomicReference<>();
            final AtomicReference<EventHandler<ActionEvent>> secondEnterHandlerRef = new AtomicReference<>();

            switch (event.getCode()) {
                case D:
                    if (event.isControlDown()) {
                        Transition selectedTransition = Transition.getSelectedTransition();
                        State selectedState = State.getSelectedState();

                        if (selectedTransition != null) {
                            log(LOG_INFO, "Deleting selected transition: " + selectedTransition);
                            deleteTransition(selectedTransition); // Call helper method instead
                            log(LOG_DEBUG, "Transition deletion requested.");
                        } else if (selectedState != null) {
                             log(LOG_INFO, "Deleting selected state: " + selectedState.getName());
                             selectedState.deleteState(); // Assume this handles removal from pane and DFA updates
                        } else {
                             log(LOG_WARN, "Delete key pressed, but nothing selected (no state or transition).");
                        }
                        event.consume();
                    }
                    break;
                case Z: // Undo (Ctrl+Z)
                    if (event.isControlDown()) {
                        log(LOG_INFO, "Ctrl+Z pressed (Undo - Not Implemented).");
                        event.consume();
                    }
                    break;
                case Y: // Redo (Ctrl+Y)
                    if (event.isControlDown()) {
                        log(LOG_INFO, "Ctrl+Y pressed (Redo - Not Implemented).");
                        event.consume();
                    }
                    break;
                case S: // Save (Ctrl+S)
                    if (event.isControlDown()) {
                        log(LOG_INFO, "Ctrl+S pressed (Save - Not Implemented).");
                        event.consume();
                    }
                    break;
                case O: // Open (Ctrl+O)
                    if (event.isControlDown()) {
                        log(LOG_INFO, "Ctrl+O pressed (Open - Not Implemented).");
                        event.consume();
                    }
                    break;
                case R: // Run Minimization (Ctrl+R)
                    if (event.isControlDown()) {
                        log(LOG_INFO, "Ctrl+R pressed. Triggering minimization via button fire.");
                        startProcessButton.fire(); // Simulate button click
                        event.consume();
                    }
                    break;
                case K: // Clear Pane (Ctrl+K)
                    if (event.isControlDown()) {
                         log(LOG_INFO, "Ctrl+K pressed (Clear - Not Implemented).");
                         // Needs logic to remove all states and transitions from pane and DFA
                        event.consume();
                    }
                    break;
                case N: // New State (Ctrl+N)
                    if (event.isControlDown()) {
                        // Prevent starting another placement if one is in progress
                        if (isPlacingNewState) {
                            log(LOG_WARN, "Ctrl+N ignored: Already placing a new state.");
                            return;
                        }

                        log(LOG_INFO, "Ctrl+N pressed: Starting new state placement.");
                        isPlacingNewState = true; // Set flag

                        State newState = new State(-50, -50, 30, Color.WHITE, this);
                        newState.setSelectionListener(this);
                        pane.getChildren().add(newState);
                        newState.select(); // Select immediately
                        log(LOG_DEBUG, "New state object created and added to pane.");

                        // Make state follow mouse
                        log(LOG_DEBUG, "Attaching mouse move listener for state placement.");
                        mouseMoveHandlerRef.set(moveEvent -> {
                            newState.moveState(moveEvent.getX(), moveEvent.getY());
                            moveEvent.consume();
                        });
                        pane.setOnMouseMoved(mouseMoveHandlerRef.get());

                        // Define the common placement/finalization logic
                        final AtomicReference<Runnable> placementLogicRef = new AtomicReference<>();
                        placementLogicRef.set(() -> {
                            log(LOG_DEBUG, "Finalizing state placement for: " + newState.getName());
                            pane.setOnMouseMoved(null);
                            pane.setOnMouseClicked(null);
                            if (newState.getEditableLabel().getEditor() != null) {
                                newState.getEditableLabel().getEditor().setOnAction(null);
                            }

                            log(LOG_DEBUG, "Checking for state overlap.");
                            if (isOverlapping(newState)) {
                                logError("State placement failed: Overlap detected.");
                                showOverlapError();
                                log(LOG_DEBUG, "Removing overlapping state graphics.");
                                pane.getChildren().remove(newState);
                                isPlacingNewState = false; // Reset flag on failure
                                return;
                            }

                            log(LOG_DEBUG, "Attempting to deselect new state after placement.");
                            newState.deselect(); // finalizeLabel logic is triggered here

                            // Check if deselection worked (State.select calls listener AFTER changing selection)
                            if (State.getSelectedState() == newState) {
                                // Deselect failed (likely because editor was still active and rejected name?)
                                log(LOG_WARN, "State deselection failed after placement (likely pending name edit). Setting up secondary listeners.");
                                // Deselect failed, setup second attempt listeners
                                secondEnterHandlerRef.set(actionEvent -> {
                                    log(LOG_DEBUG, "Secondary Enter listener triggered for placement finalization.");
                                    placementLogicRef.get().run();
                                    actionEvent.consume();
                                });
                                if (newState.getEditableLabel().getEditor() != null) {
                                     newState.getEditableLabel().getEditor().setOnAction(secondEnterHandlerRef.get());
                                }
                                secondClickHandlerRef.set(mouseEvent -> {
                                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                                        log(LOG_DEBUG, "Secondary Click listener triggered for placement finalization.");
                                        placementLogicRef.get().run();
                                        mouseEvent.consume();
                                    }
                                });
                                pane.setOnMouseClicked(secondClickHandlerRef.get());
                            } else {
                                // Deselection successful
                                log(LOG_SUCCESS, "State '" + newState.getName() + "' placed successfully.");
                                // --- Update DFA instance ---
                                log(LOG_DEBUG, "Adding new state '" + newState.getName() + "' to DFA model.");
                                dfa.addState(newState); // Add the newly placed state to the DFA
                                updateTransitionTable(); // Update the table now that the state is in the DFA
                                // --------------------------
                                isPlacingNewState = false; // Reset flag on successful finalization
                            }
                        });

                        // Initial Enter Listener
                        log(LOG_DEBUG, "Attaching initial Enter listener for placement.");
                        enterPlacementHandlerRef.set(actionEvent -> {
                             log(LOG_DEBUG, "Initial Enter listener triggered.");
                             placementLogicRef.get().run();
                             actionEvent.consume();
                        });
                         if (newState.getEditableLabel().getEditor() != null) {
                              newState.getEditableLabel().getEditor().setOnAction(enterPlacementHandlerRef.get());
                         }

                        // Initial Left Click Listener
                        log(LOG_DEBUG, "Attaching initial Click listener for placement.");
                        firstClickHandlerRef.set(mouseEvent -> {
                            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                                log(LOG_DEBUG, "Initial Click listener triggered.");
                                placementLogicRef.get().run();
                                mouseEvent.consume();
                            }
                        });
                        pane.setOnMouseClicked(firstClickHandlerRef.get());

                        event.consume();
                    }
                    break;
            }
        });
        log(LOG_SUCCESS, "Application Controller initialized.");

        updateUIComponents(); // Initial population of table and combo boxes

        // State Settings Tab Handlers
        stateNameTextField.setOnAction(event -> handleStateNameChange());
        stateNameTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { handleStateNameChange(); }
        });
        startStateCheck.setOnAction(event -> handleStartStateCheck());
        acceptingStateCheck.setOnAction(event -> handleAcceptingStateCheck());

        // Tab selection listener
        TabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == stateSettingsTab) {
                updateStateSettingsTab();
            } else if (newTab == transitionSettingsTab) {
                 // Get currently selected transition to populate the tab
                 Transition selected = Transition.getSelectedTransition(); 
                 updateTransitionSettingsTab(selected); 
            }
        });

        // Add event handlers for Transition Settings Tab
        transitionNameCombo.setOnAction(event -> handleTransitionNameChange());
        // Add focus lost listener if needed, similar to state name
        transitionNameCombo.focusedProperty().addListener((obs, oldVal, newVal) -> {
             if (!newVal) { handleTransitionNameChange(); }
         });
         
         // Potentially add handlers for from/to state combos if they become editable later
         // fromStateCombo.setOnAction(event -> handleTransitionEndpointChange());
         // toStateCombo.setOnAction(event -> handleTransitionEndpointChange());
    }

    /**
     * Builds the DFA configuration from the states and transitions present in the pane.
     * This method collects all State objects, gathers their transitions, builds the alphabet,
     * and determines the set of accepting states as well as an initial state.
     */
    private void buildDFAFromPane() {
        log(LOG_DEBUG, "Entering buildDFAFromPane...");
        List<State> stateList = new ArrayList<>();
        Set<String> alphabet = new HashSet<>();
        Set<State> acceptingStates = new HashSet<>();
        Map<State, Map<String, State>> transitionsMap = new HashMap<>();
        State initialState = null;

        // Iterate through the pane's children, filtering for State instances.
        log(LOG_DEBUG, "Iterating through pane children to find states and transitions.");
        for (Node node : pane.getChildren()) {
            if (node instanceof State) {
                State s = (State) node;
                stateList.add(s);
                if (s.isAccepting()) {
                    acceptingStates.add(s);
                }
                // For the initial state, we simply take the first one.
                // TODO: Add proper initial state handling/selection
                if (initialState == null) {
                    initialState = s;
                }
                // Build the transitions mapping for this state.
                Map<String, State> transMap = new HashMap<>();
                for (Transition t : s.getTransitions()) {
                    if (t.getSymbol() != null && t.getNextState() != null) {
                        transMap.put(t.getSymbol(), (State) t.getNextState());
                        alphabet.add(t.getSymbol());
                    }
                }
                transitionsMap.put(s, transMap);
            }
        }
        log(LOG_DEBUG, "Found " + stateList.size() + " states, " + alphabet.size() + " alphabet symbols.");
        // Pass the assembled data to the DFA's configuration method.
        log(LOG_DEBUG, "Calling dfa.configureDFA() with extracted states, alphabet, initial/accepting states, and transitions.");
        dfa.configureDFA(stateList, alphabet, initialState, acceptingStates, transitionsMap);
        log(LOG_DEBUG, "Exiting buildDFAFromPane.");
    }

    /**
     * Checks whether the provided newState overlaps with any existing states on the pane.
     */
    private boolean isOverlapping(State newState) {
        // Get the new state's position and radius.
        double x1 = newState.getLayoutX();
        double y1 = newState.getLayoutY();
        double r1 = newState.getMainCircle().getRadius();

        for (Node node : pane.getChildren()) {
            if (node instanceof State && node != newState) {
                State otherState = (State) node;
                double x2 = otherState.getLayoutX();
                double y2 = otherState.getLayoutY();
                double r2 = otherState.getMainCircle().getRadius();
                double distance = Math.hypot(x1 - x2, y1 - y2);
                if (distance < (r1 + r2)) {
                    log(LOG_DEBUG, "Overlap detected between new state and state '" + otherState.getName() + "'.");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Shows an error alert when a new state overlaps with an existing state.
     */
    private void showOverlapError() {
        logError("Showing overlap error alert.");
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid State Placement");
        alert.setHeaderText(null);
        alert.setContentText("The state overlaps with an existing state. Please reposition it.");
        alert.showAndWait();
    }

    @Override
    public void onSelected(Object obj) {
        if (obj instanceof State) {
            State selected = (State) obj;
            log(LOG_INFO, "State selected: " + selected.getName());
            updateStateSettingsTab(); 
            clearTransitionSettingsTab(); // Clear the other tab
            TabPane.getSelectionModel().select(stateSettingsTab);
        } else if (obj instanceof Transition) { 
             Transition selected = (Transition) obj;
             log(LOG_INFO, "Transition selected: " + selected);
             updateTransitionSettingsTab(selected); // Pass selected transition
             clearStateSettingsTab(); // Clear the other tab
             TabPane.getSelectionModel().select(transitionSettingsTab);
        } else {
             log(LOG_WARN, "onSelected called with unexpected object type: " + (obj == null ? "null" : obj.getClass().getSimpleName()));
             clearStateSettingsTab();
             clearTransitionSettingsTab();
        }
    }
    
    @Override
    public void onDeselected(Object obj) {
         log(LOG_INFO, "Object deselected: " + (obj instanceof State ? ((State)obj).getName() : obj));
         clearStateSettingsTab();
         clearTransitionSettingsTab();
    }
    
    // - Called by DFA when model changes require UI updates
    // - Updates components reflecting the overall structure (table, combo box items)
    public void updateUIComponents() {
        log(LOG_INFO, "Updating general UI components (Table, ComboBox Items) due to model change...");
        updateTransitionTable();
        updateComboBoxes();
        // REMOVED: Updating settings tabs here caused issues.
        // Settings tabs are updated via onSelected or TabPane listener.
    }
    
    // - Update the state settings tab based on the currently selected state
    private void updateStateSettingsTab() {
        State selected = State.getSelectedState();
        if (selected != null) { 
            log(LOG_DEBUG, "Updating State Settings tab for state: " + selected.getName());
            stateNameTextField.setText(selected.getName());
            startStateCheck.setSelected(selected.isInitial()); 
            acceptingStateCheck.setSelected(selected.isAccepting());
        } else {
             log(LOG_DEBUG, "Clearing State Settings tab as no state is selected.");
             clearStateSettingsTab();
        }
    }
    
    // - Update the transition settings tab based on the currently selected transition
    private void updateTransitionSettingsTab(Transition selected) { 
        if (selected != null) { 
            log(LOG_DEBUG, "Updating Transition Settings tab for transition: " + selected);
            
            String fromName = selected.getFromState().getName();
            String toName = (selected.getNextState() != null) ? selected.getNextState().getName() : null;
            String symbolName = selected.getSymbol();
            
            log(LOG_DEBUG, "Setting fromStateCombo to: " + fromName);
            fromStateCombo.getSelectionModel().select(fromName); 
            // Add checks to see if selection worked, in case combo items weren't ready
            if (!Objects.equals(fromStateCombo.getSelectionModel().getSelectedItem(), fromName)) {
                log(LOG_WARN, "Failed to select '"+ fromName + "' in fromStateCombo. Items: " + fromStateCombo.getItems());
            }

            if (toName != null) {
                log(LOG_DEBUG, "Setting toStateCombo to: " + toName);
                toStateCombo.getSelectionModel().select(toName); 
                if (!Objects.equals(toStateCombo.getSelectionModel().getSelectedItem(), toName)) {
                     log(LOG_WARN, "Failed to select '"+ toName + "' in toStateCombo. Items: " + toStateCombo.getItems());
                 }
            } else {
                log(LOG_DEBUG, "Clearing selection for toStateCombo as target state is null.");
                toStateCombo.getSelectionModel().clearSelection();
            }
            
            log(LOG_DEBUG, "Setting transitionNameCombo value to: " + symbolName);
            transitionNameCombo.setValue(symbolName); 
            
            // Disable editing of 'from' and 'to' state for existing transitions
            fromStateCombo.setDisable(true);
            toStateCombo.setDisable(true);
        } else {
            log(LOG_DEBUG, "Clearing transition settings tab as selected transition is null.");
            clearTransitionSettingsTab();
        }
    }

    // - Helper to clear transition settings tab
    private void clearTransitionSettingsTab() { 
         log(LOG_DEBUG, "Clearing Transition Settings tab.");
         fromStateCombo.getSelectionModel().clearSelection();
         toStateCombo.getSelectionModel().clearSelection();
         transitionNameCombo.setValue(""); // Clear editable ComboBox
         // Re-enable combo boxes if they were disabled
         fromStateCombo.setDisable(false);
         toStateCombo.setDisable(false);
    }

    // - Handle changes to the state name text field
    private void handleStateNameChange() {
        State selected = State.getSelectedState();
        if (selected != null) {
            String newName = stateNameTextField.getText().trim();
            if (!newName.equals(selected.getName())) {
                 log(LOG_INFO, "Attempting to change name of state '" + selected.getName() + "' to '" + newName + "' via settings tab.");
                 // State.setName handles validation and notifying DFA
                 selected.setName(newName); 
                 // setName might fail validation; update text field back if name didn't actually change
                 if (!selected.getName().equals(newName)) {
                     stateNameTextField.setText(selected.getName());
                 }
            }
        }
    }

    // - Handle clicks on the Start State checkbox
    private void handleStartStateCheck() {
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = startStateCheck.isSelected();
            log(LOG_INFO, "Setting state '" + selected.getName() + "' as initial state: " + isChecked);
            if (isChecked) {
                dfa.setInitialState(selected);
            } else {
                // Prevent unchecking the current initial state directly via checkbox
                // The user must select another state and check its box
                if (selected.equals(dfa.getInitialState())) {
                    log(LOG_WARN, "Cannot uncheck the current initial state. Select another state to be initial.");
                    startStateCheck.setSelected(true); // Revert the checkbox
                    showAlert(AlertType.INFORMATION, "Initial State", "Cannot deselect the current initial state. Select another state and mark it as initial instead.");
                }
            }
        }
    }

    // - Handle clicks on the Accepting State checkbox
    private void handleAcceptingStateCheck() {
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = acceptingStateCheck.isSelected();
            log(LOG_INFO, "Setting state '" + selected.getName() + "' as accepting state: " + isChecked);
            if (isChecked) {
                dfa.addAcceptingState(selected);
            } else {
                dfa.removeAcceptingState(selected);
            }
        }
    }

    // - Called by DFA when the initial state changes
    public void handleInitialStateChange(State oldInitial, State newInitial) {
        log(LOG_DEBUG, "Handling initial state change in UI. Old: " + (oldInitial != null ? oldInitial.getName() : "null") + ", New: " + (newInitial != null ? newInitial.getName() : "null"));
        // Force UI update for the State Settings tab IF the currently selected state
        // is either the one that *was* initial or the one that *is now* initial.
        State currentlySelected = State.getSelectedState();
        if (currentlySelected != null && (currentlySelected.equals(oldInitial) || currentlySelected.equals(newInitial))) {
             updateStateSettingsTab();
        }
        // The main table/combo box updates are triggered separately by DFA
    }
    
    // - Called by DFA when a state's accepting status changes
    public void handleAcceptingStateChange(State changedState) {
         if (changedState == null) return;
         log(LOG_DEBUG, "Handling accepting state change in UI for state: " + changedState.getName());
        // Force UI update for the State Settings tab IF the currently selected state
        // is the one whose accepting status changed.
        State currentlySelected = State.getSelectedState();
        if (currentlySelected != null && currentlySelected.equals(changedState)) {
            updateStateSettingsTab();
        }
        // The main table update (which might show accepting status) is triggered separately by DFA
    }

    // - Helper to clear state settings tab
    private void clearStateSettingsTab() {
        stateNameTextField.setText("");
        startStateCheck.setSelected(false);
        acceptingStateCheck.setSelected(false);
    }
    
    // - Handle changes to the transition name/symbol ComboBox
    private void handleTransitionNameChange() {
        Transition selected = Transition.getSelectedTransition(); // Get selected from static field
        if (selected != null) {
            String newSymbol = transitionNameCombo.getValue(); // Get value from editable ComboBox
            if (newSymbol != null && !newSymbol.equals(selected.getSymbol())) {
                log(LOG_INFO, "Attempting to change symbol for transition " + selected + " to '" + newSymbol + "'");
                // Use the transition's internal logic which includes validation and DFA update
                selected.setSymbol(newSymbol); 
                 // If setSymbol fails (e.g., validation), it might revert internally or need explicit UI update
                 // For simplicity, assume setSymbol handles it or re-fetch symbol if needed.
                 // If validation happens elsewhere (like handleLabelEditComplete), call that instead.
                 // Example: if (!selected.handleLabelEditComplete()) { transitionNameCombo.setValue(selected.getSymbol()); }
                 transitionNameCombo.setValue(selected.getSymbol()); // Refresh UI with potentially validated/reverted symbol
            }
        }
    }

    // Placeholder for handling changes to From/To states if they become editable
    // private void handleTransitionEndpointChange() { ... }

    // Methods for managing the transition TableView

    /**
     * Sets up the static configuration of the transition table (e.g., state column factory).
     * Should be called once during initialization.
     */
    private void setupTransitionTable() {
        log(LOG_DEBUG, "Entering setupTransitionTable...");
        // Configure the state column to display the state's name
        stateColumn.setCellValueFactory(cellData -> {
            State state = cellData.getValue();
            // Assuming State has a getName() method returning String
            return new SimpleStringProperty(state != null ? state.getName() : "");
        });
        stateColumn.setStyle("-fx-alignment: CENTER;"); // Center text

        // Clear placeholder columns potentially added in FXML if any (under the parent)
        transitionsParentColumn.getColumns().clear();
        transitionsParentColumn.setStyle("-fx-alignment: CENTER;"); // Center parent column header
        log(LOG_DEBUG, "Exiting setupTransitionTable.");
    }


    /**
     * Updates the transition table content based on the current DFA state.
     * Clears existing dynamic columns and items, then repopulates.
     * Call this whenever the DFA's structure (states, transitions, alphabet) changes.
     */
    public void updateTransitionTable() {
        log(LOG_INFO, "Updating transition table...");
        // Basic null checks for safety
        if (dfa == null || dfaTransitionTable == null || stateColumn == null || transitionsParentColumn == null) {
            logError("Cannot update transition table: Required components or DFA model is null.");
            return;
        }

        log(LOG_DEBUG, "Clearing existing table items and columns.");
        // 1. Clear existing items and dynamic transition columns
        dfaTransitionTable.getItems().clear();
        transitionsParentColumn.getColumns().clear();

        // 2. Get Alphabet and States from DFA
        log(LOG_DEBUG, "Fetching alphabet by calling dfa.getAlphabet().");
        Set<String> alphabet = dfa.getAlphabet(); // Assumes dfa.getAlphabet() exists
        log(LOG_DEBUG, "Fetching states by calling dfa.getStates().");
        Set<State> states = dfa.getStates();       // Assumes dfa.getStates() exists

        if (alphabet == null || states == null) {
             logError("Cannot update transition table: DFA alphabet or states are null.");
             return; // Or handle appropriately
        }
        log(LOG_DEBUG, "Found " + states.size() + " states and " + alphabet.size() + " alphabet symbols for table.");

        // Sort alphabet for consistent column order
        List<String> sortedAlphabet = new ArrayList<>(alphabet);
        // Remove null or empty symbols which cannot be transitions
        sortedAlphabet.removeIf(s -> s == null || s.trim().isEmpty() || s.equals("?")); // Also remove the default "?"
        Collections.sort(sortedAlphabet);
        log(LOG_DEBUG, "Sorted alphabet for table columns: " + sortedAlphabet);


        // 3. Create columns for each valid symbol in the alphabet
        log(LOG_DEBUG, "Creating table columns for alphabet symbols...");
        for (String symbol : sortedAlphabet) {

            TableColumn<State, String> symbolColumn = new TableColumn<>(symbol);
            symbolColumn.setCellValueFactory(cellData -> {
                State currentState = cellData.getValue();
                String destinationStateName = "-"; // Default display for no transition

                if (currentState != null) {
                    // Assumes State has getTransition(String symbol) returning Transition or null
                    Transition transition = currentState.getTransition(symbol);
                    if (transition != null && transition.getNextState() != null) {
                        // Assumes getNextState() returns a State object which has getName()
                        destinationStateName = transition.getNextState().getName();
                    }
                }
                return new SimpleStringProperty(destinationStateName);
            });
            // Optional: Set preferred width or other properties
            symbolColumn.setPrefWidth(75);
            symbolColumn.setStyle("-fx-alignment: CENTER;"); // Center text in cells

            transitionsParentColumn.getColumns().add(symbolColumn);
        }

        // 4. Populate the table with states
        log(LOG_DEBUG, "Populating table rows with states.");
        // Convert Set<State> to ObservableList<State>
        ObservableList<State> observableStateList = FXCollections.observableArrayList(states);
        dfaTransitionTable.setItems(observableStateList);

        // Refresh the table view to ensure changes are visible
        log(LOG_DEBUG, "Requesting table refresh.");
        dfaTransitionTable.refresh();
        log(LOG_SUCCESS, "Transition table updated successfully.");
    }

    public void rebuildUIFromMinimizedDFA() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'rebuildUIFromMinimizedDFA'");
    }

    // Add calls to updateTransitionTable() in other places where the DFA is modified:
    // - After creating a new state (e.g., in handleNewStateKeyPress or associated mouse click)
    // - After deleting a state/transition (e.g., in handleDeletionKeyPress)
    // - After modifying a transition's symbol
    // - Potentially after minimization or removing unreachable states
    // - After loading a DFA from a file (if applicable)

    // - Update the items in the state selection ComboBoxes
    public void updateComboBoxes() {
        if (dfa == null || fromStateCombo == null || toStateCombo == null) {
            logError("Cannot update ComboBoxes: DFA or ComboBoxes are null.");
            return;
        }

        log(LOG_DEBUG, "Updating state ComboBoxes...");
        Set<State> currentStates = dfa.getStates();
        List<String> stateNames = currentStates.stream()
                                        .map(State::getName)
                                        .filter(name -> name != null && !name.isEmpty()) // Filter out states without names yet
                                        .sorted()
                                        .collect(Collectors.toList());
        
        ObservableList<String> observableStateNames = FXCollections.observableArrayList(stateNames);

        // Preserve selection if possible
        String selectedFrom = fromStateCombo.getSelectionModel().getSelectedItem();
        String selectedTo = toStateCombo.getSelectionModel().getSelectedItem();

        fromStateCombo.setItems(observableStateNames);
        toStateCombo.setItems(observableStateNames);

        // Restore selection
        if (stateNames.contains(selectedFrom)) {
            fromStateCombo.getSelectionModel().select(selectedFrom);
        }
        if (stateNames.contains(selectedTo)) {
            toStateCombo.getSelectionModel().select(selectedTo);
        }
        log(LOG_DEBUG, "State ComboBoxes updated.");
        
        // Update transition name ComboBox (if needed - currently editable)
        // You might want to populate this with existing symbols from the alphabet
        Set<String> alphabet = dfa.getAlphabet();
        List<String> symbols = alphabet.stream().sorted().collect(Collectors.toList());
        ObservableList<String> observableSymbols = FXCollections.observableArrayList(symbols);
        String selectedSymbol = transitionNameCombo.getSelectionModel().getSelectedItem(); // Or use getValue() for editable
        // transitionNameCombo.setItems(observableSymbols); // Uncomment if you want to populate suggestions
        // if (symbols.contains(selectedSymbol)) { transitionNameCombo.getSelectionModel().select(selectedSymbol); }
    }

    // - Helper to show alerts
    private void showAlert(AlertType alertType, String title, String content) {
        log(LOG_WARN, "Showing alert: " + title + " - " + content);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Helper method to delete a transition ---
    private void deleteTransition(Transition transition) {
        if (transition == null) {
            log(LOG_WARN, "Attempted to delete a null transition.");
            return;
        }

        log(LOG_DEBUG, "Executing deletion for transition: " + transition);

        // 1. Deselect if it's the currently selected one
        if (transition == Transition.getSelectedTransition()) {
             Transition.getSelectedTransition().deselect(); // Correct way to deselect
             log(LOG_DEBUG, "Deselected the transition before deletion.");
             clearTransitionSettingsTab(); // Clear the settings tab too
        }

        // 2. Remove from Pane
        if (pane != null) {
            pane.getChildren().remove(transition);
            log(LOG_DEBUG, "Removed transition graphics from pane.");
        } else {
             log(LOG_WARN, "Pane is null, cannot remove transition graphics.");
        }

        // 3. Remove from 'from' State's transition list
        State fromState = transition.getFromState();
        if (fromState != null) {
            log(LOG_DEBUG, "Removing transition reference from state: " + fromState.getName());
            fromState.removeTransition(transition); // Requires implementation in State class
        } else {
            log(LOG_WARN, "Cannot remove transition from state: 'fromState' is null.");
        }

        // 4. Update UI (Table, potentially Combos if alphabet changes - though less likely)
        log(LOG_DEBUG, "Requesting UI update after transition deletion.");
        updateUIComponents(); 

        log(LOG_SUCCESS, "Transition deleted successfully: " + transition);
    }
    // -----------------------------------------
}
