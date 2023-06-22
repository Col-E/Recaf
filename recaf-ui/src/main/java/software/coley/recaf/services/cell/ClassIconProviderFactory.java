package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link ClassInfo classes}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface ClassIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create an icon for.
	 *
	 * @return Icon provider for the class.
	 */
	@Nonnull
	default IconProvider getJvmClassInfoIconProvider(@Nonnull Workspace workspace,
													 @Nonnull WorkspaceResource resource,
													 @Nonnull JvmClassBundle bundle,
													 @Nonnull JvmClassInfo info) {
		return emptyProvider();
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create an icon for.
	 *
	 * @return Icon provider for the class.
	 */
	@Nonnull
	default IconProvider getAndroidClassInfoIconProvider(@Nonnull Workspace workspace,
														 @Nonnull WorkspaceResource resource,
														 @Nonnull AndroidClassBundle bundle,
														 @Nonnull AndroidClassInfo info) {
		return emptyProvider();
	}
}
