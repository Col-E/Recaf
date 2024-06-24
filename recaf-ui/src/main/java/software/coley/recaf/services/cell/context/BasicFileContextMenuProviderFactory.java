package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
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
			var builder = new ContextMenuBuilder(menu, source).forInfo(workspace, resource, bundle, info);

			FilePathNode filePath = PathNodes.filePath(workspace, resource, bundle, info);
			if (source.isReference()) {
				builder.item("menu.goto.file", ARROW_RIGHT, Unchecked.runnable(() -> actions.gotoDeclaration(filePath)));
			} else if (source.isDeclaration()) {
				builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(info));
				builder.infoItem("menu.edit.copy", COPY_FILE, actions::copyFile);
				builder.infoItem("menu.edit.delete", TRASH_CAN, actions::deleteFile);

				// Refactor actions
				var refactor = builder.submenu("menu.refactor", PAINT_BRUSH);
				refactor.infoItem("menu.refactor.move", STACKED_MOVE, actions::moveFile);
				refactor.infoItem("menu.refactor.rename", TAG_EDIT, actions::renameFile);

				// TODO: implement operations
				//  - Search references
				//  - Override text-view language (FileTypeAssociationService)

				// Export actions
				builder.infoItem("menu.export.file", EXPORT, actions::exportClass);
			}
			return menu;
		};
	}
}
