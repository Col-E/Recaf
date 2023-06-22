package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.services.cell.PackageIconProviderFactory;
import software.coley.recaf.services.cell.ResourceIconProviderFactory;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link PackageIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicResourceIconProviderFactory implements ResourceIconProviderFactory {
	private static final IconProvider PROVIDER_JAR = Icons.createProvider(Icons.FILE_JAR);
	private static final IconProvider PROVIDER_ANDROID = Icons.createProvider(Icons.ANDROID);
	private static final IconProvider PROVIDER_DIR = Icons.createProvider(Icons.FOLDER);

	@Nonnull
	@Override
	public IconProvider getResourceIconProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource) {
		if (resource instanceof WorkspaceDirectoryResource)
			return PROVIDER_DIR;
		if (resource instanceof WorkspaceFileResource fileResource)
			if (fileResource.getFileInfo().getName().toLowerCase().endsWith(".apk"))
				return PROVIDER_ANDROID;
		return PROVIDER_JAR;
	}
}
