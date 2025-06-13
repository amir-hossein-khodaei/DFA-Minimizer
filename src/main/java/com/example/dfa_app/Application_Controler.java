package com.example.dfa_app;
import com.example.dfa_app.DFA.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



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
    private Button saveButton;
    @FXML
    private Button newPageButton;
    @FXML
    private Button newStateButton;
    @FXML
    private Button clearPageButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button redoButton;

    private DFA dfa;
    private Transition currentTransition;
    private boolean isCreatingTransition = false;
    private boolean isPlacingNewState = false;

    // Handlers for dynamic mouse events
    private EventHandler<MouseEvent> statePlacementClickHandler;
    private EventHandler<MouseEvent> statePlacementMouseMoveHandler;
    private EventHandler<MouseEvent> transitionCreationMouseMoveHandler;

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
        System.out.println("Application_Controler initialized.");
        dfa = new DFA(this);

        setupTransitionTable();
        updateTransitionTable();

        // --- Button Setup ---
        if (startProcessButton != null) {
            startProcessButton.setText("Minimize DFA");
            startProcessButton.setOnAction(event -> {
                if (dfa != null) {
                    dfa.minimizeDFA();
                }
            });
        }

        if (saveButton != null) {
            saveButton.setOnAction(event -> handleSaveButton());
        }

        if (clearPageButton != null) {
            clearPageButton.setOnAction(event -> handleClearPageButton());
        }

        if (newPageButton != null) {
            newPageButton.setOnAction(event -> handleNewPageButton());
        }

        if (newStateButton != null) {
            newStateButton.setOnAction(event -> handleNewStateButton());
        }

        // --- Event Handlers Setup ---
        pane.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handlePaneClick);
        BorderPane.setOnKeyPressed(this::handleKeyPress);

        // State settings tab listeners
        stateNameTextField.setOnAction(event -> handleStateNameChange());
        stateNameTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                handleStateNameChange();
            }
        });
        startStateCheck.setOnAction(event -> handleStartStateCheck());
        acceptingStateCheck.setOnAction(event -> handleAcceptingStateCheck());

        // Transition settings tab listeners
        transitionNameCombo.setOnAction(event -> handleTransitionNameChange());
        transitionNameCombo.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                handleTransitionNameChange();
            }
        });

        // Tab selection listener
        TabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == stateSettingsTab) {
                updateStateSettingsTab();
            } else if (newTab == transitionSettingsTab) {
                Transition selected = Transition.getSelectedTransition();
                updateTransitionSettingsTab(selected);
            }
        });

        updateUIComponents();
    }

    /**
     * Handles all key presses on the main pane.
     */
    private void handleKeyPress(KeyEvent event) {
        System.out.println("Key Pressed: " + event.getCode());
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case D:
                    System.out.println("Ctrl+D pressed: Deleting selected object.");
                    deleteSelectedObject();
                    event.consume();
                    break;
                case N:
                    System.out.println("Ctrl+N pressed: Creating new state.");
                    handleNewStateButton();
                    event.consume();
                    break;
                case R:
                    System.out.println("Ctrl+R pressed: Minimizing DFA.");
                    startProcessButton.fire();
                    event.consume();
                    break;
                case Z:
                case Y:
                case S:
                case O:
                case K:
                    System.out.println("Ctrl+" + event.getCode() + " pressed: Consuming event.");
                    event.consume();
                    break;
            }
        } else if (event.getCode() == KeyCode.ENTER) {
            if (isPlacingNewState) {
                System.out.println("Enter pressed: Finalizing state placement.");
                State stateToPlace = findStateBeingPlaced();
                if (stateToPlace != null) {
                    finalizeNewStatePlacement(stateToPlace);
                }
                event.consume();
            }
        }
    }

    /**
     * Handles all mouse clicks on the main drawing pane.
     */
    private void handlePaneClick(MouseEvent mouseEvent) {
        System.out.println("Pane Clicked at X: " + mouseEvent.getX() + ", Y: " + mouseEvent.getY() + ", Button: " + mouseEvent.getButton());
        if (isPlacingNewState) {
            System.out.println("In state placement mode, deferring to specific handler.");
            return;
        }

        Node clickedNode = mouseEvent.getPickResult().getIntersectedNode();
        Object userData = findUserData(clickedNode);
        System.out.println("Clicked Node: " + clickedNode + ", UserData: " + (userData != null ? userData.getClass().getSimpleName() : "null"));

        if (isCreatingTransition) {
            System.out.println("Completing transition creation.");
            completeTransitionCreation(clickedNode);
            mouseEvent.consume();
            return;
        }

        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            if (userData instanceof State) {
                System.out.println("Right-clicked on State: " + ((State) userData).getName());
                ((State) userData).select();
                mouseEvent.consume();
            } else if (userData instanceof Transition) {
                System.out.println("Right-clicked on Transition.");
                ((Transition) userData).select();
                mouseEvent.consume();
            }
            return;
        }

        if (mouseEvent.isControlDown() && mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (userData instanceof State) {
                System.out.println("Ctrl+Primary click on State: Starting transition creation.");
                startTransitionCreation((State) userData);
                mouseEvent.consume();
            }
            return;
        }

        if (!mouseEvent.isConsumed() && clickedNode == pane) {
            System.out.println("Clicked on pane: Deselecting all.");
            deselectAll();
            mouseEvent.consume();
        }
    }

    /**
     * Starts the two-click process of creating a new transition.
     */
    private void startTransitionCreation(State fromState) {
        System.out.println("Starting transition creation from state: " + fromState.getName());
        isCreatingTransition = true;
        currentTransition = new Transition(fromState, this.dfa, this);

        transitionCreationMouseMoveHandler = moveEvent -> {
            if (currentTransition != null) {
                currentTransition.setTempEnd(moveEvent.getX(), moveEvent.getY());
            }
            moveEvent.consume();
        };
        pane.setOnMouseMoved(transitionCreationMouseMoveHandler);
    }

    /**
     * Completes the transition creation process on the second click.
     */
    private void completeTransitionCreation(Node clickedNode) {
        System.out.println("Completing transition creation.");
        pane.setOnMouseMoved(null);
        transitionCreationMouseMoveHandler = null;
        isCreatingTransition = false;

        deselectAll();

        Object userData = findUserData(clickedNode);
        if (userData instanceof State) {
            State targetState = (State) userData;
            System.out.println("Transition target state: " + targetState.getName());
            if (currentTransition != null) {
                currentTransition.completeTransition(targetState);
                currentTransition.getFromState().addTransition(currentTransition);

                dfa.addState(currentTransition.getFromState());
                dfa.addState(targetState);
                dfa.addOrUpdateTransition(currentTransition.getFromState(), currentTransition.getSymbol(), targetState);

                updateUIComponents();
                System.out.println("Transition created: " + currentTransition.getFromState().getName() + " --(" + currentTransition.getSymbol() + ")--> " + currentTransition.getNextState().getName());
            }
        } else {
            System.out.println("Second click not on a state. Cancelling transition creation.");
            if (currentTransition != null) {
                pane.getChildren().remove(currentTransition);
            }
        }
        currentTransition = null;
    }

    /**
     * Handles the "New State" button action, entering state placement mode.
     */
    @FXML
    private void handleNewStateButton() {
        System.out.println("New State button clicked. Entering placement mode.");
        if (isPlacingNewState) {
            System.out.println("Already in state placement mode. Ignoring.");
            return;
        }
        isPlacingNewState = true;

        State newState = new State(-100, -100, 30, Color.WHITE, Color.BLACK, 1.0, this);
        newState.setSelectionListener(this);
        pane.getChildren().add(newState);
        newState.select();

        statePlacementMouseMoveHandler = moveEvent -> {
            newState.moveState(moveEvent.getX(), moveEvent.getY());
            moveEvent.consume();
        };
        pane.setOnMouseMoved(statePlacementMouseMoveHandler);

        statePlacementClickHandler = mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                System.out.println("Primary click to finalize new state placement.");
                finalizeNewStatePlacement(newState);
                mouseEvent.consume();
            }
        };
        pane.setOnMouseClicked(statePlacementClickHandler);

        if (newState.getEditableLabel().getEditor() != null) {
            newState.getEditableLabel().getEditor().setOnAction(actionEvent -> {
                System.out.println("Enter pressed on state name editor: Finalizing placement.");
                finalizeNewStatePlacement(newState);
                actionEvent.consume();
            });
        }
    }

    /**
     * Finalizes the position of a new state, checking for overlaps and adding it to the DFA.
     */
    private void finalizeNewStatePlacement(State newState) {
        System.out.println("Finalizing new state placement for state: " + newState.getName());
        pane.setOnMouseMoved(null);
        pane.setOnMouseClicked(null);
        if (newState.getEditableLabel().getEditor() != null) {
            newState.getEditableLabel().getEditor().setOnAction(null);
        }
        statePlacementMouseMoveHandler = null;
        statePlacementClickHandler = null;

        if (isOverlapping(newState)) {
            System.out.println("New state overlaps with existing state. Cancelling placement.");
            showOverlapError();
            pane.getChildren().remove(newState);
            isPlacingNewState = false;
            return;
        }

        deselectAll();
        newState.deselect();

        dfa.addState(newState);
        updateUIComponents();
        isPlacingNewState = false;
        System.out.println("New state placed: " + newState.getName() + " at (" + newState.getLayoutX() + ", " + newState.getLayoutY() + ")");
    }


    @Override
    public void onSelected(Object selectedObject) {
        System.out.println("Object selected: " + (selectedObject != null ? selectedObject.getClass().getSimpleName() : "null"));
        if (selectedObject instanceof State) {
            if (Transition.getSelectedTransition() != null) {
                System.out.println("Deselecting previously selected transition.");
                Transition.getSelectedTransition().deselect();
            }
            updateStateSettingsTab();
            clearTransitionSettingsTab();
            TabPane.getSelectionModel().select(stateSettingsTab);
            System.out.println("Selected object is State: " + ((State) selectedObject).getName());
        } else if (selectedObject instanceof Transition) {
            if (State.getSelectedState() != null) {
                System.out.println("Deselecting previously selected state.");
                State.getSelectedState().deselect();
            }
            updateTransitionSettingsTab((Transition) selectedObject);
            clearStateSettingsTab();
            TabPane.getSelectionModel().select(transitionSettingsTab);
            System.out.println("Selected object is Transition: " + ((Transition) selectedObject).getSymbol());
        } else {
            System.out.println("Selected object is neither State nor Transition. Clearing tabs.");
            clearStateSettingsTab();
            clearTransitionSettingsTab();
        }
    }

    @Override
    public void onDeselected(Object deselectedObject) {
        System.out.println("Object deselected: " + (deselectedObject != null ? deselectedObject.getClass().getSimpleName() : "null"));
        clearStateSettingsTab();
        clearTransitionSettingsTab();
    }

    private void updateStateSettingsTab() {
        System.out.println("Updating State Settings Tab.");
        State selected = State.getSelectedState();
        if (selected != null) {
            stateNameTextField.setText(selected.getName());
            startStateCheck.setSelected(selected.isInitial());
            acceptingStateCheck.setSelected(selected.isAccepting());
            System.out.println("State settings updated for: " + selected.getName());
        } else {
            clearStateSettingsTab();
            System.out.println("No state selected, clearing state settings tab.");
        }
    }

    private void updateTransitionSettingsTab(Transition selected) {
        System.out.println("Updating Transition Settings Tab.");
        if (selected != null) {
            String fromName = selected.getFromState().getName();
            String toName = (selected.getNextState() != null) ? selected.getNextState().getName() : null;
            String symbolName = selected.getSymbol();

            fromStateCombo.getSelectionModel().select(fromName);
            toStateCombo.getSelectionModel().select(toName);
            transitionNameCombo.setValue(symbolName);

            // From/To states cannot be changed from the settings panel
            fromStateCombo.setDisable(true);
            toStateCombo.setDisable(true);
            System.out.println("Transition settings updated for transition: " + symbolName + " from " + fromName + " to " + toName);
        } else {
            clearTransitionSettingsTab();
            System.out.println("No transition selected, clearing transition settings tab.");
        }
    }

    private void clearStateSettingsTab() {
        System.out.println("Clearing State Settings Tab.");
        stateNameTextField.setText("");
        startStateCheck.setSelected(false);
        acceptingStateCheck.setSelected(false);
    }

    private void clearTransitionSettingsTab() {
        System.out.println("Clearing Transition Settings Tab.");
        fromStateCombo.getSelectionModel().clearSelection();
        toStateCombo.getSelectionModel().clearSelection();
        transitionNameCombo.setValue("");
        fromStateCombo.setDisable(false);
        toStateCombo.setDisable(false);
    }

    private void handleStateNameChange() {
        System.out.println("Handling state name change.");
        State selected = State.getSelectedState();
        String newName = stateNameTextField.getText().trim();
        if (selected != null && !newName.isEmpty() && !newName.equals(selected.getName())) {
            System.out.println("Attempting to change state name from " + selected.getName() + " to " + newName);
            selected.setName(newName);
            // If the name was invalid (e.g., duplicate), UI will revert it.
            if (!selected.getName().equals(newName)) {
                stateNameTextField.setText(selected.getName());
                System.out.println("State name change failed, reverted to: " + selected.getName());
            } else {
                System.out.println("State name changed successfully to: " + newName);
            }
        } else if (selected == null) {
            System.out.println("No state selected for name change.");
        } else if (newName.isEmpty()) {
            System.out.println("New state name is empty.");
        } else if (newName.equals(selected.getName())) {
            System.out.println("New state name is the same as old name.");
        }
    }

    private void handleStartStateCheck() {
        System.out.println("Handling start state checkbox.");
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = startStateCheck.isSelected();
            if (isChecked) {
                System.out.println("Setting state " + selected.getName() + " as initial.");
                dfa.setInitialState(selected);
            } else {
                // Prevent deselecting the only initial state
                if (selected.equals(dfa.getInitialState())) {
                    startStateCheck.setSelected(true);
                    showAlert(Alert.AlertType.INFORMATION, "Initial State", "Cannot deselect the initial state. Select another state to be the new initial state.");
                    System.out.println("Cannot deselect initial state: " + selected.getName());
                } else {
                    System.out.println("Deselecting initial state (not implemented yet).");
                }
            }
        } else {
            System.out.println("No state selected for start state change.");
        }
    }

    private void handleAcceptingStateCheck() {
        System.out.println("Handling accepting state checkbox.");
        State selected = State.getSelectedState();
        if (selected != null) {
            boolean isChecked = acceptingStateCheck.isSelected();
            if (isChecked) {
                System.out.println("Adding state " + selected.getName() + " to accepting states.");
                dfa.addAcceptingState(selected);
            } else {
                System.out.println("Removing state " + selected.getName() + " from accepting states.");
                dfa.removeAcceptingState(selected);
            }
        } else {
            System.out.println("No state selected for accepting state change.");
        }
    }

    private void handleTransitionNameChange() {
        System.out.println("Handling transition name change.");
        Transition selected = Transition.getSelectedTransition();
        if (selected != null) {
            String newSymbol = transitionNameCombo.getValue();
            if (newSymbol != null && !newSymbol.equals(selected.getSymbol())) {
                System.out.println("Attempting to change transition symbol from " + selected.getSymbol() + " to " + newSymbol);
                selected.setSymbol(newSymbol);
                // If symbol was invalid, UI will revert it.
                transitionNameCombo.setValue(selected.getSymbol());
                if (!selected.getSymbol().equals(newSymbol)) {
                    System.out.println("Transition symbol change failed, reverted to: " + selected.getSymbol());
                } else {
                    System.out.println("Transition symbol changed successfully to: " + newSymbol);
                }
            } else if (newSymbol == null) {
                System.out.println("New transition symbol is null.");
            } else if (newSymbol.equals(selected.getSymbol())) {
                System.out.println("New transition symbol is the same as old symbol.");
            }
        } else {
            System.out.println("No transition selected for symbol change.");
        }
    }

    public void updateUIComponents() {
        System.out.println("Updating UI Components.");
        Platform.runLater(() -> {
            updateTransitionTable();
            updateComboBoxes();
            System.out.println("UI Components updated.");
        });
    }

    private void setupTransitionTable() {
        System.out.println("Setting up Transition Table.");
        stateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        stateColumn.setStyle("-fx-alignment: CENTER;");
        transitionsParentColumn.setStyle("-fx-alignment: CENTER;");
    }

    public void updateTransitionTable() {
        System.out.println("Updating Transition Table.");
        Platform.runLater(() -> {
            if (dfa == null || dfaTransitionTable == null) {
                System.out.println("DFA or dfaTransitionTable is null, cannot update table.");
                return;
            }

            dfaTransitionTable.getItems().clear();
            transitionsParentColumn.getColumns().clear();

            Set<String> alphabet = dfa.getAlphabet();
            Set<State> states = dfa.getStates();

            if (alphabet == null || states == null) {
                System.out.println("Alphabet or states is null, cannot update table.");
                return;
            }

            // Get a sorted list of non-empty alphabet symbols
            List<String> sortedAlphabet = new ArrayList<>(alphabet);
            sortedAlphabet.removeIf(s -> s == null || s.trim().isEmpty() || s.equals("?"));
            Collections.sort(sortedAlphabet);

            // Create a column for each symbol
            for (String symbol : sortedAlphabet) {
                TableColumn<State, String> symbolColumn = new TableColumn<>(symbol);
                symbolColumn.setCellValueFactory(cellData -> {
                    State currentState = cellData.getValue();
                    String destinationStateName = "-"; // Default display

                    Transition transition = currentState.getTransition(symbol);
                    if (transition != null && transition.getNextState() != null) {
                        destinationStateName = transition.getNextState().getName();
                    } else {
                    }
                    return new SimpleStringProperty(destinationStateName);
                });
                symbolColumn.setPrefWidth(75);
                symbolColumn.setStyle("-fx-alignment: CENTER;");
                transitionsParentColumn.getColumns().add(symbolColumn);
                System.out.println("Added column for symbol: " + symbol);
            }

            dfaTransitionTable.setItems(FXCollections.observableArrayList(states));
            System.out.println("Transition table updated with " + states.size() + " states.");
        });
    }

    public void updateComboBoxes() {
        System.out.println("Updating ComboBoxes.");
        if (dfa == null) {
            System.out.println("DFA is null, cannot update combo boxes.");
            return;
        }

        List<String> stateNames = dfa.getStates().stream()
                .map(State::getName)
                .filter(name -> name != null && !name.isEmpty())
                .sorted()
                .collect(Collectors.toList());

        ObservableList<String> observableStateNames = FXCollections.observableArrayList(stateNames);

        // Preserve selection if possible
        String selectedFrom = fromStateCombo.getSelectionModel().getSelectedItem();
        String selectedTo = toStateCombo.getSelectionModel().getSelectedItem();

        fromStateCombo.setItems(observableStateNames);
        toStateCombo.setItems(observableStateNames);

        fromStateCombo.getSelectionModel().select(selectedFrom);
        toStateCombo.getSelectionModel().select(selectedTo);
        System.out.println("ComboBoxes updated with " + stateNames.size() + " state names.");
    }

    private void deleteSelectedObject() {
        System.out.println("Attempting to delete selected object.");
        Transition selectedTransition = Transition.getSelectedTransition();
        State selectedState = State.getSelectedState();

        if (selectedTransition != null) {
            System.out.println("Deleting selected transition.");
            deleteTransition(selectedTransition);
        } else if (selectedState != null) {
            System.out.println("Deleting selected state: " + selectedState.getName());
            selectedState.deleteState();
        } else {
            System.out.println("No object selected for deletion.");
        }
    }

    public void deleteTransition(Transition transition) {
        System.out.println("Deleting transition: " + transition.getSymbol() + " from " + transition.getFromState().getName());
        if (transition == null) return;

        if (transition == Transition.getSelectedTransition()) {
            transition.deselect();
            clearTransitionSettingsTab();
        }

        pane.getChildren().remove(transition);
        transition.getFromState().removeTransition(transition);

        updateUIComponents();
        System.out.println("Transition deleted.");
    }

    @FXML
    private void handleClearPageButton() {
        System.out.println("Clear Page button clicked. Clearing DFA and pane.");
        dfa.clear();
        pane.getChildren().clear();
        updateUIComponents();
        System.out.println("Page cleared.");
    }

    @FXML
    private void handleNewPageButton() {
        System.out.println("New Page button clicked. Opening new window.");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Main_DFA.fxml"));
            Parent root = fxmlLoader.load();
            Application_Controler newController = fxmlLoader.getController();
            newController.setLogTextArea(new TextArea());

            Stage stage = new Stage();
            stage.setTitle("DFA Application - New Page");
            stage.setScene(new Scene(root, 1280, 720));
            stage.show();
            System.out.println("Opened new DFA window.");
        } catch (IOException e) {
            System.err.println("Failed to open new window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleSaveButton() {
        System.out.println("Save button clicked. Attempting to save DFA.");
        if (dfa != null) {
            Window ownerWindow = pane.getScene().getWindow();
            DFAI_O.saveDFA(dfa, ownerWindow);
            System.out.println("DFA saved successfully.");
        } else {
            System.out.println("DFA is null, cannot save.");
        }
    }

    public void handleInitialStateChange(State oldInitial, State newInitial) {
        System.out.println("Handling initial state change. Old: " + (oldInitial != null ? oldInitial.getName() : "null") + ", New: " + (newInitial != null ? newInitial.getName() : "null"));
        // Update UI if the selected state was affected
        State currentlySelected = State.getSelectedState();
        if (currentlySelected != null && (currentlySelected.equals(oldInitial) || currentlySelected.equals(newInitial))) {
            updateStateSettingsTab();
            System.out.println("Selected state was affected by initial state change, updating tab.");
        }
    }

    public void handleAcceptingStateChange(State changedState) {
        System.out.println("Handling accepting state change for state: " + (changedState != null ? changedState.getName() : "null"));
        // Update UI if the selected state was the one that changed
        if (changedState != null && changedState.equals(State.getSelectedState())) {
            updateStateSettingsTab();
            System.out.println("Selected state was changed, updating tab.");
        }
    }

    public Pane getPane() {
        System.out.println("getPane() called.");
        return pane;
    }

    public void setLogTextArea(TextArea logTextArea) {
        System.out.println("setLogTextArea() called.");
        this.logTextArea = logTextArea;
    }

    // --- Helper Methods ---

    private void deselectAll() {
        System.out.println("Deselecting all objects.");
        if (State.getSelectedState() != null) {
            State.getSelectedState().deselect();
        }
        if (Transition.getSelectedTransition() != null) {
            Transition.getSelectedTransition().deselect();
        }
    }

    private State findStateBeingPlaced() {
        System.out.println("Finding state being placed.");
        for (Node node : pane.getChildren()) {
            if (node instanceof State && ((State) node).isSelected()) {
                System.out.println("Found state being placed: " + ((State) node).getName());
                return (State) node;
            }
        }
        System.out.println("No state being placed found.");
        return null;
    }

    private Object findUserData(Node startNode) {
        System.out.println("Finding user data for node: " + startNode.getClass().getSimpleName());
        Node current = startNode;
        while (current != null) {
            if (current.getUserData() != null) {
                System.out.println("Found user data: " + current.getUserData().getClass().getSimpleName());
                return current.getUserData();
            }
            current = current.getParent();
        }
        System.out.println("No user data found.");
        return null;
    }

    private boolean isOverlapping(State newState) {
        System.out.println("Checking for overlap with new state: " + newState.getName());
        double x1 = newState.getLayoutX();
        double y1 = newState.getLayoutY();
        double r1 = newState.getMainCircle().getRadius();

        for (Node node : pane.getChildren()) {
            if (node instanceof State && node != newState) {
                State otherState = (State) node;
                double x2 = otherState.getLayoutX();
                double y2 = otherState.getLayoutY();
                double r2 = otherState.getMainCircle().getRadius();
                double distance = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));

                // Check if the distance between centers is less than the sum of radii
                if (distance < (r1 + r2)) {
                    System.out.println("Overlap detected between " + newState.getName() + " and " + otherState.getName());
                    return true;
                }
            }
        }
        System.out.println("No overlap detected.");
        return false;
    }

    private void showOverlapError() {
        System.out.println("Showing overlap error alert.");
        showAlert(Alert.AlertType.ERROR, "Invalid State Placement", "The state overlaps with an existing state. Please reposition it.");
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        System.out.println("Showing alert: " + title + " - " + content);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}