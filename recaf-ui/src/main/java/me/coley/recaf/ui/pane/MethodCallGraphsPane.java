package me.coley.recaf.ui.pane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.workspace.Workspace;

import javax.annotation.Nonnull;

public class MethodCallGraphsPane extends BorderPane {

	private final ObjectProperty<MethodInfo> currentMethodInfo;

	public MethodCallGraphsPane(@Nonnull Workspace workspace, @Nonnull MethodInfo methodInfo) {
		currentMethodInfo = new SimpleObjectProperty<>(methodInfo);
		DockingWrapperPane wrapper = DockingWrapperPane.builder()
				.title(currentMethodInfo.map(method -> "Calls: " + method.getOwner() + "#" + method.getName() + method.getDescriptor()))
				.content(new MethodCallGraphPane(workspace, MethodCallGraphPane.CallGraphMode.CALLS, currentMethodInfo))
				.size(600, 300)
				.build();
		DockingRegion region = wrapper.getTab().getParent();
		RecafDockingManager.getInstance().createTabIn(region,
				() -> new DockTab(currentMethodInfo.map(method -> "Callers: " + method.getOwner() + "#" + method.getName() + method.getDescriptor()),
						new MethodCallGraphPane(workspace, MethodCallGraphPane.CallGraphMode.CALLERS, currentMethodInfo)));
		region.getDockTabs().forEach(t -> t.setClosable(true));
		setCenter(wrapper);
	}

	public MethodInfo getCurrentMethodInfo() {
		return currentMethodInfo.get();
	}

	public ObjectProperty<MethodInfo> currentMethodInfoProperty() {
		return currentMethodInfo;
	}
}
