package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link DirectoryContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicDirectoryContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements DirectoryContextMenuProviderFactory {

	@Inject
	public BasicDirectoryContextMenuProviderFactory(@Nonnull TextProviderService textService,
													@Nonnull IconProviderService iconService,
													@Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getDirectoryContextMenuProvider(@Nonnull ContextSource source,
															   @Nonnull Workspace workspace,
															   @Nonnull WorkspaceResource resource,
															   @Nonnull FileBundle bundle,
															   @Nonnull String directoryName) {
		return () -> {
			TextProvider nameProvider = textService.getDirectoryTextProvider(workspace, resource, bundle, directoryName);
			IconProvider iconProvider = iconService.getDirectoryIconProvider(workspace, resource, bundle, directoryName);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			ObservableList<MenuItem> items = menu.getItems();
			if (source.isDeclaration()) {
				items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> ClipboardUtil.copyString(directoryName)));
				items.add(action("menu.edit.copy", CarbonIcons.COPY_FILE, () -> actions.copyDirectory(workspace, resource, bundle, directoryName)));
				items.add(action("menu.edit.delete", CarbonIcons.TRASH_CAN, () -> actions.deleteDirectory(workspace, resource, bundle, directoryName)));
				Menu menuRefactor = new Menu(Lang.get("menu.refactor"));
				menuRefactor.getItems().add(action("menu.refactor.move", CarbonIcons.STACKED_MOVE, () -> actions.moveDirectory(workspace, resource, bundle, directoryName)));
				menuRefactor.getItems().add(action("menu.refactor.rename", CarbonIcons.TAG_EDIT, () -> actions.renameDirectory(workspace, resource, bundle, directoryName)));
				items.add(menuRefactor);

				// TODO: implement operations
				//  - Search references
			}
			return menu;
		};
	}
}
