package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.info.Info;
import software.coley.recaf.ui.contextmenu.actions.BundleAction;
import software.coley.recaf.ui.contextmenu.actions.InfoAction;
import software.coley.recaf.ui.contextmenu.actions.ResourceAction;
import software.coley.recaf.ui.contextmenu.actions.WorkspaceAction;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Menu builder for {@link Info} content.
 *
 * @param <B>
 * 		Bundle type.
 * @param <I>
 * 		Info type.
 *
 * @author Matt Coley
 */
public class InfoMenuBuilder<B extends Bundle<?>, I extends Info> extends MenuBuilder<InfoMenuBuilder<B, I>> {
	private final Workspace workspace;
	private final WorkspaceResource resource;
	private final B bundle;
	private final I info;

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
	 * @param info
	 * 		Target info object.
	 */
	public InfoMenuBuilder(@Nullable InfoMenuBuilder<B, I> parent,
						   @Nonnull ItemSink sink,
						   @Nonnull Workspace workspace,
						   @Nonnull WorkspaceResource resource,
						   @Nonnull B bundle,
						   @Nonnull I info) {
		super(parent, sink);
		this.workspace = workspace;
		this.resource = resource;
		this.bundle = bundle;
		this.info = info;
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
	public MenuHandler<MenuItem> infoItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull InfoAction<B, I> action) {
		return item(id, icon, () -> action.accept(workspace, resource, bundle, info));
	}

	@Override
	public InfoMenuBuilder<B, I> submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new InfoMenuBuilder<>(this, sink.withMenu(key, icon), workspace, resource, bundle, info);
	}
}
