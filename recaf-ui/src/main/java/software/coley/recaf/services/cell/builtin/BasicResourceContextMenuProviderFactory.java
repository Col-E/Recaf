package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link ResourceContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicResourceContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements ResourceContextMenuProviderFactory {
	@Inject
	public BasicResourceContextMenuProviderFactory(@Nonnull TextProviderService textService,
												   @Nonnull IconProviderService iconService,
												   @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getResourceContextMenuProvider(@Nonnull ContextSource source,
															  @Nonnull Workspace workspace,
															  @Nonnull WorkspaceResource resource) {
		return () -> {
			// TODO: Need to make 'thing to string' an injectable service too.
			//      Though for this, I don't think we need to make plugin support, so the impl can be much simpler than graphic/menu stuff
			String name = resource.getClass().getSimpleName();

			IconProvider iconProvider = iconService.getResourceIconProvider(workspace, resource);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, name, iconProvider.makeIcon());
			// TODO: Implement operations
			//   - Remove (support only)
			return menu;
		};
	}
}
