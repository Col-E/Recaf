package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.Info;
import software.coley.recaf.services.cell.BundleIconProviderFactory;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link BundleIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicBundleIconProviderFactory implements BundleIconProviderFactory {
	private static final IconProvider CLASS_BUNDLE = Icons.createProvider(Icons.FOLDER_SRC);
	private static final IconProvider FILE_BUNDLE = Icons.createProvider(Icons.FOLDER_RES);

	@Nonnull
	@Override
	public IconProvider getBundleIconProvider(@Nonnull Workspace workspace,
											  @Nonnull WorkspaceResource resource,
											  @Nonnull Bundle<? extends Info> bundle) {
		if (bundle instanceof ClassBundle) {
			return CLASS_BUNDLE;
		} else {
			return FILE_BUNDLE;
		}
	}
}
