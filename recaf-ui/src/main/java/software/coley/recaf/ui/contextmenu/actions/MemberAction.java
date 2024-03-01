package software.coley.recaf.ui.contextmenu.actions;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.services.cell.context.ContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * For simplifying references to methods in {@link ContextMenuProviderFactory} implementations.
 *
 * @param <B>
 * 		Bundle type.
 * @param <I>
 * 		Declaring class type.
 * @param <M>
 * 		Member type.
 *
 * @author Matt Coley
 */
public interface MemberAction<B extends Bundle<?>, I extends ClassInfo, M extends ClassMember> {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Declaring class info object.
	 * @param member
	 * 		Target member.
	 */
	void accept(@Nonnull Workspace workspace,
				@Nonnull WorkspaceResource resource,
				@Nonnull B bundle,
				@Nonnull I info,
				@Nonnull M member);
}
