package me.coley.recaf.ui.pane.assembler;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javassist.CannotCompileException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.arch.Definition;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.transformer.ExpressionToAsmTransformer;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.WorkspaceClassSupplier;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

/**
 * A subcomponent of {@link AssemblerPane} that lets users immediately see the bytecode of some Java expression.
 *
 * @author Matt Coley
 */
public class ExpressionPlaygroundPane extends BorderPane implements MemberEditor {
	private static final Logger logger = Logging.get(ExpressionPlaygroundPane.class);
	private final AssemblerPipeline pipeline;
	private final ProblemTracking editorProblems;
	private final SyntaxArea editor;
	private final SyntaxArea preview;
	private CommonClassInfo declaringClass;
	private MethodInfo declaringMethod;

	/**
	 * New playground pane.
	 *
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public ExpressionPlaygroundPane(AssemblerPipeline pipeline) {
		this.pipeline = pipeline;
		editorProblems = new ProblemTracking();
		preview = new SyntaxArea(Languages.JAVA_BYTECODE, new ProblemTracking());
		editor = new SyntaxArea(Languages.JAVA, editorProblems);
		editor.setText("// " + Lang.get("assembler.playground.comment") + "\n" +
				"System.out.println(\"Hello world!\");");
		editor.textProperty().addListener((ob, o, n) -> updateBytecodePreview(n));

		Node errorDisplay = new ErrorDisplay(editor, editorProblems);
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));

		StackPane editorStack = new StackPane();
		editorStack.getChildren().add(new VirtualizedScrollPane<>(editor));
		editorStack.getChildren().add(errorDisplay);

		SplitPane split = new SplitPane(
				editorStack,
				new VirtualizedScrollPane<>(preview));
		split.setDividerPosition(0, 0.5);
		setCenter(split);
		setDisable(true);
	}

	private void updateBytecodePreview(String source) {
		// Skip until ready
		if (pipeline.getUnit() == null || pipeline.getLastVariables() == null || isDisabled()) {
			return;
		}
		// Reset problems
		editorProblems.clearOfType(ProblemOrigin.BYTECODE_PARSING);
		editorProblems.clearOfType(ProblemOrigin.JAVA_COMPILE);
		// Start
		ClassSupplier classSupplier = WorkspaceClassSupplier.getInstance();
		String selfType = declaringClass.getName();
		if (selfType == null) {
			selfType = "java/lang/Object";
		}
		Definition definition = pipeline.getUnit().getDefinition();
		if (!definition.isMethod()) {
			return;
		}
		MethodDefinition methodDefinition = (MethodDefinition) definition;
		Variables variables = pipeline.getLastVariables();
		// Setup expression transformer
		ExpressionToAsmTransformer toAsmTransformer
				= new ExpressionToAsmTransformer(classSupplier, methodDefinition, variables, selfType);
		ExpressionToAstTransformer toAstTransformer
				= new ExpressionToAstTransformer(methodDefinition, variables, toAsmTransformer);
		toAstTransformer.setLabelPrefixFunction(e -> "demo_");
		// Transform our expression
		try {
			Code code = toAstTransformer.transform(new Expression(source));
			if (code != null) {
				String bytecode = code.print(Configs.assembler().createContext());
				preview.setText(bytecode);
			} else {
				preview.setText("// No code emitted");
			}
		} catch (CannotCompileException ex) {
			// This happens for syntax errors, which happen often since the user is likely typing out the code they want
			Animations.animateWarn(editor, 1000);
			editorProblems.addProblem(-1, new ProblemInfo(ProblemOrigin.JAVA_COMPILE, ProblemLevel.ERROR, -1,
					"Could not compile playground expression: " + ex.getMessage()));
		} catch (Exception ex) {
			// Other types of errors we are more concerned about
			Animations.animateFailure(editor, 2000);
			writeError(ex);
			logger.error("Could not compile playground expression", ex);
			editorProblems.addProblem(-1, new ProblemInfo(ProblemOrigin.JAVA_COMPILE, ProblemLevel.ERROR, -1,
					"Could not compile playground expression, check logs"));
		}
	}

	private void writeError(Exception ex) {
		String trace = StringUtil.traceToString(ex);
		preview.setText("// " + trace.replace("\n", "\n// "));
	}

	@Override
	public MemberInfo getTargetMember() {
		return declaringMethod;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		if (targetMember instanceof MethodInfo) {
			declaringMethod = (MethodInfo) targetMember;
		} else {
			throw new IllegalStateException("Only methods allowed in the expression playground!");
		}
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		declaringClass = newValue;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return declaringClass;
	}

	@Override
	public boolean supportsMemberSelection() {
		// Not relevant here
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		// Not relevant here
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// Not relevant here
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}
}
