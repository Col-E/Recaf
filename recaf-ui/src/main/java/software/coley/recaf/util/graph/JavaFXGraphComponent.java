package software.coley.recaf.util.graph;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import org.jfree.fx.FXGraphics2D;

import java.awt.*;

public class JavaFXGraphComponent extends ScrollPane {

    static final double SCROLL_ZOOM = 0.2;

    final ObjectProperty<mxGraph> graphProperty = new SimpleObjectProperty<mxGraph>();
    final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);

    Canvas canvas;
    Graphics2D awtGraphics;
    mxGraphics2DCanvas mxCanvas;

    public JavaFXGraphComponent() {
        this.setPannable(true);

        this.graphProperty.addListener(this::onGraphChanged);
        this.zoomProperty.addListener(this::onZoomChanged);

        this.widthProperty().addListener((observable, oldValue, newValue) -> this.repaint());
        this.heightProperty().addListener((observable, oldValue, newValue) -> this.repaint());

        this.setOnScroll(event -> {
            if (event.isControlDown()) {
                double zoom = Math.signum(event.getDeltaY()) * SCROLL_ZOOM;
                this.zoomProperty.set(this.zoomProperty.get() + zoom);
                event.consume();
            }
        });
    }

    public void setGraph(mxGraph graph) {
        this.graphProperty.set(graph);
    }

    void onZoomChanged(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        double zoom = newValue.doubleValue();

        mxGraph graph = this.graphProperty.get();
        graph.getView().scaleAndTranslate(zoom, 0, 0);

        this.createCanvas(this.graphProperty.get());
        this.repaint();
    }

    void onGraphChanged(ObservableValue<? extends mxGraph> observable, mxGraph oldValue, mxGraph newValue) {
        if (oldValue != null) {
            oldValue.removeListener(this::onGraphRepaint);
        }

        newValue.addListener(mxEvent.REPAINT, this::onGraphRepaint);
        this.createCanvas(newValue);
        this.repaint();
    }

    void createCanvas(mxGraph graph) {
        mxRectangle bounds = graph.getView().getGraphBounds();
        int border = graph.getBorder();
        int margin = 20;
        this.canvas = new Canvas(bounds.getX() + bounds.getWidth() + border * 2 + margin,
                bounds.getY() + bounds.getHeight() + border * 2 + margin);
        this.setContent(this.canvas);
        this.awtGraphics = new FXGraphics2D(this.canvas.getGraphicsContext2D());

        this.mxCanvas = new mxGraphics2DCanvas(this.awtGraphics);
        this.mxCanvas.setScale(graph.getView().getScale());
    }

    void onGraphRepaint(Object o, mxEventObject mxEventObject) {
       this.repaint();
    }

    void repaint() {
        mxGraph graph = this.graphProperty.get();
        if (this.canvas == null) {
            this.createCanvas(graph);
            return;
        }

        final var gc = this.canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());

        Graphics2D g = (Graphics2D) this.awtGraphics.create();
        try {
            g.translate(20, 20);
            this.mxCanvas.setGraphics(g);
            graph.drawGraph(this.mxCanvas);
        } finally {
            g.dispose();
        }
    }

    public ObjectProperty<mxGraph> graphPropertyProperty() {
        return graphProperty;
    }

    public DoubleProperty zoomPropertyProperty() {
        return zoomProperty;
    }
}
