package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.util.Menus;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link ClassContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicClassContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements ClassContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicClassContextMenuProviderFactory.class);

	@Inject
	public BasicClassContextMenuProviderFactory(@Nonnull TextProviderService textService,
												@Nonnull IconProviderService iconService,
												@Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getJvmClassInfoContextMenuProvider(@Nonnull ContextSource source,
																  @Nonnull Workspace workspace,
																  @Nonnull WorkspaceResource resource,
																  @Nonnull JvmClassBundle bundle,
																  @Nonnull JvmClassInfo info) {
		return () -> {
			ContextMenu menu = createMenu(source, workspace, resource, bundle, info);
			populateJvmMenu(menu, source, workspace, resource, bundle, info);
			return menu;
		};
	}

	@Nonnull
	@Override
	public ContextMenuProvider getAndroidClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	  @Nonnull Workspace workspace,
																	  @Nonnull WorkspaceResource resource,
																	  @Nonnull AndroidClassBundle bundle,
																	  @Nonnull AndroidClassInfo info) {
		return () -> {
			ContextMenu menu = createMenu(source, workspace, resource, bundle, info);
			populateAndroidMenu(menu, source, workspace, resource, bundle, info);
			return menu;
		};
	}

	/**
	 * @param source
	 * 		Context source.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 *
	 * @return Initial menu header for the class.
	 */
	private ContextMenu createMenu(@Nonnull ContextSource source,
								   @Nonnull Workspace workspace,
								   @Nonnull WorkspaceResource resource,
								   @Nonnull ClassBundle<? extends ClassInfo> bundle,
								   @Nonnull ClassInfo info) {
		TextProvider nameProvider;
		IconProvider iconProvider;
		if (info.isJvmClass()) {
			nameProvider = textService.getJvmClassInfoTextProvider(workspace, resource,
					(JvmClassBundle) bundle, info.asJvmClass());
			iconProvider = iconService.getJvmClassInfoIconProvider(workspace, resource,
					(JvmClassBundle) bundle, info.asJvmClass());
		} else if (info.isAndroidClass()) {
			nameProvider = textService.getAndroidClassInfoTextProvider(workspace, resource,
					(AndroidClassBundle) bundle, info.asAndroidClass());
			iconProvider = iconService.getAndroidClassInfoIconProvider(workspace, resource,
					(AndroidClassBundle) bundle, info.asAndroidClass());
		} else {
			throw new IllegalStateException("Unknown class type: " + info.getClass().getName());
		}
		ContextMenu menu = new ContextMenu();
		addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
		return menu;
	}

	/**
	 * Append JVM specific operations to the given menu.
	 *
	 * @param menu
	 * 		Menu to append content to.
	 * @param source
	 * 		Context source.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 */
	private void populateJvmMenu(@Nonnull ContextMenu menu,
								 @Nonnull ContextSource source,
								 @Nonnull Workspace workspace,
								 @Nonnull WorkspaceResource resource,
								 @Nonnull JvmClassBundle bundle,
								 @Nonnull JvmClassInfo info) {
		ObservableList<MenuItem> items = menu.getItems();
		if (source.isReference()) {
			items.add(action("menu.goto.class", CarbonIcons.ARROW_RIGHT,
					() -> actions.gotoDeclaration(workspace, resource, bundle, info)));
		} else if (source.isDeclaration()) {
			ActionMenuItem copy = action("menu.edit.copy", CarbonIcons.COPY_FILE, () -> actions.copyClass(workspace, resource, bundle, info));
			ActionMenuItem delete = action("menu.edit.delete", CarbonIcons.DELETE, () -> actions.deleteClass(workspace, resource, bundle, info));
			Menu edit = Menus.menu("menu.edit", CarbonIcons.EDIT);
			ActionMenuItem removeFields = action("menu.edit.remove.field", CarbonIcons.CLOSE, () -> actions.deleteClassFields(workspace, resource, bundle, info));
			ActionMenuItem removeMethods = action("menu.edit.remove.method", CarbonIcons.CLOSE, () -> actions.deleteClassMethods(workspace, resource, bundle, info));
			ActionMenuItem removeAnnotations = action("menu.edit.remove.annotation", CarbonIcons.CLOSE, () -> actions.deleteClassAnnotations(workspace, resource, bundle, info));
			// TODO: Implement these operations after assembler is added.
			//  - For add operations, can use the assembler, using a template for each item
			ActionMenuItem editClass = action("menu.edit.assemble.class", CarbonIcons.EDIT, () -> {
			});
			ActionMenuItem addField = action("menu.edit.add.field", CarbonIcons.ADD_ALT, () -> {
			});
			ActionMenuItem addMethod = action("menu.edit.add.method", CarbonIcons.ADD_ALT, () -> {
			});
			ActionMenuItem addAnnotation = action("menu.edit.add.annotation", CarbonIcons.ADD_ALT, () -> {
			});
			edit.getItems().addAll(
					editClass,
					addField,
					addMethod,
					addAnnotation,
					removeFields,
					removeMethods,
					removeAnnotations
			);
			items.add(edit);
			items.add(copy);
			items.add(delete);
			// Disable items if not applicable
			removeFields.setDisable(info.getFields().isEmpty());
			removeMethods.setDisable(info.getMethods().isEmpty());
			removeAnnotations.setDisable(info.getAnnotations().isEmpty());
			editClass.setDisable(true);
			addField.setDisable(true);
			addMethod.setDisable(true);
			addAnnotation.setDisable(true);
		}
		Menu refactor = Menus.menu("menu.refactor", CarbonIcons.PAINT_BRUSH);
		ActionMenuItem rename = action("menu.refactor.rename", CarbonIcons.TAG_EDIT, () -> actions.renameClass(workspace, resource, bundle, info));
		ActionMenuItem move = action("menu.refactor.move", CarbonIcons.STACKED_MOVE, () -> actions.moveClass(workspace, resource, bundle, info));
		refactor.getItems().addAll(rename, move);
		items.add(refactor);
		// TODO: implement operations
		//  - Search references
		//  - View
		//    - Class hierarchy
		//  - Deobfuscate
		//    - Suggest class name / purpose
		//    - Suggest method names / purposes (get/set)
		//    - Organize fields (constants -> finals -> non-finals
	}

	/**
	 * Append Android specific operations to the given menu.
	 *
	 * @param menu
	 * 		Menu to append content to.
	 * @param source
	 * 		Context source.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 */
	private void populateAndroidMenu(@Nonnull ContextMenu menu,
									 @Nonnull ContextSource source,
									 @Nonnull Workspace workspace,
									 @Nonnull WorkspaceResource resource,
									 @Nonnull AndroidClassBundle bundle,
									 @Nonnull AndroidClassInfo info) {
		// TODO: implement operations
		//  - Edit
		//    - (class assembler)
		//    - Add field
		//    - Add method
		//    - Add annotation
		//    - Remove fields
		//    - Remove methods
		//    - Remove annotations
		//  - Copy
		//  - Delete
		//  - Refactor
		//    - Rename
		//    - Move
		//  - Search references
		//  - View
		//    - Class hierarchy
		//  - Deobfuscate
		//    - Suggest class name / purpose
		//    - Suggest method names / purposes (get/set)
		ObservableList<MenuItem> items = menu.getItems();
		items.add(action("menu.goto.class", CarbonIcons.ARROW_RIGHT,
				() -> actions.gotoDeclaration(workspace, resource, bundle, info)));
	}
}
