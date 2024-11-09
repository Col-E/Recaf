package software.coley.recaf.ui.control.cfg;

import com.mxgraph.view.mxGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cfg.ControlFlowGraph;
import software.coley.recaf.services.cfg.ControlFlowGraphBuilder;
import software.coley.recaf.services.cfg.ControlFlowGraphService;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.graph.JavaFXGraphComponent;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Dependent
public class ControlFlowGraphPane extends AbstractContentPane<PathNode<?>> implements ClassNavigable, UpdatableNavigable {

    final ObjectProperty<MethodMember> currentMethodInfo = new SimpleObjectProperty<>();
    final ControlFlowGraphView view;
    final ControlFlowGraphService service;
    ClassPathNode path;

    @Inject
    public ControlFlowGraphPane(ControlFlowGraphView view, ControlFlowGraphService service) {
        this.view = view;
        this.service = service;
        getStyleClass().addAll("borderless");
    }

    @Nonnull
    @Override
    public ClassPathNode getClassPath() {
        return path;
    }

    @Override
    public void requestFocus(@Nonnull ClassMember member) {

    }

    @Override
    public void onUpdatePath(@Nonnull PathNode<?> path) {
        if (path instanceof ClassMemberPathNode memberPathNode) {
            this.path = memberPathNode.getParent();
            ClassMember member = memberPathNode.getValue();
            if (member instanceof MethodMember method) {
                currentMethodInfo.setValue(method);

                this.generateDisplay();
            }
        }
    }

    @Nullable
    @Override
    public PathNode<?> getPath() {
        return path;
    }

    @Override
    protected void generateDisplay() {
        this.setLoading();
        CompletableFuture.supplyAsync(this::createControlFlowGraph)
                .thenAccept(graph -> FxThreadUtil.run(() -> this.setGraph(graph)));
    }

    mxGraph createControlFlowGraph() {
        ControlFlowGraph cfg = this.service.createControlFlow(this.path.getValue(), this.currentMethodInfo.get());
        // TODO: error check
        try {
            return this.view.createGraph(cfg);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        }
    }

    void setLoading() {
        Label label = new Label();
        label.textProperty().bind(Lang.getBinding("menu.view.methodcfg.loading"));
        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER);
        controls.getChildren().add(label);
        this.setCenter(controls);
    }

    void setGraph(mxGraph graph) {
        JavaFXGraphComponent graphComponent = new JavaFXGraphComponent();
        graphComponent.setGraph(graph);

        HBox controls = new HBox();
        controls.setSpacing(4);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Button zoomInButton = new Button();
        zoomInButton.setGraphic(new FontIconView(CarbonIcons.ZOOM_IN));

        Button zoomOutButton = new Button();
        zoomOutButton.setGraphic(new FontIconView(CarbonIcons.ZOOM_OUT));

        zoomInButton.setOnMouseClicked(event -> graphComponent.zoomPropertyProperty().set(graphComponent.zoomPropertyProperty().get() + 0.2d));
        zoomOutButton.setOnMouseClicked(event -> graphComponent.zoomPropertyProperty().set(graphComponent.zoomPropertyProperty().get() - 0.2d));

        controls.getChildren().addAll(zoomInButton, zoomOutButton);

        VBox box = new VBox();
        box.getChildren().add(graphComponent);
        graphComponent.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(controls);
        VBox.setVgrow(graphComponent, Priority.ALWAYS);

        this.setCenter(box);
    }

    @Nonnull
    @Override
    public Collection<Navigable> getNavigableChildren() {
        return List.of();
    }

    @Override
    public void disable() {

    }
}