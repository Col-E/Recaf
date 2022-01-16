package me.coley.recaf.ui.pane.assembler;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javassist.CannotCompileException;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.assemble.transformer.ExpressionToAsmTransformer;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.control.code.bytecode.AssemblerAstListener;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A subcomponent of {@link AssemblerPane} that lets users immediately see the bytecode of some Java expression.
 *
 * @author Matt Coley
 */
public class ExpressionPlaygroundPane extends BorderPane implements MemberEditor, AssemblerAstListener {
	private static final Logger logger = Logging.get(ExpressionPlaygroundPane.class);
	private final ProblemTracking editorProblems;
	private final SyntaxArea editor;
	private final SyntaxArea preview;
	private CommonClassInfo declaringClass;
	private MethodInfo declaringMethod;
	private Unit unit;

	/**
	 * New playground pane.
	 */
	public ExpressionPlaygroundPane() {
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
		if (unit == null || isDisabled()) {
			return;
		}
		// Reset problems
		editorProblems.clearOfType(ProblemOrigin.BYTECODE_PARSING);
		editorProblems.clearOfType(ProblemOrigin.JAVA_COMPILE);
		// Start
		ClassSupplier classSupplier = ExpressionPlaygroundPane::findWorkspaceClass;
		String selfType = declaringClass.getName();
		if (selfType == null) {
			selfType = "java/lang/Object";
		}
		MethodDefinition definition = (MethodDefinition) unit.getDefinition();
		// Only need parameter variables
		Variables variables = new Variables();
		try {
			variables.visitDefinition(selfType, definition);
			variables.visitParams(definition);
		} catch (Exception ex) {
			writeError(ex);
			logger.error("Could not extract variables from method definition", ex);
			editorProblems.addProblem(-1, new ProblemInfo(ProblemOrigin.BYTECODE_PARSING, ProblemLevel.ERROR, -1,
					"Could not extract variables from method definition, check logs"));
		}
		ExpressionToAsmTransformer toAsmTransformer
				= new ExpressionToAsmTransformer(classSupplier, definition, variables, selfType);
		ExpressionToAstTransformer toAstTransformer
				= new ExpressionToAstTransformer(definition, variables, toAsmTransformer);
		toAstTransformer.setLabelPrefixFunction(e -> "demo_");
		try {
			Code code = toAstTransformer.transform(new Expression(source));
			if (code != null) {
				String bytecode = code.print();
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
		StringWriter trace = new StringWriter();
		ex.printStackTrace(new PrintWriter(trace));
		preview.setText("// " + trace.toString().replace("\n", "\n// "));
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

	@Override
	public void onAstBuildPass(Unit unit) {
		this.unit = unit;
		// Method only
		setDisable(unit.isField());
	}

	@Override
	public void onAstBuildFail(Unit unit, ProblemTracking problemTracking) {
		// no-op
	}

	@Override
	public void onAstBuildCrash(Unit unit, Throwable reason) {
		// no-op
	}

	private static byte[] findWorkspaceClass(String name) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null) {
			ClassInfo clazz = workspace.getResources().getClass(name);
			if (clazz != null) {
				return clazz.getValue();
			}
		}
		return null;
	}
}
