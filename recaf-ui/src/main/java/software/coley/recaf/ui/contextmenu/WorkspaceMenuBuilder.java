package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.ui.contextmenu.actions.WorkspaceAction;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Menu builder for {@link Workspace} content.
 *
 * @author Matt Coley
 */
public class WorkspaceMenuBuilder extends MenuBuilder<WorkspaceMenuBuilder> {
	private final Workspace workspace;

	/**
	 * @param parent
	 * 		Optional parent menu.
	 * @param sink
	 * 		Sink to append menu items with.
	 * @param workspace
	 * 		Target workspace.
	 */
	public WorkspaceMenuBuilder(@Nullable WorkspaceMenuBuilder parent,
								@Nonnull ItemSink sink,
								@Nonnull Workspace workspace) {
		super(parent, sink);
		this.workspace = workspace;
	}

	/**
	 * @param id
	 * 		Menu item ID. Doubles as {@link Lang#get(String)} key.
	 * @param icon
	 * 		Menu item graphic.
	 * @param action
	 * 		Menu item action.
	 *
	 * @return Handler for optional post-addition manipulations of the added item.
	 */
	@Nonnull
	public MenuHandler<MenuItem> workspaceItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull WorkspaceAction action) {
		return item(id, icon, () -> action.accept(workspace));
	}

	@Override
	public WorkspaceMenuBuilder submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new WorkspaceMenuBuilder(this, sink.withMenu(key, icon), workspace);
	}
}
