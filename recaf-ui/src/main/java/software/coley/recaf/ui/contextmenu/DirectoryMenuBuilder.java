package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.ui.contextmenu.actions.BundleAction;
import software.coley.recaf.ui.contextmenu.actions.DirectoryAction;
import software.coley.recaf.ui.contextmenu.actions.ResourceAction;
import software.coley.recaf.ui.contextmenu.actions.WorkspaceAction;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Menu builder for directory paths in a {@link Bundle}.
 *
 * @param <B>
 * 		Bundle type.
 *
 * @author Matt Coley
 */
public class DirectoryMenuBuilder<B extends Bundle<?>> extends MenuBuilder<DirectoryMenuBuilder<B>> {
	private final Workspace workspace;
	private final WorkspaceResource resource;
	private final B bundle;
	private final String directoryName;

	/**
	 * @param parent
	 * 		Optional parent menu.
	 * @param sink
	 * 		Sink to append menu items with.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Target directory path.
	 */
	public DirectoryMenuBuilder(@Nullable DirectoryMenuBuilder<B> parent,
								@Nonnull ItemSink sink,
								@Nonnull Workspace workspace,
								@Nonnull WorkspaceResource resource,
								@Nonnull B bundle,
								@Nonnull String directoryName) {
		super(parent, sink);
		this.workspace = workspace;
		this.resource = resource;
		this.bundle = bundle;
		this.directoryName = directoryName;
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
	public MenuHandler<MenuItem> bundleItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull BundleAction<B> action) {
		return item(id, icon, () -> action.accept(workspace, resource, bundle));
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
	public MenuHandler<MenuItem> directoryItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull DirectoryAction<B> action) {
		return item(id, icon, () -> action.accept(workspace, resource, bundle, directoryName));
	}

	@Override
	public DirectoryMenuBuilder<B> submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new DirectoryMenuBuilder<>(this, sink.withMenu(key, icon), workspace, resource, bundle, directoryName);
	}

	/**
	 * @param bundleType
	 * 		Target type.
	 * @param <X>
	 * 		Target type.
	 *
	 * @return Cast self.
	 */
	@SuppressWarnings("unchecked")
	public <X extends Bundle<?>> DirectoryMenuBuilder<X> cast(@Nonnull Class<X> bundleType) {
		return (DirectoryMenuBuilder<X>) this;
	}
}
