package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

@Dependent
public class JvmStackAnalysisPane extends ContextualAssemblerComponent {
	// TODO: Everything

	@Override
	protected void onSelectClass(@Nonnull ClassInfo declared) {

	}

	@Override
	protected void onSelectMethod(@Nonnull ClassInfo declaring, @Nonnull MethodMember method) {

	}

	@Override
	protected void onSelectField(@Nonnull ClassInfo declaring, @Nonnull FieldMember field) {

	}
}
