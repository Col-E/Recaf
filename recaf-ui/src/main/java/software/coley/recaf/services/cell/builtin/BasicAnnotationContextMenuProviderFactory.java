package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link AnnotationContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAnnotationContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements AnnotationContextMenuProviderFactory {
	@Inject
	public BasicAnnotationContextMenuProviderFactory(@Nonnull TextProviderService textService,
													 @Nonnull IconProviderService iconService,
													 @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getAnnotationContextMenuProvider(@Nonnull ContextSource source,
																@Nonnull Workspace workspace,
																@Nonnull WorkspaceResource resource,
																@Nonnull ClassBundle<? extends ClassInfo> bundle,
																@Nonnull Annotated annotated,
																@Nonnull AnnotationInfo annotation) {
		return () -> {
			TextProvider nameProvider = textService.getAnnotationTextProvider(workspace, resource, bundle, annotated, annotation);
			IconProvider iconProvider = iconService.getAnnotationIconProvider(workspace, resource, bundle, annotated, annotation);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			return menu;
		};
	}
}
