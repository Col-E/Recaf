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
 * Basic implementation for {@link CatchIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicCatchIconProviderFactory implements CatchIconProviderFactory {
	@Nonnull
	@Override
	public IconProvider getCatchIconProvider(@Nonnull Workspace workspace,
											 @Nonnull WorkspaceResource resource,
											 @Nonnull ClassBundle<? extends ClassInfo> bundle,
											 @Nonnull ClassInfo declaringClass,
											 @Nonnull MethodMember declaringMethod,
											 @Nonnull String caughtType) {
		return () -> {
			Label label = new Label("catch");
			label.setTextFill(Color.web("rgb(0, 175, 255)"));
			return label;
		};
	}
}
