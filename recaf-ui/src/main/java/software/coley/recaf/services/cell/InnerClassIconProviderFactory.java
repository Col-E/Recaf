package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link InnerClassInfo inner classes}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface InnerClassIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param outerClass
	 * 		Outer class.
	 * @param inner
	 * 		The inner class to create an icon for.
	 *
	 * @return Icon provider for the inner class.
	 */
	@Nonnull
	default IconProvider getInnerClassInfoIconProvider(@Nonnull Workspace workspace,
													   @Nonnull WorkspaceResource resource,
													   @Nonnull ClassBundle<? extends ClassInfo> bundle,
													   @Nonnull ClassInfo outerClass,
													   @Nonnull InnerClassInfo inner) {
		return emptyProvider();
	}
}
