package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.services.cell.DirectoryIconProviderFactory;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link DirectoryIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicDirectoryIconProviderFactory implements DirectoryIconProviderFactory {
	private static final IconProvider PROVIDER = Icons.createProvider(Icons.FOLDER);

	@Nonnull
	@Override
	public IconProvider getDirectoryIconProvider(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull String directoryName) {
		return PROVIDER;
	}
}
