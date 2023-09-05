package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link PackageContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicPackageContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements PackageContextMenuProviderFactory {
	@Inject
	public BasicPackageContextMenuProviderFactory(@Nonnull TextProviderService textService,
												  @Nonnull IconProviderService iconService,
												  @Nonnull Actions actions) {
		super(textService, iconService, actions);
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

			ObservableList<MenuItem> items = menu.getItems();
			if (source.isDeclaration()) {
				items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> ClipboardUtil.copyString(packageName)));
				if (bundle instanceof JvmClassBundle jvmClassBundle) {
					items.add(action("menu.edit.copy", CarbonIcons.COPY_FILE, () -> actions.copyPackage(workspace, resource, jvmClassBundle, packageName)));
					items.add(action("menu.edit.delete", CarbonIcons.TRASH_CAN, () -> actions.deletePackage(workspace, resource, jvmClassBundle, packageName)));
					Menu menuRefactor = new Menu(Lang.get("menu.refactor"));
					menuRefactor.getItems().add(action("menu.refactor.move", CarbonIcons.STACKED_MOVE, () -> actions.movePackage(workspace, resource, jvmClassBundle, packageName)));
					menuRefactor.getItems().add(action("menu.refactor.rename", CarbonIcons.TAG_EDIT, () -> actions.renamePackage(workspace, resource, jvmClassBundle, packageName)));
					items.add(menuRefactor);
				}
				// TODO: implement operations
				//  - Search references
			}

			return menu;
		};
	}
}
