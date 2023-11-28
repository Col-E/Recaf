package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.ui.contextmenu.actions.ResourceAction;
import software.coley.recaf.ui.contextmenu.actions.WorkspaceAction;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Menu builder for {@link WorkspaceResource} content.
 *
 * @author Matt Coley
 */
public class ResourceMenuBuilder extends MenuBuilder<ResourceMenuBuilder> {
	private final Workspace workspace;
	private final WorkspaceResource resource;

	/**
	 * @param parent
	 * 		Optional parent menu.
	 * @param sink
	 * 		Sink to append menu items with.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Target resource.
	 */
	public ResourceMenuBuilder(@Nullable ResourceMenuBuilder parent,
							   @Nonnull ItemSink sink,
							   @Nonnull Workspace workspace,
							   @Nonnull WorkspaceResource resource) {
		super(parent, sink);
		this.workspace = workspace;
		this.resource = resource;
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
	public MenuHandler<MenuItem> resourceItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull ResourceAction action) {
		return item(id, icon, () -> action.accept(workspace, resource));
	}

	@Override
	public ResourceMenuBuilder submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new ResourceMenuBuilder(this, sink.withMenu(key, icon), workspace, resource);
	}
}
