package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;

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
			var builder = new ContextMenuBuilder(menu, source).forDirectory(workspace, resource, bundle, directoryName);

			if (source.isDeclaration()) {
				builder.directoryItem("menu.edit.copy", COPY_FILE, actions::copyDirectory);
				builder.directoryItem("menu.edit.delete", TRASH_CAN, actions::deleteDirectory);

				var refactor = builder.submenu("menu.refactor", PAINT_BRUSH);
				refactor.directoryItem("menu.refactor.move", STACKED_MOVE, actions::moveDirectory);
				refactor.directoryItem("menu.refactor.rename", TAG_EDIT, actions::renameDirectory);

				builder.directoryItem("menu.export.directory", EXPORT, actions::exportDirectory);

				// TODO: implement operations
				//  - Search references
			}

			// Copy path
			builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(directoryName));

			return menu;
		};
	}
}
