package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.ui.pane.search.ClassReferenceSearchPane;
import software.coley.recaf.ui.pane.search.MemberReferenceSearchPane;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;
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
			nameProvider = textService.getClassInfoTextProvider(workspace, resource, bundle, info.asJvmClass());
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
		var builder = new ContextMenuBuilder(menu, source).forInfo(workspace, resource, bundle, info);
		if (source.isReference()) {
			builder.infoItem("menu.goto.class", ARROW_RIGHT, actions::gotoDeclaration);
		} else if (source.isDeclaration()) {
			// Edit menu
			var edit = builder.submenu("menu.edit", EDIT);
			edit.item("menu.edit.assemble.class", EDIT, Unchecked.runnable(() ->
					actions.openAssembler(PathNodes.classPath(workspace, resource, bundle, info))
			));
			edit.infoItem("menu.edit.add.field", ADD_ALT, actions::addClassField);
			edit.infoItem("menu.edit.add.method", ADD_ALT, actions::addClassMethod);
			edit.infoItem("menu.edit.override.method", HEALTH_CROSS, actions::overrideClassMethod);
			edit.infoItem("menu.edit.remove.field", CLOSE, actions::deleteClassFields).disableWhen(info.getFields().isEmpty());
			edit.infoItem("menu.edit.remove.method", CLOSE, actions::deleteClassMethods).disableWhen(info.getMethods().isEmpty());
			edit.infoItem("menu.edit.remove.annotation", CLOSE, actions::deleteClassAnnotations).disableWhen(info.getAnnotations().isEmpty());
			edit.infoItem("menu.edit.copy", COPY_FILE, actions::copyClass);
			edit.infoItem("menu.edit.delete", COPY_FILE, actions::deleteClass);
		}

		// Search actions
		var search = builder.submenu("menu.search", SEARCH);
		search.item("menu.search.class.member-references", CODE_REFERENCE, () -> {
			MemberReferenceSearchPane pane = actions.openNewMemberReferenceSearch();
			pane.ownerPredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
			pane.ownerValueProperty().setValue(info.getName());
		});
		search.item("menu.search.class.type-references", CODE_REFERENCE, () -> {
			ClassReferenceSearchPane pane = actions.openNewClassReferenceSearch();
			pane.typePredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
			pane.typeValueProperty().setValue(info.getName());
		});

		// Refactor actions
		var refactor = builder.submenu("menu.refactor", PAINT_BRUSH);
		refactor.infoItem("menu.refactor.rename", TAG_EDIT, actions::renameClass);
		refactor.infoItem("menu.refactor.move", STACKED_MOVE, actions::moveClass);

		// Copy path
		builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(info));

		// Documentation actions
		builder.infoItem("menu.analysis.comment", ADD_COMMENT, actions::openCommentEditing);

		// Export actions
		builder.infoItem("menu.export.class", EXPORT, actions::exportClass);

		// TODO: implement operations
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
