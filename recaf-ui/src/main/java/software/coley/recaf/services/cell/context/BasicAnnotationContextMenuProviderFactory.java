package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.ARROW_RIGHT;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.TRASH_CAN;
import static software.coley.collections.Unchecked.runnable;

/**
 * Basic implementation for {@link AnnotationContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAnnotationContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements AnnotationContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicAnnotationContextMenuProviderFactory.class);

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
			var builder = new ContextMenuBuilder(menu, source).forAnnotation(workspace, resource, bundle, annotated, annotation);

			String annotationType = Type.getType(annotation.getDescriptor()).getInternalName();

			ClassPathNode annotationDecPath = workspace.findClass(annotationType);
			if (annotationDecPath != null)
				builder.item("menu.goto.class", ARROW_RIGHT, runnable(() -> actions.gotoDeclaration(annotationDecPath)));

			builder.item("menu.edit.remove.annotation", TRASH_CAN, () -> actions.immediateDeleteAnnotations(bundle, annotated, annotationType));

			return menu;
		};
	}


}
