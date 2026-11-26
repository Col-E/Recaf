package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikMethodAnalysisLookup;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.backend.jvm.compile.analysis.JvmMethodAnalysisLookup;
import me.darknet.assembler.compiler.ClassResult;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collections;
import java.util.List;

/**
 * Contextual assembler component that consumes assembler outputs.
 *
 * @author Matt Coley
 */
public abstract class AstBuildConsumerComponent extends ContextualAssemblerComponent implements AssemblerAstConsumer, AssemblerBuildConsumer {
	protected List<ASTElement> astElements = Collections.emptyList();
	protected volatile JvmMethodAnalysisLookup analysisLookupJvm;
	protected volatile DalvikMethodAnalysisLookup analysisLookupDalvik;
	protected ClassInfo currentClass;
	protected MethodMember currentMethod;
	protected FieldMember currentField;

	@Override
	protected void onSelectClass(@Nonnull ClassInfo declared) {
		currentClass = declared;
		currentField = null;
		currentMethod = null;
		onClassSelected();
	}

	@Override
	protected void onSelectMethod(@Nonnull ClassInfo declaring, @Nonnull MethodMember method) {
		currentClass = declaring;
		currentMethod = method;
		currentField = null;
		onMethodSelected();
	}

	@Override
	protected void onSelectField(@Nonnull ClassInfo declaring, @Nonnull FieldMember field) {
		currentClass = declaring;
		currentMethod = null;
		currentField = field;
		onFieldSelected();
	}

	@Override
	public void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase) {
		this.astElements = Collections.unmodifiableList(astElements);
		onPipelineOutputUpdate();
	}

	@Override
	public void consumeClass(@Nonnull ClassResult<?> result, @Nonnull ClassInfo classInfo) {
		if (result instanceof JavaCompileResult javaCompileResult) {
			analysisLookupJvm = javaCompileResult.analysisLookup();
			onPipelineOutputUpdate();
		} else if (result instanceof DalvikCompileResult dalvikCompileResult) {
			analysisLookupDalvik = dalvikCompileResult.analysisLookup();
			onPipelineOutputUpdate();
		}
	}

	/**
	 * Called when {@link #currentClass} is updated.
	 */
	protected void onClassSelected() {}

	/**
	 * Called when {@link #currentMethod} is updated.
	 */
	protected void onMethodSelected() {}

	/**
	 * Called when {@link #currentField} is updated.
	 */
	protected void onFieldSelected() {}

	/**
	 * Called when {@link #astElements}, {@link #analysisLookupJvm}, or {@link #analysisLookupDalvik} updates.
	 */
	protected void onPipelineOutputUpdate() {}
}
