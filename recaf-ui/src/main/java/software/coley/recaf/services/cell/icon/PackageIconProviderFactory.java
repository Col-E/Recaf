package software.coley.recaf.services.cell.icon;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for packages <i>(paths in {@link JvmClassBundle} and {@link AndroidClassBundle})</i>,
 * to be plugged into {@link IconProviderService} to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface PackageIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		The full package name, separated by {@code /}.
	 *
	 * @return Icon provider for the package.
	 */
	@Nonnull
	default IconProvider getPackageIconProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource,
												@Nonnull ClassBundle<? extends ClassInfo> bundle,
												@Nonnull String packageName) {
		return emptyProvider();
	}
}
