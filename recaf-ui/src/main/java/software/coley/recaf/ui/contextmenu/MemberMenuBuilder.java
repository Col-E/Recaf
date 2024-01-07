package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.ui.contextmenu.actions.*;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Menu builder for {@link ClassMember} content.
 *
 * @param <B>
 * 		Bundle type.
 * @param <I>
 * 		Class type.
 * @param <M>
 * 		Member type.
 *
 * @author Matt Coley
 */
public class MemberMenuBuilder<B extends Bundle<?>, I extends ClassInfo, M extends ClassMember> extends MenuBuilder<MemberMenuBuilder<B, I, M>> {
	private final Workspace workspace;
	private final WorkspaceResource resource;
	private final B bundle;
	private final I info;
	private final M member;

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
	 * 		Declaring class of member.
	 * @param member
	 * 		Target member.
	 */
	public MemberMenuBuilder(@Nullable MemberMenuBuilder<B, I, M> parent,
							 @Nonnull ItemSink sink,
							 @Nonnull Workspace workspace,
							 @Nonnull WorkspaceResource resource,
							 @Nonnull B bundle,
							 @Nonnull I info,
							 @Nonnull M member) {
		super(parent, sink);
		this.workspace = workspace;
		this.resource = resource;
		this.bundle = bundle;
		this.info = info;
		this.member = member;
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
	public MenuHandler<MenuItem> memberItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull MemberAction<B, I, M> action) {
		return item(id, icon, () -> action.accept(workspace, resource, bundle, info, member));
	}

	@Override
	public MemberMenuBuilder<B, I, M> submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new MemberMenuBuilder<>(this, sink.withMenu(key, icon), workspace, resource, bundle, info, member);
	}
}
