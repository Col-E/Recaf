package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.ui.contextmenu.actions.*;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Menu builder for {@link AnnotationInfo} content.
 *
 * @author Matt Coley
 */
public class AnnotationMenuBuilder extends MenuBuilder<AnnotationMenuBuilder> {
	private final Workspace workspace;
	private final WorkspaceResource resource;
	private final ClassBundle<? extends ClassInfo> bundle;
	private final Annotated annotated;
	private final AnnotationInfo annotation;

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
	 * @param annotated
	 * 		Item declared as the annotation target.
	 * @param annotation
	 * 		Target annotation.
	 */
	public AnnotationMenuBuilder(@Nullable AnnotationMenuBuilder parent,
								 @Nonnull ItemSink sink,
								 @Nonnull Workspace workspace,
								 @Nonnull WorkspaceResource resource,
								 @Nonnull ClassBundle<? extends ClassInfo> bundle,
								 @Nonnull Annotated annotated,
								 @Nonnull AnnotationInfo annotation) {
		super(parent, sink);
		this.workspace = workspace;
		this.resource = resource;
		this.bundle = bundle;
		this.annotated = annotated;
		this.annotation = annotation;
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
	public MenuHandler<MenuItem> bundleItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull BundleAction<?> action) {
		return item(id, icon, () -> action.accept(workspace, resource, Unchecked.cast(bundle)));
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
	public MenuHandler<MenuItem> infoItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull InfoAction<?, ?> action) {
		return item(id, icon, () -> action.accept(workspace, resource, Unchecked.cast(bundle), Unchecked.cast(annotated)));
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
	public MenuHandler<MenuItem> annoItem(@Nonnull String id, @Nonnull Ikon icon, @Nonnull AnnotationAction action) {
		return item(id, icon, () -> action.accept(workspace, resource, bundle, annotated, annotation));
	}

	@Override
	public AnnotationMenuBuilder submenu(@Nonnull String key, @Nonnull Ikon icon) {
		return new AnnotationMenuBuilder(this, sink.withMenu(key, icon), workspace, resource, bundle, annotated, annotation);
	}
}
