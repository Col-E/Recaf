package me.coley.recaf.ui.control.code.bytecode;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.*;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.assemble.parser.BytecodeLexer;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import me.coley.recaf.assemble.transformer.AstToFieldTransformer;
import me.coley.recaf.assemble.transformer.AstToMethodTransformer;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.assemble.validation.MessageLevel;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.coley.recaf.assemble.validation.bytecode.BytecodeValidator;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.AssemblerConfig;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.pane.assembler.VariableHighlighter;
import me.coley.recaf.util.WorkspaceClassSupplier;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.FieldReplacingVisitor;
import me.coley.recaf.util.visitor.MethodReplacingVisitor;
import me.coley.recaf.util.visitor.SingleMemberVisitor;
import me.coley.recaf.util.visitor.WorkspaceClassWriter;
import me.coley.recaf.workspace.resource.Resource;
import org.antlr.v4.runtime.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static me.coley.recaf.ui.control.code.ProblemOrigin.*;

/**
 * Text editing portion of the assembler UI.
 *
 * @author Matt Coley
 */
public class AssemblerArea extends SyntaxArea implements MemberEditor {
	private static final Logger logger = Logging.get(AssemblerArea.class);
	private static final int INITIAL_DELAY_MS = 1500;
	private static final int AST_LOOP_MS = 100;
	private static final ANTLRErrorStrategy ERR_RECOVER = new DefaultErrorStrategy();
	private static final ANTLRErrorStrategy ERR_JUST_FAIL = new ParserBailStrategy();
	private final List<AssemblerAstListener> astListeners = new ArrayList<>();
	private final ProblemTracking problemTracking;
	private final AtomicBoolean isUnitUpdated = new AtomicBoolean(false);
	private final AtomicBoolean hasSeenChanges = new AtomicBoolean(false);
	private final AtomicBoolean hasParseErrored = new AtomicBoolean(false);
	private final VariableHighlighter variableHighlighter = new VariableHighlighter(this);
	private ClassInfo classInfo;
	private MemberInfo targetMember;
	private ContextMenu menu;
	private Unit lastUnit;

	/**
	 * Sets up the editor area.
	 *
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public AssemblerArea(ProblemTracking problemTracking) {
		super(Languages.JAVA_BYTECODE, problemTracking);
		this.problemTracking = problemTracking;
		variableHighlighter.addIndicator(getIndicatorFactory());
		variableHighlighter.addSelectedLineListener(currentParagraphProperty());
		setupAstParseThread();
		setOnContextMenuRequested(this::onMenuRequested);
	}

	@Override
	protected void onTextChanged(PlainTextChange change) {
		super.onTextChanged(change);
		// Skip if text is empty and the change is inserting the entire content
		if (getText().equals(change.getInserted()))
			return;
		// The expectation is that the unit will most often be up-to-date as the user is likely looking around
		// more often than typing in actual changes, so less syncing will have to be done.
		isUnitUpdated.set(false);
		hasSeenChanges.set(false);
		hasParseErrored.set(false);
	}

	/**
	 * Creates the thread that updates {@link #lastUnit} in the background.
	 */
	private void setupAstParseThread() {
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("AST parse" + " #%d")
				.build();
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
		executorService.scheduleAtFixedRate(() -> {
			try {
				boolean attempted = buildAst();
				if (attempted) {
					if (problemTracking.hasProblems(ProblemLevel.ERROR))
						astListeners.forEach(l -> l.onAstBuildFail(lastUnit, problemTracking));
					else
						astListeners.forEach(l -> l.onAstBuildPass(lastUnit));
				}
			} catch (Throwable t) {
				// Shouldn't occur, but make sure its known if it does
				logger.error("Unhandled exception in the AST parse thread", t);
				astListeners.forEach(l -> l.onAstBuildCrash(lastUnit, t));
				// Set flag so that we don't re-parse broken input until there is some change by the user
				hasParseErrored.set(true);
			}
		}, INITIAL_DELAY_MS, AST_LOOP_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * @return Last parsed unit.
	 */
	public Unit getLastUnit() {
		return lastUnit;
	}

	/**
	 * @param lineNo
	 * 		Line number.
	 *
	 * @return {@link Element} of latest AST at the given line.
	 * {@code null} if no AST is available.
	 */
	public Element getElementOnLine(int lineNo) {
		if (lastUnit == null || lastUnit.getCode() == null) {
			return null;
		}
		return lastUnit.getCode().getChildOnLine(lineNo);
	}

	/**
	 * This method is run on a loop. See {@link #AST_LOOP_MS} for timing.
	 * When the text changes <i>(See: {@link #onTextChanged(PlainTextChange)})</i>
	 * we mark the unit as being not-updated.
	 * This re-parses the unit and ensures it is up-to-date with the code in the text area.
	 * If there are errors the unit retains its non up-to-date status.
	 *
	 * @return {@code true} if build was attempted.
	 * {@code false} if build was skipped.
	 */
	private boolean buildAst() {
		// Mark the current changes as being 'seen'.
		boolean haveAlreadySeen = hasSeenChanges.getAndSet(true);
		// If the unit is already updated then there's no work to be done here.
		if (isUnitUpdated.get())
			return false;
		else if (haveAlreadySeen)
			// If there are no changes, meaning the last change was already seen, we have no-reason to check
			// if the AST is any different. Instead, wait until the user updates the text and this will be called again.
			return false;
		// Ignore until error flag is cleared (done when edit is made)
		if (hasParseErrored.get())
			return false;
		// Reset errors
		problemTracking.clearOfType(BYTECODE_PARSING);
		problemTracking.clearOfType(BYTECODE_VALIDATION);
		problemTracking.clearOfType(BYTECODE_COMPILE);
		// ANTLR tokenize
		String code = getText();
		CharStream is = CharStreams.fromString(code);
		BytecodeLexer lexer = new BytecodeLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		// ANTLR parse
		BytecodeParser parser = new BytecodeParser(stream);
		parser.getErrorListeners().clear();
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
									int line, int charPositionInLine, String msg, RecognitionException e) {
				ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.WARNING, line, msg);
				problemTracking.addProblem(line, problem);
			}
		});
		if (config().attemptRecover)
			parser.setErrorHandler(ERR_RECOVER);
		else
			parser.setErrorHandler(ERR_JUST_FAIL);
		BytecodeParser.UnitContext unitCtx = null;
		try {
			unitCtx = parser.unit();
		} catch (ParserException ex) {
			// Parser problems are fatal
			int line = ex.getNode().getStart().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return true;
		}
		// Transform to our AST
		AntlrToAstTransformer antlrToAstTransformer = new AntlrToAstTransformer();
		Unit unit;
		try {
			unit = antlrToAstTransformer.visitUnit(unitCtx);
			// Done creating the AST
			lastUnit = unit;
		} catch (ParserException ex) {
			// Parser problems are fatal
			int line = ex.getNode().getStart().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return true;
		}
		// Validate
		AstValidator validator = new AstValidator(unit);
		try {
			if (config().astValidation)
				validator.visit();
		} catch (AstException ex) {
			// Some validation processes have fatal errors.
			// These are typically rare and are a result of me having a brain fart.
			int line = ex.getSource().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return true;
		}
		// Check for AST validation problems
		if (reportErrors(BYTECODE_VALIDATION, validator)) {
			return true;
		}
		// Inversely to before, we expect the unit to not be updated prior to this logic being run.
		// Now it is, so we set it to true.
		isUnitUpdated.compareAndSet(false, true);
		return true;
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
		ClassReader cr = new ClassReader(classInfo.getValue());
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
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
		}
		// Check if there is parsable AST info
		if (lastUnit == null) {
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
		int column = hitPos.getMinor();
		// Sync caret
		moveTo(hit.getInsertionIndex());
		// Create menu if needed
		if (menu == null) {
			menu = new ContextMenu();
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			// TODO: Context menu based on AST
			//  - May need to make AST model more verbose so positional resolving is easier
			//     - ClassName
			//     - MemberName
			//     - Descriptor
			Element element = null;
			List<Element> children = lastUnit.getCode().getChildren();
			double linePercent = line / (double) getParagraphs().size();
			int childIndex = (int) (children.size() * linePercent);
			if (childIndex < children.size()) {
				element = children.get(childIndex);
				int elementLine = element.getLine();
				while (elementLine < line && childIndex < children.size()) {
					childIndex++;
					element = children.get(childIndex);
					elementLine = element.getLine();
				}
				while (elementLine > line && childIndex > 0) {
					childIndex--;
					element = children.get(childIndex);
					elementLine = element.getLine();
				}
			}
			if (element != null)
				logger.info("CTX @ {} = {}", element.getLine(), element.print());
		}
		// Show at new position
		menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
		menu.requestFocus();
	}

	@Override
	public SaveResult save() {
		// The unit must be updated in order to save.
		if (!isUnitUpdated.get() || lastUnit == null)
			return SaveResult.FAILURE;
		// Generate
		if (targetMember.isMethod())
			return generateMethod();
		else
			return generateField();
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target field} if generation succeeded.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateField() {
		AstToFieldTransformer transformer = new AstToFieldTransformer(lastUnit);
		FieldNode fieldAssembled = transformer.buildField();
		if (config().bytecodeValidation) {
			BytecodeValidator bytecodeValidator = new BytecodeValidator(classInfo.getName(), fieldAssembled);
			try {
				bytecodeValidator.visit();
			} catch (BytecodeException ex) {
				// Fatal bytecode validation exception
				int line = ex.getLine();
				String msg = ex.getMessage();
				ProblemInfo problem = new ProblemInfo(BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
				problemTracking.addProblem(line, problem);
				return SaveResult.FAILURE;
			}
			// Check for bytecode validation problems
			if (reportErrors(BYTECODE_COMPILE, bytecodeValidator)) {
				return SaveResult.FAILURE;
			}
		}
		// Update field
		updateClass(fieldAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target method} if generation succeeded.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateMethod() {
		try {
			ClassSupplier supplier = WorkspaceClassSupplier.getInstance();
			AstToMethodTransformer transformer = new AstToMethodTransformer(supplier, classInfo.getName());
			transformer.setUnit(lastUnit);
			transformer.visit();
			MethodNode methodAssembled = transformer.buildMethod();
			if (config().bytecodeValidation) {
				BytecodeValidator bytecodeValidator = new BytecodeValidator(classInfo.getName(), methodAssembled);
				try {
					bytecodeValidator.visit();
				} catch (BytecodeException ex) {
					// Fatal bytecode validation exception
					int line = ex.getLine();
					String msg = ex.getMessage();
					ProblemInfo problem = new ProblemInfo(BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
					problemTracking.addProblem(line, problem);
					return SaveResult.FAILURE;
				}
				// Check for bytecode validation problems
				if (reportErrors(BYTECODE_COMPILE, bytecodeValidator)) {
					return SaveResult.FAILURE;
				}
			}
			// Update method
			updateClass(methodAssembled);
			return SaveResult.SUCCESS;
		} catch (MethodCompileException ex) {
			// Compile problems are fatal. These should be rare since most things should be caught in the prior step.
			int line = ex.getSource().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return SaveResult.FAILURE;
		}
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
		ClassReader cr = new ClassReader(classInfo.getValue());
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

	/**
	 * @param listener
	 * 		New AST listener to add.
	 */
	public void addAstListener(AssemblerAstListener listener) {
		astListeners.add(listener);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
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
