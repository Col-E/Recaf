package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.COPY_LINK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.TRASH_CAN;

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

			var builder = new ContextMenuBuilder(menu, source).forResource(workspace, resource);
			if (resource.isEmbeddedResource()) {
				if (resource instanceof WorkspaceFileResource fileResource) {
					builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(fileResource.getFileInfo()));
				}
				builder.item("misc.remove", TRASH_CAN, () -> workspace.removeSupportingResource(resource));
			}

			return menu;
		};
	}
}
