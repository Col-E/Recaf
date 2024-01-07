package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.Info;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.TRASH_CAN;

/**
 * Basic implementation for {@link BundleContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicBundleContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements BundleContextMenuProviderFactory {

	@Inject
	public BasicBundleContextMenuProviderFactory(@Nonnull TextProviderService textService,
												 @Nonnull IconProviderService iconService,
												 @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getBundleContextMenuProvider(@Nonnull ContextSource source,
															@Nonnull Workspace workspace,
															@Nonnull WorkspaceResource resource,
															@Nonnull Bundle<? extends Info> bundle) {
		return () -> {
			TextProvider nameProvider = textService.getBundleTextProvider(workspace, resource, bundle);
			IconProvider iconProvider = iconService.getBundleIconProvider(workspace, resource, bundle);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			var builder = new ContextMenuBuilder(menu, source).forBundle(workspace, resource, bundle);
			builder.item("misc.clear", TRASH_CAN, bundle::clear);
			return menu;
		};
	}
}
