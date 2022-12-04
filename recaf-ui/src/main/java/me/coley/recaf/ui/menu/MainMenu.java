package me.coley.recaf.ui.menu;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.RecentWorkspacesConfig;
import me.coley.recaf.ui.control.MenuLabel;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.window.MainWindow;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceCloseListener;
import me.coley.recaf.workspace.WorkspaceOpenListener;
import me.coley.recaf.workspace.resource.AgentResource;

/**
 * Menu bar applies to the {@link MainWindow}.
 *
 * @author Matt Coley
 * @see MenuBarPopulation Populates the menu after construction.
 */
@ApplicationScoped
public class MainMenu extends BorderPane implements WorkspaceOpenListener, WorkspaceCloseListener {
	private final MenuBar menu = new MenuBar();
	private final BooleanProperty noWorkspace = new SimpleBooleanProperty(true);
	private final BooleanProperty agentWorkspace = new SimpleBooleanProperty(false);
	private final BooleanProperty remapping = new SimpleBooleanProperty(false);
	private final MenuLabel status = new MenuLabel("Status: IDLE");

	@Inject
	public MainMenu(NavigationBar navigationBar) {
		// Layout the content. The navigation bar goes 'under' the menu.
		// This way it can slide in and out like a drawer.
		StackPane stack = new StackPane();
		stack.getChildren().addAll(navigationBar, menu);
		StackPane.setAlignment(menu, Pos.TOP_LEFT);
		StackPane.setAlignment(navigationBar, Pos.BOTTOM_LEFT);
		setCenter(stack);

		// Info menu
		//	MenuBar info = new MenuBar();
		//	info.getMenus().add(status);
		//	setRight(info);

		// Initial state
		onWorkspaceClosed(null);
	}

	public void addMenu(Menu menu) {
		this.menu.getMenus().add(menu);
	}

	public BooleanProperty noWorkspaceProperty() {
		return noWorkspace;
	}

	public BooleanProperty agentWorkspaceProperty() {
		return agentWorkspace;
	}

	public BooleanProperty remappingProperty() {
		return remapping;
	}

	@Override
	public void onWorkspaceOpened(Workspace workspace) {
		noWorkspace.set(false);
		agentWorkspace.set(workspace.getResources().getPrimary() instanceof AgentResource);
		RecentWorkspacesConfig recentWorkspaces = Configs.recentWorkspaces();
		if (recentWorkspaces.canSerialize(workspace))
			recentWorkspaces.addWorkspace(workspace);
	}

	@Override
	public void onWorkspaceClosed(Workspace workspace) {
		noWorkspace.set(true);
		agentWorkspace.set(false);
	}
}
