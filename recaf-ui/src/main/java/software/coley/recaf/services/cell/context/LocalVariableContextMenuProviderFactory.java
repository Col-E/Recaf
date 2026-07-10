package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link LocalVariable} types.
 *
 * @author Matt Coley
 */
public interface LocalVariableContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param declaringMethod
	 * 		Containing method.
	 * @param variable
	 * 		The variable to create a menu for.
	 *
	 * @return Menu provider for the variable.
	 */
	@Nonnull
	default ContextMenuProvider getLocalVariableContextMenuProvider(@Nonnull ContextSource source,
	                                                                @Nonnull Workspace workspace,
	                                                                @Nonnull WorkspaceResource resource,
	                                                                @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                                                @Nonnull ClassInfo declaringClass,
	                                                                @Nonnull MethodMember declaringMethod,
	                                                                @Nonnull LocalVariable variable) {
		return emptyProvider();
	}
}
