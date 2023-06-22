package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.services.cell.AnnotationIconProviderFactory;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link AnnotationIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAnnotationIconProviderFactory implements AnnotationIconProviderFactory {
	private static final IconProvider ANNOTATION = Icons.createProvider(Icons.ANNOTATION);

	@Nonnull
	@Override
	public IconProvider getAnnotationIconProvider(@Nonnull Workspace workspace,
												  @Nonnull WorkspaceResource resource,
												  @Nonnull ClassBundle<? extends ClassInfo> bundle,
												  @Nonnull Annotated annotated,
												  @Nonnull AnnotationInfo annotation) {
		return ANNOTATION;
	}
}
