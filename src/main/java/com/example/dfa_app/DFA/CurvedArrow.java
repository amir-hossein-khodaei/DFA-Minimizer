package com.example.dfa_app.DFA;

import javafx.animation.ScaleTransition;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.util.Duration;




public class CurvedArrow extends Group {

    private final QuadCurve curve;
    private final Polygon arrowHead;
    private final Circle controlPoint;

    private double startX, startY;
    private double controlX, controlY;
    private double endX, endY;
    private double arrowTipX, arrowTipY;

    private boolean selected = false;
    private boolean complete = false;

    
    private static final double ARROW_LENGTH = 15.0;
    private static final double ARROW_WIDTH = 10.0;

    public CurvedArrow() {
        
        curve = new QuadCurve();
        curve.setStroke(Color.BLACK);
        curve.setStrokeWidth(2);
        curve.setFill(Color.TRANSPARENT);
        curve.setPickOnBounds(true);

        
        arrowHead = new Polygon();
        arrowHead.setFill(Color.BLACK);
        arrowHead.getStyleClass().add("arrow-head");

        
        controlPoint = new Circle(5, Color.RED);
        controlPoint.setStroke(Color.BLACK);
        controlPoint.setStrokeWidth(2);
        controlPoint.setVisible(false);
        controlPoint.setCursor(Cursor.HAND);

        
        getChildren().addAll(curve, arrowHead, controlPoint);

        curve.setMouseTransparent(true);
    }

    
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public double getControlX() { return controlX; }
    public double getControlY() { return controlY; }

    
    public Polygon getArrowHead() {
        return arrowHead;
    }

    
    public Circle getControlPoint() {
        return controlPoint;
    }

    
    public void setStart(double x, double y) {
        this.startX = x;
        this.startY = y;
        curve.setStartX(x);
        curve.setStartY(y);
        updateArrowHead();
    }

    
    public void setEnd(double x, double y) {
        this.endX = x;
        this.endY = y;
        curve.setEndX(x);
        curve.setEndY(y);
        updateArrowHead();
    }

    
    public void setControl(double x, double y) {
        this.controlX = x;
        this.controlY = y;
        curve.setControlX(x);
        curve.setControlY(y);
        controlPoint.setCenterX(x);
        controlPoint.setCenterY(y);
        
        updateArrowHead();
    }

    
    private double[] getCurvePoint(double t) {
        double oneMinusT = 1 - t;
        double x = oneMinusT * oneMinusT * startX + 2 * oneMinusT * t * controlX + t * t * endX;
        double y = oneMinusT * oneMinusT * startY + 2 * oneMinusT * t * controlY + t * t * endY;
        return new double[]{x, y};
    }

    
    private double[] getCurveDerivative(double t) {
        double oneMinusT = 1 - t;
        double dx = 2 * oneMinusT * (controlX - startX) + 2 * t * (endX - controlX);
        double dy = 2 * oneMinusT * (controlY - startY) + 2 * t * (endY - controlY);
        return new double[]{dx, dy};
    }

    
    public void updateArrowHead() {
        
        double t = 0.5;
        double[] midPoint = getCurvePoint(t);
        arrowTipX = midPoint[0];
        arrowTipY = midPoint[1];

        
        double[] derivative = getCurveDerivative(t);
        double angle = Math.atan2(derivative[1], derivative[0]);

        
        double baseX = arrowTipX - ARROW_LENGTH * Math.cos(angle);
        double baseY = arrowTipY - ARROW_LENGTH * Math.sin(angle);
        double leftX = baseX + ARROW_WIDTH * Math.sin(angle);
        double leftY = baseY - ARROW_WIDTH * Math.cos(angle);
        double rightX = baseX - ARROW_WIDTH * Math.sin(angle);
        double rightY = baseY + ARROW_WIDTH * Math.cos(angle);
        arrowHead.getPoints().setAll(arrowTipX, arrowTipY, leftX, leftY, rightX, rightY);

        
         if (complete) {
             animateArrowHead();
         }
    }

    
    private void animateArrowHead() {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), arrowHead);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.7);
        st.setToY(1.7);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    
    public double[] getArrowTip() {
        return new double[]{arrowTipX, arrowTipY};
    }

    
    public void select() {
        selected = true;
        curve.setStroke(Color.BLUE);
        controlPoint.setVisible(true);
        setCursor(Cursor.HAND);
        toFront();
    }

    
    public void deselect() {
        selected = false;
        curve.setStroke(Color.BLACK);
        controlPoint.setVisible(false);
        setCursor(Cursor.DEFAULT);
    }

    public boolean isSelected() {
        return selected;
    }

    
    public void setComplete(boolean complete) {
        this.complete = complete;
        updateArrowHead();
    }
}
