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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class Application_Controler  implements SelectionListener  {
    @FXML
    private TabPane TabPane;
    @FXML
    private Tab transitionSettingsTab , stateSettingsTab;
    @FXML
    private BorderPane BorderPane;
    @FXML
    private TextField stateNameTextField;
    @FXML
    private CheckBox startStateCheck, acceptingStateCheck;
    @FXML
    private ComboBox fromStateCombo, toStateCombo, transitionNameCombo;
    @FXML
    private TableView dfaTransitionTable;
    @FXML
    private TableColumn stateColumn;
    @FXML
    private TableColumn transitionsParentColumn;
    @FXML
    private Pane pane;
    @FXML
    private Button startProcessButton;
    @FXML
    private TextArea logTextArea;
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




    // Hold a reference to the DFA instance.
    private DFA dfa;
    private Transition currentTransition;
    private boolean waitingForSecondClick = false;
    private boolean isPlacingNewState = false; // Flag for new state placement

    @FXML
    public void initialize() {
        // Initialize the DFA model.
        dfa = new DFA();


        Timeline dfaUpdater = new Timeline(
                new KeyFrame(Duration.millis(100), event -> {
                    logTextArea.setText(dfa.getDFAData());
                })
        );
        dfaUpdater.setCycleCount(Timeline.INDEFINITE);
        dfaUpdater.play();

        final AtomicReference<EventHandler<MouseEvent>> transitionMouseMoveHandlerRef = new AtomicReference<>();

        // --- SINGLE MOUSE CLICKED Filter (Handles Everything) ---
        pane.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            // Ignore clicks if currently placing a new state via Ctrl+N
            if (isPlacingNewState) {
                // System.out.println("Clicked Filter: Ignoring click because isPlacingNewState=true"); // Keep log minimal
                return; 
            }

            Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
            State currentlySelected = State.getSelectedState();
            
            // Log click target
            String clickedInfo = "Clicked on: " + (clickedNode == null ? "null" : clickedNode.getClass().getSimpleName());
            if (clickedNode != null && clickedNode.getUserData() != null) {
                 clickedInfo += " (userData: " + clickedNode.getUserData().getClass().getSimpleName() + ")";
            }
             System.out.println(clickedInfo);

            // --- Revised Logic Order ---
            
            // 1. Handle Transition Completion (if waiting)
            if (waitingForSecondClick) {
                System.out.println("[Completion] Entered block. currentTransition is " + (currentTransition == null ? "NULL" : "NOT NULL"));
                
                pane.setOnMouseMoved(null); // Stop arrow following
                transitionMouseMoveHandlerRef.set(null);

                // Ignore clicks on pane during second step
                if (clickedNode == pane) { 
                    System.out.println("[Completion] Ignoring click on pane.");
                    mouseEvent.consume();
                    return;
                }
                
                // --- Find Target State by walking up hierarchy --- 
                State targetState = null;
                Node current = clickedNode;
                while (current != null && !(current instanceof State)) {
                     System.out.println("[Completion] Walking up... current node is: " + current.getClass().getSimpleName());
                     current = current.getParent();
                }
                if (current instanceof State) {
                     targetState = (State) current;
                     System.out.println("[Completion] Found target state via hierarchy walk: " + targetState.getName());
                } else {
                     System.out.println("[Completion] Could not find State parent for clicked node: " + (clickedNode == null ? "null" : clickedNode.getClass().getSimpleName()));
                }
                // ------------------------------------------------
                
                // Check if we found a target state and have a transition pending
                if (currentTransition != null && targetState != null) {
                    // Complete the transition 
                    currentTransition.completeTransition(targetState); 
                    currentTransition.setSymbol("?"); 
                    currentTransition.getFromState().addTransition(currentTransition);
                    System.out.println("Transition added: (" + currentTransition.getFromState().getName() + " -> " + targetState.getName() + ") with default symbol '?'");
                    currentTransition.select(); // Selects and triggers editing
                    
                } else {
                     // Click was not on a valid state -> Cancel
                     System.out.println("Transition cancelled: Second click target was not resolved to a State.");
                    if (currentTransition != null) {
                        pane.getChildren().remove(currentTransition);
                    }
                }
                
                // Reset state regardless of success/failure of completion
                System.out.println("[Completion] Resetting flags.");
                waitingForSecondClick = false;
                currentTransition = null;
                mouseEvent.consume();
                return; // Finished handling completion attempt
            }
            
            // 2. Handle Transition Initiation (Ctrl+Click on state) - CHECK BEFORE GENERAL STATE CLICK
            if (!waitingForSecondClick && mouseEvent.isControlDown() && mouseEvent.getButton() == MouseButton.PRIMARY && 
                clickedNode != null && clickedNode.getUserData() instanceof State) {
                
                System.out.println("Handling Click: Transition Initiation");
                State fromState = (State) clickedNode.getUserData();
                currentTransition = new Transition(fromState); 
                transitionMouseMoveHandlerRef.set(moveEvent -> {
                     if (currentTransition != null) {
                         currentTransition.setTempEnd(moveEvent.getX(), moveEvent.getY());
                     }
                     moveEvent.consume();
                });
                pane.setOnMouseMoved(transitionMouseMoveHandlerRef.get());
                waitingForSecondClick = true;
                mouseEvent.consume(); 
                return; // Finished handling initiation
            }
            
            // 3. Handle State Selection/Deselection (No Ctrl, Click on state)
            if (clickedNode != null && clickedNode.getUserData() instanceof State) {
                 System.out.println("Handling Click: State Select/Deselect");
                State state = (State) clickedNode.getUserData();
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    state.select();
                } else if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                    if (currentlySelected == state && !currentlySelected.wasJustDragged()) {
                        currentlySelected.deselect();
                    }
                }
                mouseEvent.consume(); 
                return; // Finished handling state click
            }
            
            // 4. Handle Pane Click Deselection (if none of the above)
            if (!mouseEvent.isConsumed() && clickedNode == pane) {
                 System.out.println("Handling Click: Pane Click Deselect");
                 // Deselect currently selected State OR Transition
                if (currentlySelected != null && !currentlySelected.wasJustDragged()) {
                    currentlySelected.deselect();
                } else if (Transition.getSelectedTransition() != null) {
                     // Add check for dragging if needed for transitions too
                     Transition.getSelectedTransition().deselect();
                } 
                mouseEvent.consume(); 
                 return; // Finished handling pane click
            }
            
            // 5. Fall through - Log if click wasn't handled
             if (!mouseEvent.isConsumed()) {
                 System.out.println("Warning: Click event was not consumed by any specific logic.");
             }

        }); // End of MOUSE_CLICKED filter

        // REMOVE MOUSE PRESSED Filter 
        // pane.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> { ... });

        // Button action for minimization process
        startProcessButton.setOnAction(actionEvent -> {
            buildDFAFromPane();
            dfa.removeUnreachableStates();
            dfa.minimizeDFA();
            dfa.printMinimizedDFA();
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
                        State selectedState = State.getSelectedState();
                        if (selectedState != null) {
                             System.out.println("Deleting state: " + selectedState.getName());
                             selectedState.deleteState(); 
                        }
                        event.consume();
                    }
                    break;
                case Z: // Undo (Ctrl+Z)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+Z pressed. (Undo not implemented)");
                        event.consume();
                    }
                    break;
                case Y: // Redo (Ctrl+Y)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+Y pressed. (Redo not implemented)");
                        event.consume();
                    }
                    break;
                case S: // Save (Ctrl+S)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+S pressed. (Save not implemented)");
                        event.consume();
                    }
                    break;
                case O: // Open (Ctrl+O)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+O pressed. (Open not implemented)");
                        event.consume();
                    }
                    break;
                case R: // Run Minimization (Ctrl+R)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+R pressed. Triggering minimization.");
                        startProcessButton.fire(); // Simulate button click
                        event.consume();
                    }
                    break;
                case K: // Clear Pane (Ctrl+K)
                    if (event.isControlDown()) {
                        System.out.println("Ctrl+K pressed. (Clear not implemented)");
                         // Needs logic to remove all states and transitions from pane and DFA
                        event.consume();
                    }
                    break;
                case N: // New State (Ctrl+N)
                    if (event.isControlDown()) {
                        // Prevent starting another placement if one is in progress
                        if (isPlacingNewState) return; 
                        
                        System.out.println("Creating new state..."); 
                        isPlacingNewState = true; // Set flag

                        State newState = new State(-50, -50, 30, Color.WHITE); 
                        newState.setSelectionListener(this);
                        pane.getChildren().add(newState);
                        newState.select(); // Select immediately

                        // Make state follow mouse
                        mouseMoveHandlerRef.set(moveEvent -> {
                            newState.moveState(moveEvent.getX(), moveEvent.getY());
                            moveEvent.consume();
                        });
                        pane.setOnMouseMoved(mouseMoveHandlerRef.get());

                        // Define the common placement/finalization logic 
                        final AtomicReference<Runnable> placementLogicRef = new AtomicReference<>();
                        placementLogicRef.set(() -> {
                            pane.setOnMouseMoved(null);
                            pane.setOnMouseClicked(null);
                            if (newState.getEditableLabel().getEditor() != null) {
                                newState.getEditableLabel().getEditor().setOnAction(null);
                            }

                            if (isOverlapping(newState)) {
                                showOverlapError();
                                pane.getChildren().remove(newState);
                                isPlacingNewState = false; // Reset flag on failure
                                return; 
                            }

                            newState.deselect();

                            if (State.getSelectedState() == newState) {
                                // Deselect failed, setup second attempt listeners
                                secondEnterHandlerRef.set(actionEvent -> {
                                    placementLogicRef.get().run();
                                    actionEvent.consume();
                                });
                                if (newState.getEditableLabel().getEditor() != null) {
                                     newState.getEditableLabel().getEditor().setOnAction(secondEnterHandlerRef.get());
                                }
                                secondClickHandlerRef.set(mouseEvent -> {
                                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                                        placementLogicRef.get().run();
                                        mouseEvent.consume();
                                    }
                                });
                                pane.setOnMouseClicked(secondClickHandlerRef.get());
                            } else {
                                System.out.println("State '" + newState.getName() + "' added successfully."); 
                                isPlacingNewState = false; // Reset flag on successful finalization
                            }
                        });

                        // Initial Enter Listener
                        enterPlacementHandlerRef.set(actionEvent -> {
                             placementLogicRef.get().run();
                             actionEvent.consume();
                        });
                         if (newState.getEditableLabel().getEditor() != null) {
                              newState.getEditableLabel().getEditor().setOnAction(enterPlacementHandlerRef.get());
                         }

                        // Initial Left Click Listener
                        firstClickHandlerRef.set(mouseEvent -> {
                            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
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
    }

    /**
     * Builds the DFA configuration from the states and transitions present in the pane.
     * This method collects all State objects, gathers their transitions, builds the alphabet,
     * and determines the set of accepting states as well as an initial state.
     */
    private void buildDFAFromPane() {
        List<State> stateList = new ArrayList<>();
        Set<String> alphabet = new HashSet<>();
        Set<State> acceptingStates = new HashSet<>();
        Map<State, Map<String, State>> transitionsMap = new HashMap<>();
        State initialState = null;

        // Iterate through the pane's children, filtering for State instances.
        for (Node node : pane.getChildren()) {
            if (node instanceof State) {
                State s = (State) node;
                stateList.add(s);
                if (s.isAccepting()) {
                    acceptingStates.add(s);
                }
                // For the initial state, we simply take the first one.
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
        // Pass the assembled data to the DFA's configuration method.
        dfa.configureDFA(stateList, alphabet, initialState, acceptingStates, transitionsMap);
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid State Placement");
        alert.setHeaderText(null);
        alert.setContentText("The state overlaps with an existing state. Please reposition it.");
        alert.showAndWait();
    }

    public void onSelected(Object obj) {
        if (obj instanceof State) {
            State selected = (State) obj;
            stateNameTextField.setText(selected.getName());
            acceptingStateCheck.setSelected(selected.isAccepting());
            TabPane.getSelectionModel().select(stateSettingsTab);
        } else if (obj instanceof CurvedArrow) {
            Transition selected = null;
            Object arrowUserData = ((CurvedArrow) obj).getUserData();
            if (arrowUserData instanceof Transition) {
                selected = (Transition) arrowUserData;
                fromStateCombo.getSelectionModel().select(selected.getFromState().getName());
                toStateCombo.getSelectionModel().select(selected.getNextState().getName());
                transitionNameCombo.getSelectionModel().select(selected.getSymbol());
                TabPane.getSelectionModel().select(transitionSettingsTab);
            }
        }
    }

    @Override
    public void onDeselected(Object obj) {
        if (obj instanceof State) {
            State deselected = (State) obj;
        }
        else if (obj instanceof CurvedArrow) {
        }
    }

    // Helper method for showing alerts (if not already present)
    private void showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
