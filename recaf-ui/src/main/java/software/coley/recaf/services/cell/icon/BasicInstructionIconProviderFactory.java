package software.coley.recaf.services.cell.icon;

import dev.xdark.blw.code.Instruction;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link InstructionIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicInstructionIconProviderFactory implements InstructionIconProviderFactory {
	@Nonnull
	@Override
	public IconProvider getInstructionIconProvider(@Nonnull Workspace workspace,
												   @Nonnull WorkspaceResource resource,
												   @Nonnull ClassBundle<? extends ClassInfo> bundle,
												   @Nonnull ClassInfo declaringClass,
												   @Nonnull MethodMember declaringMethod,
												   @Nonnull Instruction instruction) {
		return () -> new FontIconView(CarbonIcons.CODE, Color.web("rgb(0, 175, 255)"));
	}
}
