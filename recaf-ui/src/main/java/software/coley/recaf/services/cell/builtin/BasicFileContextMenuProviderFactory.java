package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link FileContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicFileContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements FileContextMenuProviderFactory {
	@Inject
	public BasicFileContextMenuProviderFactory(@Nonnull TextProviderService textService,
											   @Nonnull IconProviderService iconService,
											   @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getFileInfoContextMenuProvider(@Nonnull ContextSource source,
															  @Nonnull Workspace workspace,
															  @Nonnull WorkspaceResource resource,
															  @Nonnull FileBundle bundle,
															  @Nonnull FileInfo info) {
		return () -> {
			TextProvider nameProvider = textService.getFileInfoTextProvider(workspace, resource, bundle, info);
			IconProvider iconProvider = iconService.getFileInfoIconProvider(workspace, resource, bundle, info);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			ObservableList<MenuItem> items = menu.getItems();
			if (info.isTextFile()) {
				items.add(action("menu.goto.file", CarbonIcons.ARROW_RIGHT,
						() -> actions.gotoDeclaration(workspace, resource, bundle, info.asTextFile())));
			} else {
				// TODO: Binary file support
				//   items.add(action("menu.goto.file", CarbonIcons.ARROW_RIGHT,
				//   		() -> actions.gotoDeclaration(workspace, resource, bundle, info)));
			}

			// TODO: implement operations
			//  - Copy
			//  - Delete
			//  - Refactor
			//    - Rename
			//    - Move
			//  - Search references
			//  - Override text-view language
			return menu;
		};
	}
}
