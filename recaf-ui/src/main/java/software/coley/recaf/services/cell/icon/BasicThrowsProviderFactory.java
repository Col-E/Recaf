package software.coley.recaf.services.cell.icon;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link ThrowsIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicThrowsProviderFactory implements ThrowsIconProviderFactory {
	@Nonnull
	@Override
	public IconProvider getThrowsIconProvider(@Nonnull Workspace workspace,
											  @Nonnull WorkspaceResource resource,
											  @Nonnull ClassBundle<? extends ClassInfo> bundle,
											  @Nonnull ClassInfo declaringClass,
											  @Nonnull MethodMember declaringMethod,
											  @Nonnull String thrownType) {
		return () -> {
			Label label = new Label("throws");
			label.setTextFill(Color.web("rgb(0, 175, 255)"));
			return label;
		};
	}
}
