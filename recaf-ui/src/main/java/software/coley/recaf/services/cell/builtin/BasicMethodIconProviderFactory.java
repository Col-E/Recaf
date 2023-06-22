package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.services.cell.MethodIconProviderFactory;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link MethodIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicMethodIconProviderFactory implements MethodIconProviderFactory {
	private static final IconProvider METHOD = Icons.createProvider(Icons.METHOD);
	private static final IconProvider METHOD_ABSTRACT = Icons.createProvider(Icons.METHOD_ABSTRACT);
	private static final IconProvider ACCESS_FINAL = Icons.createProvider(Icons.ACCESS_FINAL);
	private static final IconProvider ACCESS_STATIC = Icons.createProvider(Icons.ACCESS_STATIC);

	@Nonnull
	@Override
	public IconProvider getMethodMemberIconProvider(@Nonnull Workspace workspace,
													@Nonnull WorkspaceResource resource,
													@Nonnull ClassBundle<? extends ClassInfo> bundle,
													@Nonnull ClassInfo declaringClass,
													@Nonnull MethodMember method) {
		return () -> methodIconProvider(method);
	}

	private static Node methodIconProvider(MethodMember method) {
		// Base
		StackPane stack = new StackPane();
		ObservableList<Node> children = stack.getChildren();
		if (method.hasAbstractModifier())
			children.add(METHOD_ABSTRACT.makeIcon());
		else
			children.add(METHOD.makeIcon());

		// Add overlay for certain flags.
		if (method.hasFinalModifier())
			children.add(ACCESS_FINAL.makeIcon());
		if (method.hasStaticModifier())
			children.add(ACCESS_STATIC.makeIcon());

		// Wrap with visibility.
		return new HBox(stack, Icons.getVisibilityIcon(method.getAccess()));
	}
}
