package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import javafx.stage.WindowEvent;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.ui.control.popup.ChangeClassVersionPopup;
import software.coley.recaf.ui.control.popup.DecompileAllPopup;
import software.coley.recaf.ui.pane.search.ClassReferenceSearchPane;
import software.coley.recaf.ui.pane.search.MemberReferenceSearchPane;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;

/**
 * Basic implementation for {@link PackageContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicPackageContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements PackageContextMenuProviderFactory {
	private final Instance<DecompileAllPopup> decompileAllPaneProvider;

	@Inject
	public BasicPackageContextMenuProviderFactory(@Nonnull Instance<DecompileAllPopup> decompileAllPaneProvider,
	                                              @Nonnull TextProviderService textService,
	                                              @Nonnull IconProviderService iconService,
	                                              @Nonnull Actions actions) {
		super(textService, iconService, actions);
		this.decompileAllPaneProvider = decompileAllPaneProvider;
	}

	@Nonnull
	@Override
	public ContextMenuProvider getPackageContextMenuProvider(@Nonnull ContextSource source,
	                                                         @Nonnull Workspace workspace,
	                                                         @Nonnull WorkspaceResource resource,
	                                                         @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                                         @Nonnull String packageName) {
		return () -> {
			TextProvider nameProvider = textService.getPackageTextProvider(workspace, resource, bundle, packageName);
			IconProvider iconProvider = iconService.getPackageIconProvider(workspace, resource, bundle, packageName);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			var builder = new ContextMenuBuilder(menu, source).forDirectory(workspace, resource, bundle, packageName);

			if (source.isDeclaration()) {
				if (bundle instanceof JvmClassBundle jvmBundle) {
					var jvmBuilder = builder.cast(JvmClassBundle.class);
					var edit = jvmBuilder.submenu("menu.edit", EDIT);
					edit.directoryItem("menu.edit.newclass", ADD_ALT, actions::newClass);
					edit.directoryItem("menu.edit.copy", COPY_FILE, actions::copyPackage);
					edit.directoryItem("menu.edit.delete", TRASH_CAN, actions::deletePackage);
					edit.item("menu.edit.changeversion", ARROWS_VERTICAL, () -> {
						ChangeClassVersionPopup popup = new ChangeClassVersionPopup();
						popup.setTargetPackage(jvmBundle, packageName);
						popup.show();
					});

					var refactor = jvmBuilder.submenu("menu.refactor", PAINT_BRUSH);
					refactor.directoryItem("menu.refactor.move", STACKED_MOVE, actions::movePackage);
					refactor.directoryItem("menu.refactor.rename", TAG_EDIT, actions::renamePackage);

					jvmBuilder.directoryItem("menu.export.package", EXPORT, actions::exportPackage);
				}
			}

			// Search actions
			var search = builder.submenu("menu.search", SEARCH);
			search.item("menu.search.class.member-references", CODE_REFERENCE, () -> {
				MemberReferenceSearchPane pane = actions.openNewMemberReferenceSearch();
				pane.ownerPredicateIdProperty().setValue(StringPredicateProvider.KEY_STARTS_WITH);
				pane.ownerValueProperty().setValue(packageName + "/");
			});
			search.item("menu.search.class.type-references", CODE_REFERENCE, () -> {
				ClassReferenceSearchPane pane = actions.openNewClassReferenceSearch();
				pane.typePredicateIdProperty().setValue(StringPredicateProvider.KEY_STARTS_WITH);
				pane.typeValueProperty().setValue(packageName + "/");
			});

			// Misc
			if (bundle instanceof JvmClassBundle jvmBundle) {
				builder.item("menu.file.decompileall", DOCUMENT_EXPORT, () -> {
					DecompileAllPopup popup = decompileAllPaneProvider.get();
					popup.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> decompileAllPaneProvider.destroy(popup));
					popup.setTargetBundle(jvmBundle);
					popup.setNamePredicate(name -> name.startsWith(packageName));
					popup.show();
				});
			}

			// Copy path
			builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(packageName));

			return menu;
		};
	}
}
