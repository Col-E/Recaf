package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.cell.FieldIconProviderFactory;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link FieldIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicFieldIconProviderFactory implements FieldIconProviderFactory {
	private static final IconProvider FIELD = Icons.createProvider(Icons.FIELD);
	private static final IconProvider ACCESS_FINAL = Icons.createProvider(Icons.ACCESS_FINAL);
	private static final IconProvider ACCESS_STATIC = Icons.createProvider(Icons.ACCESS_STATIC);

	@Nonnull
	@Override
	public IconProvider getFieldMemberIconProvider(@Nonnull Workspace workspace,
												   @Nonnull WorkspaceResource resource,
												   @Nonnull ClassBundle<? extends ClassInfo> bundle,
												   @Nonnull ClassInfo declaringClass,
												   @Nonnull FieldMember field) {
		return () -> fieldIconProvider(field);
	}

	private static Node fieldIconProvider(FieldMember field) {
		// Base
		StackPane stack = new StackPane();
		ObservableList<Node> children = stack.getChildren();
		children.add(FIELD.makeIcon());

		// Add overlay for certain flags.
		if (field.hasFinalModifier())
			children.add(ACCESS_FINAL.makeIcon());
		if (field.hasStaticModifier())
			children.add(ACCESS_STATIC.makeIcon());

		// Wrap with visibility.
		return new HBox(stack, Icons.getVisibilityIcon(field.getAccess()));
	}
}
