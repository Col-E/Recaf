package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

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
			// TODO: implement operations
			//  - Copy
			//  - Delete
			//  - Refactor
			//    - Rename
			//    - Move
			//  - Search references
			return menu;
		};
	}
}
