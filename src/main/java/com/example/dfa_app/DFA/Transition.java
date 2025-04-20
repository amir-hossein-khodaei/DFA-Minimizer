package com.example.dfa_app.DFA;

import com.example.dfa_app.SelectionListener;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.event.ActionEvent;

/**
 * Represents a connection (transition) between two states.
 * This class maintains a CurvedArrow (for the visual representation) and an EditableLabel (for naming).
 * It contains no direct event handlers for selection/deselection—the control point's drag events are registered
 * when the Transition is selected and removed when it is deselected.
 */
public class Transition extends Group implements simularity {

    private final State fromState;
    private State toState;
    private final CurvedArrow curvedArrow;
    private final EditableLabel editableLabel;
    private boolean complete = false;
    private String symbol = "?"; // Initialize symbol
    
    // Fields to store the fixed control point offset vector components relative to the perpendicular vector
    private double fixedControlOffsetFactor = 0; 
    private boolean controlOffsetFactorSet = false;

    // Used while drawing interactively.
    private double tempEndX;
    private double tempEndY;

    // Offset for computing a perpendicular control point.
    private static final double CONTROL_OFFSET = 40.0;
    private SelectionListener selectionListener;
    private static Transition selectedTransition = null; // Track selected transition

    public Transition(State fromState) {
        if (fromState == null) {
            throw new IllegalArgumentException("fromState cannot be null.");
        }
        this.fromState = fromState;
        this.curvedArrow = new CurvedArrow();
        this.editableLabel = new EditableLabel();
        editableLabel.setText("?");
        editableLabel.setVisible(false);

        // Enable key events.
        this.setFocusTraversable(true);

        // Add arrow and label.
        getChildren().addAll(curvedArrow, editableLabel);

        // If the parent of fromState is a Pane, add this Transition to it.
        if (fromState.getParent() instanceof Pane) {
            ((Pane) fromState.getParent()).getChildren().add(this);
        }

        // Listen for fromState layout changes.
        InvalidationListener layoutListener = obs -> updateTransition();
        fromState.layoutXProperty().addListener(layoutListener);
        fromState.layoutYProperty().addListener(layoutListener);

        updateTransition();
    }

    // Getter for the editable label
    public EditableLabel getEditableLabel() {
        return editableLabel;
    }

    // Getter for the 'from' state
    public State getFromState() {
        return fromState;
    }

    public CurvedArrow getCurvedArrow() {
        return curvedArrow;
    }

    // Allows updating a temporary endpoint while drawing.
    public void setTempEnd(double x, double y) {
        this.tempEndX = x;
        this.tempEndY = y;
        updateTransition();
    }

    // Updates the geometry of the arrow based on state positions and control point calculations.
    private void updateTransition() {
        double fromX = fromState.getLayoutX();
        double fromY = fromState.getLayoutY();
        double startX, startY, endX, endY, controlX, controlY;

        if (complete && toState != null) {
            if (fromState == toState) {
                double radius = fromState.getMainCircle().getRadius();
                startX = fromX;
                startY = fromY - radius;
                endX = fromX + radius;
                endY = fromY;
                controlX = fromX;
                controlY = fromY - radius * 2.5;
            } else {
                double toX = toState.getLayoutX();
                double toY = toState.getLayoutY();
                double dx = toX - fromX;
                double dy = toY - fromY;
                double distance = Math.hypot(dx, dy);
                if (distance == 0) { distance = 1; }
                
                double fromRadius = fromState.getMainCircle().getRadius();
                startX = fromX + (dx / distance) * fromRadius;
                startY = fromY + (dy / distance) * fromRadius;
                double toRadius = toState.getMainCircle().getRadius();
                endX = toX - (dx / distance) * toRadius;
                endY = toY - (dy / distance) * toRadius;
                
                double midX = (startX + endX) / 2.0;
                double midY = (startY + endY) / 2.0;
                
                double norm = distance;
                double perpX = -dy / norm;
                double perpY = dx / norm;
                
                controlX = midX + fixedControlOffsetFactor * perpX;
                controlY = midY + fixedControlOffsetFactor * perpY;
            }
        } else {
            double dx = tempEndX - fromX;
            double dy = tempEndY - fromY;
            double distance = Math.hypot(dx, dy);
            if (distance == 0) { distance = 1; }
            double fromRadius = fromState.getMainCircle().getRadius();
            startX = fromX + (dx / distance) * fromRadius;
            startY = fromY + (dy / distance) * fromState.getMainCircle().getRadius();
            endX = tempEndX;
            endY = tempEndY;
            double midX = (startX + endX) / 2.0;
            double midY = (startY + endY) / 2.0;
            double norm = distance;
            double perpX = -dy / norm;
            double perpY = dx / norm;
            double dynamicOffset = CONTROL_OFFSET + (int) (Math.random() * 21) - 10;
            controlX = midX + dynamicOffset * perpX;
            controlY = midY + dynamicOffset * perpY;
        }

        curvedArrow.setStart(startX, startY);
        curvedArrow.setEnd(endX, endY);
        curvedArrow.setControl(controlX, controlY);

        if (complete) {
            Platform.runLater(() -> {
                try {
                    editableLabel.applyCss();
                    editableLabel.layout();
                    double labelWidth = editableLabel.getLabelWidth();
                    double labelHeight = editableLabel.getLabelHeight();
                    double[] tip = curvedArrow.getArrowTip();
                    if (tip == null || tip.length < 2) {
                        System.err.println("[Transition Update] Error: getArrowTip() returned invalid data.");
                        return;
                    }
                    double labelX = tip[0] - labelWidth / 2.0;
                    double labelY = tip[1] - labelHeight / 2.0;
                    
                    System.out.println("[Transition Update] Label Pos: x=" + labelX + ", y=" + labelY + ", w=" + labelWidth + ", h=" + labelHeight);
                    
                    editableLabel.setLabelPosition(labelX, labelY);
                    editableLabel.setEditorPosition(labelX, labelY);
                } catch (Exception e) {
                    System.err.println("[Transition Update] Error during label position update: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    // Completes the transition by attaching the target state.
    public void completeTransition(State targetState) {
        if (targetState == null) {
            throw new IllegalArgumentException("Target state cannot be null.");
        }
        this.toState = targetState;
        this.complete = true;
        curvedArrow.setComplete(true);
        
        if (!controlOffsetFactorSet && fromState != toState) {
            double extraOffset = (int) (Math.random() * 41) - 20;
            fixedControlOffsetFactor = CONTROL_OFFSET + extraOffset;
            controlOffsetFactorSet = true;
        } else if (fromState == toState) {
            fixedControlOffsetFactor = 0;
            controlOffsetFactorSet = true;
        }
        
        InvalidationListener toListener = obs -> updateTransition();
        toState.layoutXProperty().addListener(toListener);
        toState.layoutYProperty().addListener(toListener);

        editableLabel.setVisible(true);
        editableLabel.setText("?");

        updateTransition();
    }

    public void setSymbol(String proposedName) {
        // Only log if the symbol actually changes
        if (proposedName != null && !proposedName.equals(this.symbol)) {
            String oldSymbol = this.symbol; // Store old symbol
            this.symbol = proposedName;
             System.out.println("Transition symbol changed from '" + (oldSymbol == null ? "<null>" : oldSymbol) + "' to '" + proposedName + "'");
        }
    }

    public String getSymbol() {
        return symbol;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public State getNextState() {
        return toState;
    }

    /**
     * When a Transition is selected, we want to both update visual style and allow the user
     * to drag the control point to adjust the curve. Therefore, we attach drag event handlers to
     * the control point here.
     */
    @Override
    public void select() {
        // Deselect previous state or transition
         if (State.getSelectedState() != null) State.getSelectedState().deselect();
         if (selectedTransition != null && selectedTransition != this) {
            selectedTransition.deselect();
        }
        selectedTransition = this;
        
        curvedArrow.select(); // Visual selection
        registerControlPointDrag(); // Allow curve dragging
        
        // Only start editing if the transition is complete
        if (isComplete()) {
             System.out.println("Transition selected and complete, starting edit.");
            editableLabel.startEditing(); 
             // Add Enter key listener for finalization
             if (editableLabel.getEditor() != null) {
                 editableLabel.getEditor().setOnAction(event -> {
                     System.out.println("Enter pressed on transition editor.");
                     this.deselect(); // Trigger finalization logic
                     event.consume();
                 });
             } else {
                  System.err.println("Warning: Transition editor not available in select()");
             }
        }
        // Notify controller listener if needed (optional)
        // if (selectionListener != null) { selectionListener.onSelected(this); }
    }

    /**
     * When a Transition is deselected, we remove the drag event handlers from the control point.
     */
    @Override
    public void deselect() {
        System.out.println("Transition deselect() called.");
        // Only try to finalize symbol if it was complete and being edited
        if (isComplete() && editableLabel.getEditor().isVisible()) { 
            // Attempt to finalize NAME, commit deselection only if successful
            if (!attemptFinalizeName()) { 
                System.out.println("Finalize failed (invalid symbol), keeping selected.");
                keepSelected(); // Keep selected if symbol is invalid
                return; // Stop deselection
            }
             System.out.println("Finalize successful.");
        }
        // If not complete, or finalize succeeded, commit the deselection
         System.out.println("Committing deselection.");
        commitDeselection();
    }

    public boolean isSelected() {
        return curvedArrow.isSelected();
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Attaches mouse event handlers to the control point so the user can drag it.
     */
    private void registerControlPointDrag() {
        CurvedArrow arrow = this.getCurvedArrow();
        Circle cp = arrow.getControlPoint();

        // Local array to track drag offset.
        final double[] dragDelta = new double[2];

        cp.setOnMousePressed((MouseEvent e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D cpScene = cp.localToScene(cp.getCenterX(), cp.getCenterY());
                dragDelta[0] = e.getSceneX() - cpScene.getX();
                dragDelta[1] = e.getSceneY() - cpScene.getY();
                e.consume();
            }
        });

        cp.setOnMouseDragged((MouseEvent e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double newSceneX = e.getSceneX() - dragDelta[0];
                double newSceneY = e.getSceneY() - dragDelta[1];
                // Convert scene coordinates into the arrow's local coordinate system.
                Point2D localPoint = arrow.sceneToLocal(newSceneX, newSceneY);
                arrow.setControl(localPoint.getX(), localPoint.getY());
                e.consume();
            }
        });

        cp.setOnMouseReleased((MouseEvent e) -> {
            e.consume();
        });
    }

    /**
     * Removes the mouse event handlers from the control point.
     */
    private void deregisterControlPointDrag() {
        CurvedArrow arrow = this.getCurvedArrow();
        Circle cp = arrow.getControlPoint();
        cp.setOnMousePressed(null);
        cp.setOnMouseDragged(null);
        cp.setOnMouseReleased(null);
    }

    // Method to check if transition is complete
    public boolean isComplete() {
        return complete;
    }

    // Static getter for selected transition
    public static Transition getSelectedTransition() {
        return selectedTransition;
    }
    
    // --- Symbol Validation Helpers ---
    private boolean isInvalidSymbol(String candidate) {
        // Currently just checking for empty/null. 
        // Could add uniqueness check later if needed (e.g., per from/to state pair).
        return candidate == null || candidate.trim().isEmpty();
    }
    
    private void showInvalidSymbolAlert() {
         showAlert("Invalid Transition Symbol", "Transition symbol cannot be empty.");
    }
    
    // --- Finalization Logic ---
    public boolean attemptFinalizeName() {
        String proposedName = editableLabel.getText();
        if (proposedName != null && !proposedName.trim().isEmpty()) {
            // Success
            editableLabel.finalizeLabel();
            setSymbol(proposedName); // Symbol set ONLY on successful finalization
            curvedArrow.deselect(); // Maybe remove this? Deselection handles visuals.
            return true; // Indicate success
        } else {
            // Failure
            showAlert("Invalid Transition Symbol", "Transition symbol cannot be empty.");
            editableLabel.startEditing(); // Keep editing active
            return false; // Indicate failure
        }
    }
    
    // --- Selection State Management ---
    private void keepSelected() {
        editableLabel.startEditing(); // Keep editor active
        curvedArrow.select(); // Keep visual selected state
    }
    
    private void commitDeselection() {
        curvedArrow.deselect();
        deregisterControlPointDrag();
        // Remove Enter listener (might already be null if finalizedSymbol succeeded)
         if (editableLabel.getEditor() != null) {
             editableLabel.getEditor().setOnAction(null);
         }
        if (selectedTransition == this) {
            selectedTransition = null;
        }
        // Notify controller listener if needed (optional)
        // if (selectionListener != null) { selectionListener.onDeselected(this); }
    }
}
