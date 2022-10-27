package me.coley.recaf.ui.pane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;

import javax.annotation.Nonnull;

public class MethodCallGraphsPane extends TabPane {

	private final ObjectProperty<MethodInfo> currentMethodInfo;

	public MethodCallGraphsPane(@Nonnull Workspace workspace, @Nonnull MethodInfo methodInfo) {
		currentMethodInfo = new SimpleObjectProperty<>(methodInfo);
		getTabs().add(creatTab(workspace, MethodCallGraphPane.CallGraphMode.CALLS));
		getTabs().add(creatTab(workspace, MethodCallGraphPane.CallGraphMode.CALLERS));
	}

	@Nonnull
	private Tab creatTab(Workspace workspace,
											 @Nonnull MethodCallGraphPane.CallGraphMode mode) {
		Tab tab = new Tab();
		tab.setContent(new MethodCallGraphPane(workspace, mode, currentMethodInfo));
		tab.textProperty().bind(Lang.getBinding("menu.view.methodcallgraph." + mode.name().toLowerCase()));
		tab.setClosable(false);
		return tab;
	}

	public MethodInfo getCurrentMethodInfo() {
		return currentMethodInfo.get();
	}

	public ObjectProperty<MethodInfo> currentMethodInfoProperty() {
		return currentMethodInfo;
	}
}
