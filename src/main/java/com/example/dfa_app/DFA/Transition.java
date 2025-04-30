package com.example.dfa_app.DFA;

import com.example.dfa_app.SelectionListener;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert.AlertType;
import com.example.dfa_app.Application_Controler;
import java.util.Objects; // Ensure Objects is imported

// - Represents a transition between DFA states
// - Manages visual representation (arrow) and interaction
// - Handles symbol labeling and selection/deselection
public class Transition extends Group implements simularity {

    private final State fromState;
    private State toState;
    private final CurvedArrow curvedArrow;
    private final EditableLabel editableLabel;
    private boolean complete = false;
    private String symbol = ""; // Initialize symbol
    
    // - Stores the offset factor for control point positioning
    private double persistentControlOffsetFactor = 0; 

    // - Used during interactive drawing
    private double tempEndX;
    private double tempEndY;

    // - Offset for perpendicular control point positioning
    private static final double CONTROL_OFFSET = 40.0;
    private SelectionListener selectionListener;
    private static Transition selectedTransition = null; // Track selected transition
    private DFA dfaInstance; // Reference to the main DFA object
    private Application_Controler controllerInstance; // Reference to the controller for table updates
    private ChangeListener<Boolean> editorFocusListener;
    
    private static final String LOG_DEBUG = "[DEBUG] ";
    private static final String LOG_WARN = "[WARN] ";
    
    // - Create a new transition from a source state
    // - Initialize with default positioning and visuals
    public Transition(State fromState, DFA dfa, Application_Controler controller) {
        if (fromState == null) {
            throw new IllegalArgumentException("fromState cannot be null.");
        }
        this.fromState = fromState;
        this.dfaInstance = dfa; // Store reference
        this.controllerInstance = controller; // Store reference

        this.curvedArrow = new CurvedArrow();
        this.editableLabel = new EditableLabel();
        
        editableLabel.setVisible(false);

        // - Enable key events
        this.setFocusTraversable(true);

        // - Add visual components
        getChildren().addAll(curvedArrow, editableLabel);

        // - Set user data for event handling
        this.setUserData(this); // Set on the Transition group itself

        // - Add to parent container if possible
        if (fromState.getParent() instanceof Pane) {
            ((Pane) fromState.getParent()).getChildren().add(this);
        }

        // - Calculate initial random offset for control point
        this.persistentControlOffsetFactor = CONTROL_OFFSET + (int) (Math.random() * 121) - 60;

        // - Set initial control position based on offset
        double initialControlX = 0, initialControlY = 0;
        double toX = fromState.getLayoutX() + 50; // Arbitrary initial direction
        double toY = fromState.getLayoutY(); 
        double dx = toX - fromState.getLayoutX(); double dy = toY - fromState.getLayoutY();
        double distance = Math.hypot(dx, dy); if (distance == 0) { distance = 1; }
        double fromRadius = fromState.getMainCircle().getRadius();
        double startX = fromState.getLayoutX() + (dx / distance) * fromRadius;
        double startY = fromState.getLayoutY() + (dy / distance) * fromRadius;
        double endX = toX; // Use arbitrary end for initial calculation
        double endY = toY;
        double midX = (startX + endX) / 2.0; double midY = (startY + endY) / 2.0;
        double norm = distance; double perpX = -dy / norm; double perpY = dx / norm;
        initialControlX = midX + persistentControlOffsetFactor * perpX; 
        initialControlY = midY + persistentControlOffsetFactor * perpY;
        this.curvedArrow.setControl(initialControlX, initialControlY); // Set initial control pos

        // - Listen for source state position changes
        InvalidationListener layoutListener = obs -> updateTransition();
        fromState.layoutXProperty().addListener(layoutListener);
        fromState.layoutYProperty().addListener(layoutListener);

        updateTransition();
    }

    // - Access the editable label component
    public EditableLabel getEditableLabel() {
        return editableLabel;
    }

    // - Get the source state
    public State getFromState() {
        return fromState;
    }

    // - Access the arrow component
    public CurvedArrow getCurvedArrow() {
        return curvedArrow;
    }

    // - Update endpoint during drawing phase
    public void setTempEnd(double x, double y) {
        this.tempEndX = x;
        this.tempEndY = y;
        updateTransition();
    }

    // - Recalculate and update arrow geometry
    // - Handles different cases: complete, self-loop, or in-progress
    private void updateTransition() {
        double fromX = fromState.getLayoutX();
        double fromY = fromState.getLayoutY();
        // - Variables for positioning calculations
        double startX, startY, endX, endY, controlX, controlY; 

        if (complete && toState != null) {
            if (fromState == toState) {
                // - Handle self-loop transition geometry
                double radius = fromState.getMainCircle().getRadius();
                startX = fromX; startY = fromY - radius;
                endX = fromX + radius; endY = fromY;
                 // - Set control point for self-loop
                 controlX = fromX;
                 controlY = fromY - radius * 2.5; 
                 // - Update control point if needed
                 if (curvedArrow.getControlX() != controlX || curvedArrow.getControlY() != controlY) {
                     curvedArrow.setControl(controlX, controlY);
                 }
            } else {
                 // - Calculate points for standard transition between states
                double toX = toState.getLayoutX(); double toY = toState.getLayoutY();
                double dx = toX - fromX; double dy = toY - fromY;
                double distance = Math.hypot(dx, dy); if (distance == 0) { distance = 1; }
                double fromRadius = fromState.getMainCircle().getRadius();
                startX = fromX + (dx / distance) * fromRadius;
                startY = fromY + (dy / distance) * fromRadius;
                double toRadius = toState.getMainCircle().getRadius();
                endX = toX - (dx / distance) * toRadius;
                endY = toY - (dy / distance) * toRadius;

                // - Recalculate control point based on persistent offset
                double midX = (startX + endX) / 2.0; double midY = (startY + endY) / 2.0;
                double norm = distance; double perpX = -dy / norm; double perpY = dx / norm;
                controlX = midX + persistentControlOffsetFactor * perpX; 
                controlY = midY + persistentControlOffsetFactor * perpY;
                // - Update the arrow's control point
                curvedArrow.setControl(controlX, controlY); 
            }
        } else {
             // - Handle incomplete transition geometry (following mouse)
             double dx = tempEndX - fromX;
             double dy = tempEndY - fromY;
             double distance = Math.hypot(dx, dy);
             if (distance == 0) { distance = 1; }
             double fromRadius = fromState.getMainCircle().getRadius();
             startX = fromX + (dx / distance) * fromRadius;
             startY = fromY + (dy / distance) * fromState.getMainCircle().getRadius();
             endX = tempEndX;
             endY = tempEndY;
             // - Use existing control point for incomplete transitions
             controlX = curvedArrow.getControlX();
             controlY = curvedArrow.getControlY();
        }

        // - Update the arrow endpoints
        curvedArrow.setStart(startX, startY);
        curvedArrow.setEnd(endX, endY);
        // - Update arrowhead
        curvedArrow.updateArrowHead(); 

        if (complete) {
            // - Update label positioning for completed transition
            updateLabelPosition(startX, startY, endX, endY, controlX, controlY);
        }
    }

    // - Calculate and set the position for the transition label
    private void updateLabelPosition(double startX, double startY, double endX, double endY, double controlX, double controlY) {
        if (!complete || editableLabel == null) return; // Don't update if not complete or label is missing

        try {
             boolean initialVisibility = editableLabel.isVisible();
             // Ensure label is temporarily visible to calculate its bounds accurately
             if (!initialVisibility) editableLabel.setVisible(true); 
             editableLabel.applyCss(); editableLabel.layout(); // Force layout calculation
             double labelWidth = editableLabel.getLabelWidth(); double labelHeight = editableLabel.getLabelHeight();
             if (!initialVisibility) editableLabel.setVisible(false); // Restore original visibility

             // If bounds are zero, maybe visibility trick didn't work, use defaults or log error
             if (labelWidth <= 0 || labelHeight <= 0) { 
                 // System.err.println("[Transition Update] Warning: Could not get valid label bounds. Using estimated position.");
                 // Fallback or alternative positioning might be needed here
                 labelWidth = 30; // Estimate
                 labelHeight = 15; // Estimate
             }

            // Calculate label position based on the provided curve points
            double midCurveX = 0.25 * startX + 0.5 * controlX + 0.25 * endX;
            double midCurveY = 0.25 * startY + 0.5 * controlY + 0.25 * endY;
             double tangentX = endX - startX; double tangentY = endY - startY;
             double tangentLength = Math.hypot(tangentX, tangentY);
             double normPerpX = 0; double normPerpY = 1; // Default perpendicular if tangent is zero
             if (tangentLength > 1e-6) { normPerpX = -tangentY / tangentLength; normPerpY = tangentX / tangentLength; }
             double LABEL_MID_OFFSET = 15.0;
             double labelCenterX = midCurveX + normPerpX * LABEL_MID_OFFSET;
             double labelCenterY = midCurveY + normPerpY * LABEL_MID_OFFSET;
             double labelX = labelCenterX - labelWidth / 2.0; double labelY = labelCenterY - labelHeight / 2.0;
             editableLabel.setLabelPosition(labelX, labelY);
             editableLabel.setEditorPosition(labelX, labelY);
        } catch (Exception e) { 
             // System.err.println("[Transition Update] Error during synchronous label position update: " + e.getMessage()); 
             // e.printStackTrace(); 
        }
    }

    // - Complete transition by connecting to target state
    // - Set up editing for transition symbol
    public void completeTransition(State targetState) {
        if (targetState == null) {
            throw new IllegalArgumentException("Target state cannot be null.");
        }
        this.toState = targetState;
        this.complete = true;
        curvedArrow.setComplete(true);
        
        // Calculate the initial control position based on the persistent factor
        // Ensure control point is set according to persistent factor
        double controlX = 0, controlY = 0; 
        if (fromState != toState) {
            double fromX = fromState.getLayoutX(); double fromY = fromState.getLayoutY();
            double toX = toState.getLayoutX(); double toY = toState.getLayoutY();
            double dx = toX - fromX; double dy = toY - fromY;
            double distance = Math.hypot(dx, dy); if (distance == 0) { distance = 1; }
            double fromRadius = fromState.getMainCircle().getRadius();
            double startX = fromX + (dx / distance) * fromRadius;
            double startY = fromY + (dy / distance) * fromRadius;
            double toRadius = toState.getMainCircle().getRadius();
            double endX = toX - (dx / distance) * toRadius;
            double endY = toY - (dy / distance) * toRadius;
            double midX = (startX + endX) / 2.0; double midY = (startY + endY) / 2.0;
            double norm = distance; double perpX = -dy / norm; double perpY = dx / norm;
            controlX = midX + persistentControlOffsetFactor * perpX; 
            controlY = midY + persistentControlOffsetFactor * perpY;
        } else { 
             double radius = fromState.getMainCircle().getRadius();
             controlX = fromState.getLayoutX();
             controlY = fromState.getLayoutY() - radius * 2.5;
        }
        this.curvedArrow.setControl(controlX, controlY);
        // System.out.printf("[CompleteTransition] Ensured control point at (%.1f, %.1f)%n", controlX, controlY);
        
        InvalidationListener toListener = obs -> updateTransition();
        toState.layoutXProperty().addListener(toListener);
        toState.layoutYProperty().addListener(toListener);
        editableLabel.setVisible(true);
        editableLabel.setText(this.symbol);
        updateTransition(); 

        // Select to make control point visible and start editing
        this.select(); 
    }

    // - Handle label editing completion with validation
    private boolean handleLabelEditComplete() {
        System.out.println(LOG_DEBUG + "Transition handleLabelEditComplete(): Handling label edit...");
        if (editableLabel == null || editableLabel.getEditor() == null) {
            System.out.println(LOG_WARN + "Transition handleLabelEditComplete(): Label or Editor is null, cannot complete.");
            return true; // Cannot edit, so consider it vacuously successful?
        }
        
        String newSymbol = editableLabel.getText();
        String originalSymbol = this.symbol;
        
        // Check for empty/null symbol
        if (isInvalidSymbol(newSymbol)) {
            showInvalidSymbolAlert();
            editableLabel.getEditor().setText(originalSymbol); 
            Platform.runLater(() -> editableLabel.getEditor().requestFocus());
            return false; 
        }

        // Check for determinism violation (only if symbol changed)
        if (!newSymbol.equals(originalSymbol)) {
             if (fromState == null) { // Safety check
                 System.err.println(LOG_WARN + "Transition handleLabelEditComplete(): fromState is null!");
                 return false; // Cannot check determinism
             }
            for (Transition existingTransition : fromState.getTransitions()) {
                if (existingTransition != this && existingTransition.isComplete() && newSymbol.equals(existingTransition.getSymbol())) {
                    showAlert("Determinism Violation", 
                              "State '" + fromState.getName() + "' already has a transition for symbol '" + newSymbol + "'.");
                    
                    // Revert and refocus
                    editableLabel.getEditor().setText(originalSymbol);
                    Platform.runLater(() -> editableLabel.getEditor().requestFocus());
                    return false; 
                }
            }
        }

        // Validation passed 
        System.out.println(LOG_DEBUG + "Transition handleLabelEditComplete(): Validation passed.");
        this.setSymbol(newSymbol); // Updates internal symbol
        editableLabel.finalizeLabel(); // Finalizes label visual 
        
        // Update model AFTER validation and internal update
        if (!newSymbol.equals(originalSymbol)) {
             if (dfaInstance != null && fromState != null && toState != null) { 
                 dfaInstance.addOrUpdateTransition(fromState, newSymbol, toState); 
             }
             // DFA will notify controller if needed (e.g., for table update)
        }
        return true; // Indicate success
    }

    // - Set transition symbol with validation
    public void setSymbol(String proposedName) {
        // Only log if the symbol actually changes
        if (proposedName != null && !proposedName.equals(this.symbol)) {
            String oldSymbol = this.symbol; // Store old symbol
            this.symbol = proposedName;
             // System.out.println("Transition symbol changed from '" + (oldSymbol == null ? "<null>" : oldSymbol) + "' to '" + proposedName + "'");
        }
    }

    // - Get current transition symbol
    public String getSymbol() {
        return symbol;
    }

    // - Display alert dialog with given title and message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // - Get the destination state of the transition
    public State getNextState() {
        return toState;
    }

    // - Handle selection of this transition
    // - Update visual state and register event handlers
    @Override
    public void select() {
        System.out.println(LOG_DEBUG + "Transition select(): Selecting transition: " + this);
        if (State.getSelectedState() != null) State.getSelectedState().deselect();
        if (selectedTransition != null && selectedTransition != this) { selectedTransition.deselect(); }
        selectedTransition = this;
        
        if (curvedArrow != null) curvedArrow.select(); // Makes arrow blue, shows persistent control point
        registerControlPointDrag(); // Register drag for the persistent control point
        
        if (editableLabel != null) {
            editableLabel.setVisible(true); 
            editableLabel.startEditing(); 
            // Setup listener for Enter key confirmation
            if(editableLabel.getEditor() != null) {
                editableLabel.getEditor().setOnAction(event -> { 
                    System.out.println(LOG_DEBUG + "Transition select(): Enter pressed on label editor.");
                    if(handleLabelEditComplete()) { 
                        // Successfully finalized on Enter - Optionally deselect, but maybe not?
                        // this.deselect(); // Let user click away instead?
                    }
                    event.consume(); 
                });
            }
        }

        if (selectionListener != null) { selectionListener.onSelected(this); }
    }

    // - Handle deselection of this transition
    // - Validate symbol and update visual state
    @Override
    public void deselect() {
        System.out.println(LOG_DEBUG + "Transition deselect(): Attempting deselection for: " + this);

        boolean finalizeSuccess = true;
        // Only attempt finalization if the editor is currently visible and part of the scene graph
        if (editableLabel != null && editableLabel.getEditor() != null &&
            editableLabel.getEditor().isVisible() && editableLabel.getScene() != null) 
        {
             System.out.println(LOG_DEBUG + "Transition deselect(): Editor is visible, attempting finalize symbol...");
             finalizeSuccess = handleLabelEditComplete();
        } else {
            System.out.println(LOG_DEBUG + "Transition deselect(): Editor not visible or not ready, skipping finalize symbol attempt.");
        }

        if (finalizeSuccess) {
            System.out.println(LOG_DEBUG + "Transition deselect(): Finalize successful or skipped, committing deselection.");
            commitDeselection(); // Separate method for actual deselection steps
        } else {
            System.out.println(LOG_WARN + "Transition deselect(): Finalize failed (invalid symbol), keeping selected/editing.");
            // Keep selected, focus should have been requested by handleLabelEditComplete
            Platform.runLater(() -> {
                if(editableLabel != null && editableLabel.getEditor() != null) {
                     editableLabel.getEditor().requestFocus();
                 }
             });
        }
    }

    // - Check if this transition is currently selected
    public boolean isSelected() {
        return curvedArrow.isSelected();
    }

    // - Register selection event listener
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    // - Set up drag event handlers for control point
    private void registerControlPointDrag() { 
        Circle cp = curvedArrow.getControlPoint(); 
        if (cp == null) { 
            // System.err.println("Error: Control point is null in registerControlPointDrag."); 
            return; 
        } 
        final double[] dragDelta = new double[2];
        cp.setOnMousePressed((MouseEvent e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragDelta[0] = e.getX() - cp.getCenterX();
                dragDelta[1] = e.getY() - cp.getCenterY();
                e.consume();
            }
        });
        cp.setOnMouseDragged((MouseEvent e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double newLocalX = e.getX() - dragDelta[0];
                double newLocalY = e.getY() - dragDelta[1];
                // Update arrow control point directly
                curvedArrow.setControl(newLocalX, newLocalY);
                
                // Update label position based on the new control point
                // Need to get current start/end points from the arrow
                updateLabelPosition(curvedArrow.getStartX(), curvedArrow.getStartY(), 
                                  curvedArrow.getEndX(), curvedArrow.getEndY(), 
                                  newLocalX, newLocalY);
                e.consume();
            }
        });
        cp.setOnMouseReleased((MouseEvent e) -> {
             if (e.getButton() == MouseButton.PRIMARY) {
                 if (complete && fromState != toState) {
                    double currentStartX = curvedArrow.getStartX(); double currentStartY = curvedArrow.getStartY(); 
                    double currentEndX = curvedArrow.getEndX(); double currentEndY = curvedArrow.getEndY();     
                    double finalControlX = cp.getCenterX(); 
                    double finalControlY = cp.getCenterY(); 
                    double midX = (currentStartX + currentEndX) / 2.0; double midY = (currentStartY + currentEndY) / 2.0;
                    double controlVecX = finalControlX - midX; double controlVecY = finalControlY - midY;
                    double dx = currentEndX - currentStartX; double dy = currentEndY - currentStartY;
                    double distance = Math.hypot(dx, dy); if (distance < 1e-6) distance = 1e-6; 
                    double perpX = -dy / distance; double perpY = dx / distance;
                    this.persistentControlOffsetFactor = controlVecX * perpX + controlVecY * perpY;
                    // System.out.println("[ControlPoint Released] New persistentControlOffsetFactor: " + this.persistentControlOffsetFactor);
                 }
                 e.consume();
             }
        });
    }

    // - Remove drag event handlers for control point
    private void deregisterControlPointDrag() { 
         Circle cp = curvedArrow.getControlPoint(); 
         if (cp == null) return;
         cp.setOnMousePressed(null);
         cp.setOnMouseDragged(null);
         cp.setOnMouseReleased(null);
         // System.out.println("[Deregister] Removed drag handlers from control point.");
    }

    // - Check if transition is finalized
    public boolean isComplete() {
        return complete;
    }

    // - Access currently selected transition
    public static Transition getSelectedTransition() {
        return selectedTransition;
    }
    
    // - Validate transition symbol
    private boolean isInvalidSymbol(String candidate) {
        // Invalid only if null or effectively empty (whitespace)
        return candidate == null || candidate.trim().isEmpty();
    }
    
    // - Show alert for invalid symbol input
    private void showInvalidSymbolAlert() {
        // Updated alert message
        showAlert("Invalid Symbol", "Transition symbol cannot be empty.");
    }
    
    // - Commit the deselection visuals and state
    private void commitDeselection() {
        System.out.println(LOG_DEBUG + "Transition commitDeselection(): Committing for: " + this.getSymbol());
        if (curvedArrow != null) curvedArrow.deselect();
        deregisterControlPointDrag();
        if (editableLabel != null) {
            editableLabel.finalizeLabel();
            if (editableLabel.getEditor() != null) {
                 editableLabel.getEditor().setOnAction(null); // Remove Enter listener
                 // Remove focus listener just in case it was somehow added elsewhere
                 if (editorFocusListener != null) {
                      editableLabel.getEditor().focusedProperty().removeListener(editorFocusListener);
                  }
             }
        }
        if (selectedTransition == this) {
            selectedTransition = null;
        }
        if (selectionListener != null) {
             selectionListener.onDeselected(this); // Notify listener
         }
    }

    // New constructor for direct creation (e.g., loading, minimization)
    // This does NOT create visual elements or add listeners
    public Transition(State fromState, String symbol, State toState) {
        if (fromState == null || toState == null || symbol == null) {
             throw new IllegalArgumentException("State and symbol cannot be null for direct transition creation.");
        }
        this.fromState = fromState;
        this.toState = toState;
        this.symbol = symbol;
        this.complete = true; // Mark as complete since states and symbol are provided
        
        // Initialize minimal required fields, leave visual components null
        this.curvedArrow = null; 
        this.editableLabel = null;
        this.dfaInstance = null; // No DFA context needed for this type
        this.controllerInstance = null; // No controller context needed
    }
}
