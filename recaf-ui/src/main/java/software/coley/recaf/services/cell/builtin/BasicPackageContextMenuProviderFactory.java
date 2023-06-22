package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

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
