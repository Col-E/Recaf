package software.coley.recaf.ui.control.graph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.callgraph.CallGraph;
import software.coley.recaf.services.callgraph.CallGraphService;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Container pane for two {@link MethodCallGraphPane} for inbound and outbound calls.
 *
 * @author Amejonah
 */
@Dependent
public class MethodCallGraphsPane extends TabPane implements ClassNavigable, UpdatableNavigable {
	private final ObjectProperty<MethodMember> currentMethodInfo;
	private ClassPathNode path;

	@Inject
	public MethodCallGraphsPane(@Nonnull Workspace workspace, @Nonnull CallGraphService callGraphService,
	                            @Nonnull TextFormatConfig format, @Nonnull Actions actions,
	                            @Nonnull CellConfigurationService configurationService) {
		currentMethodInfo = new SimpleObjectProperty<>();

		CallGraph callGraph = Objects.requireNonNull(callGraphService.getCurrentWorkspaceCallGraph(), "Graph not created");
		getTabs().add(creatTab(workspace, callGraph, configurationService, format, actions, MethodCallGraphPane.CallGraphMode.CALLS, currentMethodInfo));
		getTabs().add(creatTab(workspace, callGraph, configurationService, format, actions, MethodCallGraphPane.CallGraphMode.CALLERS, currentMethodInfo));

		// Remove the standard tab-pane border.
		getStyleClass().addAll("borderless");
	}

	@Nonnull
	private Tab creatTab(@Nonnull Workspace workspace, @Nonnull CallGraph callGraph, @Nonnull CellConfigurationService configurationService,
	                     @Nonnull TextFormatConfig format, @Nonnull Actions actions, @Nonnull MethodCallGraphPane.CallGraphMode mode,
	                     @Nullable ObjectProperty<MethodMember> methodInfoObservable) {
		Tab tab = new Tab();
		tab.setContent(new MethodCallGraphPane(workspace, callGraph, configurationService, format, actions, mode, methodInfoObservable));
		tab.textProperty().bind(Lang.getBinding("menu.view.methodcallgraph." + mode.name().toLowerCase()));
		tab.setClosable(false);
		return tab;
	}

	@Nonnull
	public ObjectProperty<MethodMember> currentMethodInfoProperty() {
		return currentMethodInfo;
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassMemberPathNode memberPathNode) {
			this.path = memberPathNode.getParent();
			ClassMember member = memberPathNode.getValue();
			if (member instanceof MethodMember method)
				currentMethodInfo.setValue(method);
		}
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return getClassPath();
	}

	@Override
	public boolean isTrackable() {
		// Disabling tracking allows other panels with the same path-node to be opened.
		return false;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		getTabs().clear();
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		// no-op
	}
}
