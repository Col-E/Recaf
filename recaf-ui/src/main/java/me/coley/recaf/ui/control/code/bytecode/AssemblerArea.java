package me.coley.recaf.ui.control.code.bytecode;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.*;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.*;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.assemble.validation.MessageLevel;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.AssemblerConfig;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.pane.assembler.FlowHighlighter;
import me.coley.recaf.ui.pane.assembler.VariableHighlighter;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.util.visitor.*;
import me.coley.recaf.workspace.resource.Resource;
import me.darknet.assembler.parser.AssemblerException;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static me.coley.recaf.ui.control.code.ProblemOrigin.BYTECODE_PARSING;
import static me.coley.recaf.ui.control.code.ProblemOrigin.BYTECODE_VALIDATION;

/**
 * Text editing portion of the assembler UI.
 *
 * @author Matt Coley
 */
public class AssemblerArea extends SyntaxArea implements MemberEditor,
		AstValidationListener, BytecodeValidationListener, ParserFailureListener, BytecodeFailureListener {
	private static final Logger logger = Logging.get(AssemblerArea.class);
	private static final int INITIAL_DELAY_MS = 500;
	private static final int AST_LOOP_MS = 100;
	private final ProblemTracking problemTracking;
	private final AssemblerPipeline pipeline;
	private ClassInfo classInfo;
	private MemberInfo targetMember;
	private ContextMenu menu;
	private ScheduledFuture<?> astParseThread;

	/**
	 * Sets up the editor area.
	 *
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public AssemblerArea(ProblemTracking problemTracking, AssemblerPipeline pipeline) {
		super(Languages.JAVA_BYTECODE, problemTracking);
		this.problemTracking = problemTracking;
		this.pipeline = pipeline;
		// Setup variable highlighting
		VariableHighlighter variableHighlighter = new VariableHighlighter(pipeline, this);
		variableHighlighter.addIndicator(getIndicatorFactory());
		variableHighlighter.addSelectedLineListener(currentParagraphProperty());
		// Setup flow highlighting
		FlowHighlighter flowHighlighter = new FlowHighlighter(pipeline, this);
		flowHighlighter.addIndicator(getIndicatorFactory());
		flowHighlighter.addSelectedLineListener(currentParagraphProperty());
		// AST parsing loop
		setupAstParseThread();
		// Context menu support
		setOnContextMenuRequested(this::onMenuRequested);
		// Register listeners to hook into problem tracking
		pipeline.addParserFailureListener(this);
		pipeline.addBytecodeFailureListener(this);
		pipeline.addBytecodeValidationListener(this);
		/*pipeline.addAntlrErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
									int line, int charPositionInLine, String msg, RecognitionException e) {
				ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.WARNING, line, msg);
				problemTracking.addProblem(line, problem);
			}
		});*/
		boolean recover = Configs.assembler().attemptRecover;
		boolean validate = Configs.assembler().astValidation;
		if (validate) {
			pipeline.addAstValidationListener(this);
		}
	}

	@Override
	protected void onTextChanged(PlainTextChange change) {
		super.onTextChanged(change);
		// Update unit
		pipeline.setText(getText());
	}

	@Override
	public void cleanup() {
		super.cleanup();
		// Stop parse thread
		astParseThread.cancel(true);
	}

	/**
	 * Creates the thread that updates the AST in the background.
	 */
	private void setupAstParseThread() {
		astParseThread = ThreadUtil.scheduleAtFixedRate(() -> {
			try {
				if (pipeline.updateAst() && pipeline.validateAst()) {
					logger.trace("AST updated and validated");
					if (pipeline.isMethod() &&
							pipeline.isOutputOutdated() &&
							pipeline.generateMethod())
						logger.trace("AST compiled to method and analysis executed");
				}
			} catch (Throwable t) {
				// Shouldn't occur, but make sure its known if it does
				logger.error("Unhandled exception in the AST parse thread", t);
			}
		}, INITIAL_DELAY_MS, AST_LOOP_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Disassembles the {@link #getTargetMember() target member} and updates the editor's text.
	 */
	public void disassemble() {
		if (classInfo == null) {
			logger.warn("Cannot disassemble, target class info missing");
			return;
		}
		if (targetMember == null) {
			logger.warn("Cannot disassemble, target member info missing");
			return;
		}
		// Get the target member node
		ClassNode node = new ClassNode();
		ClassReader cr = classInfo.getClassReader();
		cr.accept(new SingleMemberVisitor(node, targetMember), ClassReader.SKIP_FRAMES);
		// Since we visit only the target member info, there should only be one member in the list.
		if (targetMember.isMethod() && node.methods.isEmpty()) {
			logger.error("Failed to isolate method for disassembling '{}.{}{}'",
					node.name, targetMember.getName(), targetMember.getDescriptor());
			return;
		} else if (targetMember.isField() && node.fields.isEmpty()) {
			logger.error("Failed to isolate field for disassembling '{}.{} {}'",
					node.name, targetMember.getName(), targetMember.getDescriptor());
			return;
		}
		// Disassemble
		BytecodeToAstTransformer transformer;
		if (targetMember.isField()) {
			FieldNode field = node.fields.get(0);
			transformer = new BytecodeToAstTransformer(field);
		} else {
			MethodNode method = node.methods.get(0);
			transformer = new BytecodeToAstTransformer(method);
		}
		transformer.visit();
		Unit unit = transformer.getUnit();
		String code = unit.print();
		// Update text
		setText(code);
		// Also attempt to recompile once the code is set.
		// We do not want to update the class, this is to initialize the pipeline state without the user needing
		// to manually trigger a save first.
		pipeline.updateAst();
		if (pipeline.isMethod())
			pipeline.generateMethod();
		else
			pipeline.generateField();
		SaveResult initialBuild = targetMember.isMethod() ? generateMethod(false) : generateField(false);
		if (initialBuild == SaveResult.SUCCESS)
			logger.trace("Initial build of disassemble successful!");
		else
			logger.trace("Initial build of disassemble failed!");
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
		}
		// Check if there is parsable AST info
		if (pipeline.getUnit() == null) {
			// TODO: More visually noticeable warning to user that the AST failed to be parsed
			//  - Offer to switch class representation?
			logger.warn("Could not request context menu since the code is not parsable!");
			return;
		}
		// Convert the event position to line/column
		CharacterHit hit = hit(e.getX(), e.getY());
		Position hitPos = offsetToPosition(hit.getInsertionIndex(),
				TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Position is 0 indexed
		// Sync caret
		moveTo(hit.getInsertionIndex());
		// Create menu if needed
		if (menu == null) {
			menu = new ContextMenu();
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			Element element = pipeline.getElementOnLine(line);
			if (element != null) {
				// TODO: Context menu based on AST
				// TODO: Sub-menu for more accurate selections (class/member names in part of AST selected)
				logger.info("TODO: CTX @ {} = {}", element.getLine(), element.print());
			}
		}
		// Show at new position
		menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
		menu.requestFocus();
	}

	@Override
	public SaveResult save() {
		// The unit must be updated in order to save.
		if (pipeline.getUnit() == null)
			return SaveResult.FAILURE;
		// Errors must be resolved before generating the field/method
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Generate
		if (targetMember.isMethod())
			return generateMethod(true);
		else if(targetMember.isField())
			return generateField(true);
		else
			return generateClass(true);
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target field} if generation succeeded.
	 *
	 * @param apply
	 *        {@code true} to update the {@link #getCurrentClassInfo() declaring class} with the generated field.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateField(boolean apply) {
		// Generate field
		if (!pipeline.generateField())
			return SaveResult.FAILURE;
		FieldNode fieldAssembled = pipeline.getLastField();
		// Check if there were reported errors
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Update field
		if (apply) updateClass(fieldAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target method} if generation succeeded.
	 *
	 * @param apply
	 *        {@code true} to update the {@link #getCurrentClassInfo() declaring class} with the generated method.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateMethod(boolean apply) {
		// Generate method if not up-to-date
		if (pipeline.isUnitOutdated() && !pipeline.generateMethod())
			return SaveResult.FAILURE;
		MethodNode methodAssembled = pipeline.getLastMethod();
		// Check if there were reported errors
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Update field
		if (apply) updateClass(methodAssembled);
		return SaveResult.SUCCESS;
	}

	private SaveResult generateClass(boolean apply) {

		if(pipeline.isUnitOutdated() && !pipeline.generateClass())
			return SaveResult.FAILURE;

		ClassNode classAssembled = pipeline.getLastClass();

		if(problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;

		if(apply) updateClass(classAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Called to update the {@link #getTargetMember() target field}.
	 *
	 * @param updatedField
	 * 		Field that was compiled.
	 */
	private void updateClass(FieldNode updatedField) {
		updateClass(cw -> new FieldReplacingVisitor(cw, targetMember, updatedField));
	}

	/**
	 * Called to update the {@link #getTargetMember() target method}.
	 *
	 * @param updatedMethod
	 * 		Method that was compiled.
	 */
	private void updateClass(MethodNode updatedMethod) {
		updateClass(cw -> new MethodReplacingVisitor(cw, targetMember, updatedMethod));
	}

	private void updateClass(ClassNode updatedClass) {
		updateClass(cw -> new ClassReplacingVisitor(cw, targetMember, updatedClass));
	}

	/**
	 * @param replacerProvider
	 * 		Function to map a class-writer to a delegated class visitor.
	 *
	 * @see #updateClass(MethodNode)
	 * @see #updateClass(FieldNode)
	 */
	private void updateClass(Function<ClassWriter, ClassVisitor> replacerProvider) {
		// Because we are creating a new method, we need to generate frames.
		int flags = ClassWriter.COMPUTE_FRAMES;
		ClassWriter cw = new WorkspaceClassWriter(RecafUI.getController(), flags);
		ClassVisitor replacer = replacerProvider.apply(cw);
		ClassReader cr = classInfo.getClassReader();
		// Read and filter through replacer. Skip frames since we're just going to compute them anyways.
		cr.accept(replacer, ClassReader.SKIP_FRAMES);
		// Done, update the workspace
		byte[] updatedClass = cw.toByteArray();
		Resource resource = RecafUI.getController().getWorkspace().getResources().getPrimary();
		resource.getClasses().put(ClassInfo.read(updatedClass));
	}

	/**
	 * @param validator
	 * 		Validator that was visited.
	 *
	 * @return {@code true} if errors were reported.
	 * {@code false} if no errors were reported.
	 */
	private boolean reportErrors(ProblemOrigin origin, Validator<?> validator) {
		// These are validation messages that aren't logical killers, but may prevent actual further processing.
		boolean hasErrors = false;
		problemTracking.clearOfType(origin);
		for (ValidationMessage message : validator.getMessages()) {
			if (!hasErrors && message.getLevel() == MessageLevel.ERROR) {
				hasErrors = true;
			}
			int line = message.getSource().getLine();
			String msg = message.getMessage();
			ProblemLevel level;
			switch (message.getLevel()) {
				case INFO:
					level = ProblemLevel.INFO;
					break;
				case WARN:
					level = ProblemLevel.WARNING;
					break;
				case ERROR:
				default:
					level = ProblemLevel.ERROR;
					break;
			}
			ProblemInfo problem = new ProblemInfo(origin, level, line, msg);
			problemTracking.addProblem(line, problem);
		}
		return hasErrors;
	}

	@Override
	public void onAstValidationBegin(Unit unit) {
		// Validation implies parse completed successfully
		problemTracking.clearOfType(BYTECODE_PARSING);
	}

	@Override
	public void onAstValidationComplete(Unit unit, Validator<?> validator) {
		reportErrors(BYTECODE_VALIDATION, validator);
	}

	@Override
	public void onBytecodeValidationBegin(Object object) {
		// no-op, bytecode can be compiled even with validation warnings
	}

	@Override
	public void onBytecodeValidationComplete(Object object, Validator<?> validator) {
		reportErrors(BYTECODE_VALIDATION, validator);
	}

	@Override
	public void onParseFail(AssemblerException ex) {
		int line = ex.getLocation().getLine();
		String msg = ex.describe();
		ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onParserTransformFail(AssemblerException ex) {
		int line = ex.getLocation().getLine();
		String msg = ex.describe();
		ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onAstValidationError(AstException ex) {
		int line = ex.getSource().getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onValidationFailure(Object object, BytecodeException ex) {
		int line = ex.getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onCompileFailure(Unit unit, MethodCompileException ex) {
		int line = ex.getSource().getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		pipeline.setType(newValue.getName());
		if (newValue instanceof ClassInfo) {
			classInfo = (ClassInfo) newValue;
		}
	}

	@Override
	public MemberInfo getTargetMember() {
		return targetMember;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		this.targetMember = targetMember;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public boolean supportsMemberSelection() {
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op, this represents an actual member so nothing to select
	}

	private static AssemblerConfig config() {
		return Configs.assembler();
	}
}
