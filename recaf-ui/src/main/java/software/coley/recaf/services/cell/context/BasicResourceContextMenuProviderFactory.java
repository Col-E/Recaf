package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
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
			String name = textService.getResourceTextProvider(workspace, resource).makeText();
			IconProvider iconProvider = iconService.getResourceIconProvider(workspace, resource);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, name, iconProvider.makeIcon());
			// TODO: Implement operations
			//   - var builder = new ContextMenuBuilder(menu, source).forResource(workspace, resource);
			//   - Remove (support only)
			return menu;
		};
	}
}
