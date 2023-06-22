package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Icon provider for {@link MethodMember method}, to be plugged into {@link IconProviderService}
 * to allow for third party icon customization.
 *
 * @author Matt Coley
 */
public interface MethodIconProviderFactory extends IconProviderFactory {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param method
	 * 		The method to create an icon for.
	 *
	 * @return Icon provider for the method.
	 */
	@Nonnull
	default IconProvider getMethodMemberIconProvider(@Nonnull Workspace workspace,
													 @Nonnull WorkspaceResource resource,
													 @Nonnull ClassBundle<? extends ClassInfo> bundle,
													 @Nonnull ClassInfo declaringClass,
													 @Nonnull MethodMember method) {
		return emptyProvider();
	}
}
