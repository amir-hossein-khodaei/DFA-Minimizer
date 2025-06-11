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
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.stream.Collectors;
import java.util.Objects;

public class Application_Controler implements SelectionListener {
    
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

    private DFA dfa;
    private Transition currentTransition;
    private boolean waitingForSecondClick = false;
    private boolean isPlacingNewState = false; 

    public void log(String message) {
        if (logTextArea != null) {
            Platform.runLater(() -> logTextArea.appendText(message + "\n"));
        }
    }
    
    @FXML
    private void handleClearLogButton() {
        if (logTextArea != null) {
            logTextArea.clear();
        }
    }

    @FXML
    public void initialize() {
        
        if (clearLogButton != null) {
            clearLogButton.setOnAction(event -> handleClearLogButton());
        }
        
        dfa = new DFA(this);

        setupTransitionTable();
        updateTransitionTable();

        if (startProcessButton != null) {
            startProcessButton.setText("Minimize DFA"); 
            startProcessButton.setOnAction(event -> {
                if (dfa != null) {
                    dfa.minimizeDFA(); 
                } else {
                    log("DFA model is null, cannot minimize.");
                }
            });
        }

        final AtomicReference<EventHandler<MouseEvent>> transitionMouseMoveHandlerRef = new AtomicReference<>();

        pane.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            if (isPlacingNewState) {
                return;
            }

            Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
            String targetInfo = (clickedNode == null) ? "Pane" : clickedNode.getClass().getSimpleName();
            if (clickedNode != null && clickedNode.getUserData() != null) {
                targetInfo += " (userData: " + clickedNode.getUserData().getClass().getSimpleName() + ")";
            }
            
            State currentlySelected = State.getSelectedState();

            String clickedInfo = "Clicked on: " + (clickedNode == null ? "null" : clickedNode.getClass().getSimpleName());
            if (clickedNode != null && clickedNode.getUserData() != null) {
                 clickedInfo += " (userData: " + clickedNode.getUserData().getClass().getSimpleName() + ")";
            }

            if (waitingForSecondClick) {
                pane.setOnMouseMoved(null); 
                transitionMouseMoveHandlerRef.set(null);

                if (clickedNode == pane) {
                    mouseEvent.consume();
                    return;
                }

                State targetState = null;
                Node current = clickedNode;
                while (current != null && !(current instanceof State)) {
                     current = current.getParent();
                }
                if (current instanceof State) {
                     targetState = (State) current;
                } else {
                    log("Target state not found for transition completion.");
                }

                if (currentTransition != null && targetState != null) {
                    currentTransition.completeTransition(targetState);
                    currentTransition.getFromState().addTransition(currentTransition);

                    dfa.addState(currentTransition.getFromState());
                    dfa.addState(targetState);

                    updateTransitionTable(); 

                } else {
                    if (currentTransition != null) {
                        pane.getChildren().remove(currentTransition);
                    }
                }

                waitingForSecondClick = false;
                currentTransition = null;
                mouseEvent.consume();
                return;
            }
            
            if (!waitingForSecondClick && mouseEvent.getButton() == MouseButton.SECONDARY) {
                Object source = mouseEvent.getTarget();

                Transition clickedTransition = null;
                if (source instanceof Node) {
                    Node clickedNodeInScene = (Node) source;
                    Node parent = clickedNodeInScene;
                    while (parent != null && !(parent.getUserData() instanceof Transition)) {
                        parent = parent.getParent();
                    }
                    if (parent != null && parent.getUserData() instanceof Transition) {
                        clickedTransition = (Transition) parent.getUserData();
                    }
                }

                if (clickedTransition != null) {
                    clickedTransition.select(); 
                    mouseEvent.consume();
                    return;
                }
            }

            if (!waitingForSecondClick && mouseEvent.isControlDown() && mouseEvent.getButton() == MouseButton.PRIMARY &&
                clickedNode != null && clickedNode.getUserData() instanceof State) {
                State fromState = (State) clickedNode.getUserData();
                currentTransition = new Transition(fromState, this.dfa, this);
                transitionMouseMoveHandlerRef.set(moveEvent -> {
                     if (currentTransition != null) {
                         currentTransition.setTempEnd(moveEvent.getX(), moveEvent.getY());
                     }
                     moveEvent.consume();
                });
                pane.setOnMouseMoved(transitionMouseMoveHandlerRef.get());
                waitingForSecondClick = true;
                mouseEvent.consume();
                return;
            }

            if (clickedNode != null && clickedNode.getUserData() instanceof State) {
                 State state = (State) clickedNode.getUserData();
                 if (mouseEvent.getButton() == MouseButton.SECONDARY || mouseEvent.getButton() == MouseButton.PRIMARY) {
                    if (currentlySelected == state && !currentlySelected.wasJustDragged() && mouseEvent.getButton() == MouseButton.PRIMARY) {
                        currentlySelected.deselect();
                    } else {
                        state.select();
                    }
                 }
                 mouseEvent.consume();
                 return;
            }

            if (!mouseEvent.isConsumed() && clickedNode == pane) {
                 if (currentlySelected != null && !currentlySelected.wasJustDragged()) {
                    currentlySelected.deselect();
                } else if (Transition.getSelectedTransition() != null) {
                     Transition.getSelectedTransition().deselect();
                }
                mouseEvent.consume();
                 return;
            }

        });
        
        BorderPane.setOnKeyPressed(event -> {
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
                            deleteTransition(selectedTransition); 
                        } else if (selectedState != null) {
                             selectedState.deleteState(); 
                        }
                        event.consume();
                    }
                    break;
                case Z:
                    if (event.isControlDown()) {
                        event.consume();
                    }
                    break;
                case Y:
                    if (event.isControlDown()) {
                        event.consume();
                    }
                    break;
                case S:
                    if (event.isControlDown()) {
                        event.consume();
                    }
                    break;
                case O:
                    if (event.isControlDown()) {
                        event.consume();
                    }
                    break;
                case R:
                    if (event.isControlDown()) {
                        startProcessButton.fire(); 
                        event.consume();
                    }
                    break;
                case K:
                    if (event.isControlDown()) {
                        event.consume();
                    }
                    break;
                case N:
                    if (event.isControlDown()) {
                        if (isPlacingNewState) {
                            return;
                        }

                        isPlacingNewState = true; 

                        State newState = new State(-50, -50, 30, Color.WHITE, Color.BLACK, 1.0, this);
                        newState.setSelectionListener(this);
                        pane.getChildren().add(newState);
                        newState.select(); 

                        mouseMoveHandlerRef.set(moveEvent -> {
                            newState.moveState(moveEvent.getX(), moveEvent.getY());
                            moveEvent.consume();
                        });
                        pane.setOnMouseMoved(mouseMoveHandlerRef.get());

                        final AtomicReference<Runnable> placementLogicRef = new AtomicReference<>();
                        placementLogicRef.set(() -> {
                            pane.setOnMouseMoved(null);
                            pane.setOnMouseClicked(null);
                            if (newState.getEditableLabel().getEditor() != null) {
                                newState.getEditableLabel().getEditor().setOnAction(null);
                            }

                            if (isOverlapping(newState)) {
                                log("State placement failed: Overlap detected.");
                                showOverlapError();
                                pane.getChildren().remove(newState);
                                isPlacingNewState = false; 
                                return;
                            }

                            newState.deselect(); 

                            if (State.getSelectedState() == newState) {
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
                                dfa.addState(newState); 
                                updateTransitionTable(); 
                                isPlacingNewState = false; 
                            }
                        });

                        enterPlacementHandlerRef.set(actionEvent -> {
                             placementLogicRef.get().run();
                             actionEvent.consume();
                        });
                         if (newState.getEditableLabel().getEditor() != null) {
                              newState.getEditableLabel().getEditor().setOnAction(enterPlacementHandlerRef.get());
                         }

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

        updateUIComponents(); 

        stateNameTextField.setOnAction(event -> handleStateNameChange());
        stateNameTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { handleStateNameChange(); }
        });
        startStateCheck.setOnAction(event -> handleStartStateCheck());
        acceptingStateCheck.setOnAction(event -> handleAcceptingStateCheck());

        TabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == stateSettingsTab) {
                updateStateSettingsTab();
            } else if (newTab == transitionSettingsTab) {
                 Transition selected = Transition.getSelectedTransition(); 
                 updateTransitionSettingsTab(selected); 
            }
        });

        transitionNameCombo.setOnAction(event -> handleTransitionNameChange());
        transitionNameCombo.focusedProperty().addListener((obs, oldVal, newVal) -> {
             if (!newVal) { handleTransitionNameChange(); }
         });
    }

    private void buildDFAFromPane() {
        List<State> stateList = new ArrayList<>();
        Set<String> alphabet = new HashSet<>();
        Set<State> acceptingStates = new HashSet<>();
        Map<State, Map<String, State>> transitionsMap = new HashMap<>();
        State initialState = null;

        for (Node node : pane.getChildren()) {
            if (node instanceof State) {
                State s = (State) node;
                stateList.add(s);
                if (s.isAccepting()) {
                    acceptingStates.add(s);
                }
                if (initialState == null) {
                    initialState = s;
                }
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
        log("Calling dfa.configureDFA() with extracted states, alphabet, initial/accepting states, and transitions.");
        dfa.configureDFA(stateList, alphabet, initialState, acceptingStates, transitionsMap);
    }

    private boolean isOverlapping(State newState) {
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

    private void showOverlapError() {
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
            updateStateSettingsTab(); 
            clearTransitionSettingsTab(); 
            TabPane.getSelectionModel().select(stateSettingsTab);
        } else if (obj instanceof Transition) { 
             Transition selected = (Transition) obj;
             updateTransitionSettingsTab(selected); 
             clearStateSettingsTab(); 
             TabPane.getSelectionModel().select(transitionSettingsTab);
        } else {
             clearStateSettingsTab();
             clearTransitionSettingsTab();
        }
    }
    
    @Override
    public void onDeselected(Object obj) {
         clearStateSettingsTab();
         clearTransitionSettingsTab();
    }
    
    public void updateUIComponents() {
        Platform.runLater(() -> {
            updateTransitionTable();
            updateComboBoxes();
        });
    }
    
    private void updateStateSettingsTab() {
        State selected = State.getSelectedState();
        if (selected != null) { 
            stateNameTextField.setText(selected.getName());
            startStateCheck.setSelected(selected.isInitial()); 
            acceptingStateCheck.setSelected(selected.isAccepting());
        } else {
             clearStateSettingsTab();
        }
    }
    
    private void updateTransitionSettingsTab(Transition selected) { 
        if (selected != null) { 
            String fromName = selected.getFromState().getName();
            String toName = (selected.getNextState() != null) ? selected.getNextState().getName() : null;
            String symbolName = selected.getSymbol();
            
            fromStateCombo.getSelectionModel().select(fromName); 
            if (!Objects.equals(fromStateCombo.getSelectionModel().getSelectedItem(), fromName)) {
            }

            if (toName != null) {
                toStateCombo.getSelectionModel().select(toName); 
                if (!Objects.equals(toStateCombo.getSelectionModel().getSelectedItem(), toName)) {
                 }
            } else {
                toStateCombo.getSelectionModel().clearSelection();
            }
            
            transitionNameCombo.setValue(symbolName); 
            
            fromStateCombo.setDisable(true);
            toStateCombo.setDisable(true);
        } else {
            clearTransitionSettingsTab();
        }
    }

    private void clearTransitionSettingsTab() { 
         fromStateCombo.getSelectionModel().clearSelection();
         toStateCombo.getSelectionModel().clearSelection();
         transitionNameCombo.setValue(""); 
         fromStateCombo.setDisable(false);
         toStateCombo.setDisable(false);
    }

    private void handleStateNameChange() {
        State selected = State.getSelectedState();
        if (selected != null) {
            String newName = stateNameTextField.getText().trim();
            if (!newName.equals(selected.getName())) {
                 selected.setName(newName); 
                 if (!selected.getName().equals(newName)) {
                     stateNameTextField.setText(selected.getName());
                 }
            }
        }
    }

    private void handleStartStateCheck() {
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = startStateCheck.isSelected();
            if (isChecked) {
                dfa.setInitialState(selected);
            } else {
                if (selected.equals(dfa.getInitialState())) {
                    startStateCheck.setSelected(true); 
                    showAlert(AlertType.INFORMATION, "Initial State", "Cannot deselect the current initial state. Select another state and mark it as initial instead.");
                }
            }
        }
    }

    private void handleAcceptingStateCheck() {
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = acceptingStateCheck.isSelected();
            if (isChecked) {
                dfa.addAcceptingState(selected);
            } else {
                dfa.removeAcceptingState(selected);
            }
        }
    }

    public void handleInitialStateChange(State oldInitial, State newInitial) {
        State currentlySelected = State.getSelectedState();
        if (currentlySelected != null && (currentlySelected.equals(oldInitial) || currentlySelected.equals(newInitial))) {
             updateStateSettingsTab();
        }
    }
    
    public void handleAcceptingStateChange(State changedState) {
         if (changedState == null) return;
        State currentlySelected = State.getSelectedState();
        if (currentlySelected != null && currentlySelected.equals(changedState)) {
            updateStateSettingsTab();
        }
    }

    private void clearStateSettingsTab() {
        stateNameTextField.setText("");
        startStateCheck.setSelected(false);
        acceptingStateCheck.setSelected(false);
    }
    
    private void handleTransitionNameChange() {
        Transition selected = Transition.getSelectedTransition();
        if (selected != null) {
            String newSymbol = transitionNameCombo.getValue();
            if (newSymbol != null && !newSymbol.equals(selected.getSymbol())) {
                selected.setSymbol(newSymbol); 
                transitionNameCombo.setValue(selected.getSymbol());
            }
        }
    }

    private void setupTransitionTable() {
        stateColumn.setCellValueFactory(cellData -> {
            State state = cellData.getValue();
            return new SimpleStringProperty(state != null ? state.getName() : "");
        });
        stateColumn.setStyle("-fx-alignment: CENTER;");

        transitionsParentColumn.getColumns().clear();
        transitionsParentColumn.setStyle("-fx-alignment: CENTER;");
    }

    public void updateTransitionTable() {
        Platform.runLater(() -> {
            if (dfa == null || dfaTransitionTable == null || stateColumn == null || transitionsParentColumn == null) {
                return;
            }

            dfaTransitionTable.getItems().clear();
            transitionsParentColumn.getColumns().clear();

            Set<String> alphabet = dfa.getAlphabet();
            Set<State> states = dfa.getStates();

            if (alphabet == null || states == null) {
                 return;
            }

            List<String> sortedAlphabet = new ArrayList<>(alphabet);
            sortedAlphabet.removeIf(s -> s == null || s.trim().isEmpty() || s.equals("?"));

            for (String symbol : sortedAlphabet) {

                TableColumn<State, String> symbolColumn = new TableColumn<>(symbol);
                symbolColumn.setCellValueFactory(cellData -> {
                    State currentState = cellData.getValue();
                    String destinationStateName = "-";

                    if (currentState != null) {
                        Transition transition = currentState.getTransition(symbol);
                        if (transition != null && transition.getNextState() != null) {
                            destinationStateName = transition.getNextState().getName();
                        }
                    }
                    return new SimpleStringProperty(destinationStateName);
                });
                symbolColumn.setPrefWidth(75);
                symbolColumn.setStyle("-fx-alignment: CENTER;");

                transitionsParentColumn.getColumns().add(symbolColumn);
            }

            ObservableList<State> observableStateList = FXCollections.observableArrayList(states);
            dfaTransitionTable.setItems(observableStateList);
        });
    }

    public void rebuildUIFromMinimizedDFA() {
        throw new UnsupportedOperationException("Unimplemented method 'rebuildUIFromMinimizedDFA'");
    }

    public void updateComboBoxes() {
        if (dfa == null || fromStateCombo == null || toStateCombo == null) {
            return;
        }

        Set<State> currentStates = dfa.getStates();
        List<String> stateNames = currentStates.stream()
                                        .map(State::getName)
                                        .filter(name -> name != null && !name.isEmpty())
                                        .sorted()
                                        .collect(Collectors.toList());
        
        ObservableList<String> observableStateNames = FXCollections.observableArrayList(stateNames);

        String selectedFrom = fromStateCombo.getSelectionModel().getSelectedItem();
        String selectedTo = toStateCombo.getSelectionModel().getSelectedItem();

        fromStateCombo.setItems(observableStateNames);
        toStateCombo.setItems(observableStateNames);

        if (stateNames.contains(selectedFrom)) {
            fromStateCombo.getSelectionModel().select(selectedFrom);
        }
        if (stateNames.contains(selectedTo)) {
            toStateCombo.getSelectionModel().select(selectedTo);
        }
    }

    private void showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void deleteTransition(Transition transition) {
        if (transition == null) {
            return;
        }

        if (transition == Transition.getSelectedTransition()) {
             Transition.getSelectedTransition().deselect();
             clearTransitionSettingsTab();
        }

        if (pane != null) {
            pane.getChildren().remove(transition);
        }

        State fromState = transition.getFromState();
        if (fromState != null) {
            fromState.removeTransition(transition);
        }

        updateUIComponents(); 
    }

    public void setLogTextArea(TextArea logTextArea) {
        this.logTextArea = logTextArea;
    }

    public Pane getPane() {
        return pane;
    }
}
