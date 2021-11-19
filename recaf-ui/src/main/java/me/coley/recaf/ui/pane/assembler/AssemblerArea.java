package me.coley.recaf.ui.pane.assembler;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.*;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
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
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.FieldReplacingVisitor;
import me.coley.recaf.util.visitor.MethodReplacingVisitor;
import me.coley.recaf.util.visitor.SingleMemberVisitor;
import me.coley.recaf.util.visitor.WorkspaceClassWriter;
import me.coley.recaf.workspace.resource.Resource;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.TwoDimensional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

/**
 * Text editing portion of the assembler UI.
 *
 * @author Matt Coley
 */
public class AssemblerArea extends SyntaxArea implements MemberEditor {
	private static final Logger logger = Logging.get(AssemblerArea.class);
	private final ProblemTracking problemTracking;
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
		setOnContextMenuRequested(this::onMenuRequested);

		// TODO: Add text listener to update AST
		//   - currently stuff is only done on-request (on-save)
		//     but we want the AST to be validated on a regular basis
		//   - meaning not EVERY text update, but always up-to-date if you sit for a second.
		//     but not always firing after you press space-bar or something rapidly
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
			// menu.getItems().add(action("menu.image.center", this::resetPosition));
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
		// ANTLR tokenize
		String code = getText();
		CharStream is = CharStreams.fromString(code);
		BytecodeLexer lexer = new BytecodeLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		// ANTLR parse
		BytecodeParser parser = new BytecodeParser(stream);
		parser.setErrorHandler(new ParserBailStrategy());
		BytecodeParser.UnitContext unitCtx = null;
		try {
			problemTracking.clearOfType(ProblemOrigin.BYTECODE_PARSING);
			unitCtx = parser.unit();
		} catch (ParserException ex) {
			// Parser problems are fatal
			int line = ex.getNode().getStart().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return SaveResult.FAILURE;
		}
		// Transform to our AST
		AntlrToAstTransformer antlrToAstTransformer = new AntlrToAstTransformer();
		Unit unit;
		try {
			problemTracking.clearOfType(ProblemOrigin.BYTECODE_PARSING);
			unit = antlrToAstTransformer.visitUnit(unitCtx);
			this.lastUnit = unit;
		} catch (ParserException ex) {
			// Parser problems are fatal
			int line = ex.getNode().getStart().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return SaveResult.FAILURE;
		}
		// Validate
		AstValidator validator = new AstValidator(unit);
		try {
			problemTracking.clearOfType(ProblemOrigin.BYTECODE_VALIDATION);
			if (config().astValidation)
				validator.visit();
		} catch (AstException ex) {
			// Some validation processes have fatal errors.
			// These are typically rare and are a result of me having a brain fart.
			int line = ex.getSource().getLine();
			String msg = ex.getMessage();
			ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
			problemTracking.addProblem(line, problem);
			return SaveResult.FAILURE;
		}
		// Check for AST validation problems
		if (reportErrors(ProblemOrigin.BYTECODE_VALIDATION, validator)) {
			return SaveResult.FAILURE;
		}
		// Generate
		if (targetMember.isMethod()) {
			return generateMethod(unit);
		} else {
			return generateField(unit);
		}
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target field} if generation succeeded.
	 *
	 * @param unit
	 * 		Unit to pull field data from.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateField(Unit unit) {
		problemTracking.clearOfType(ProblemOrigin.BYTECODE_COMPILE);
		AstToFieldTransformer transformer = new AstToFieldTransformer(unit);
		FieldNode fieldAssembled = transformer.get();
		if (config().bytecodeValidation) {
			BytecodeValidator bytecodeValidator = new BytecodeValidator(classInfo.getName(), fieldAssembled);
			try {
				bytecodeValidator.visit();
			} catch (BytecodeException ex) {
				// Fatal bytecode validation exception
				int line = ex.getLine();
				String msg = ex.getMessage();
				ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
				problemTracking.addProblem(line, problem);
				return SaveResult.FAILURE;
			}
			// Check for bytecode validation problems
			if (reportErrors(ProblemOrigin.BYTECODE_COMPILE, bytecodeValidator)) {
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
	 * @param unit
	 * 		Unit to pull method data from.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateMethod(Unit unit) {
		try {
			problemTracking.clearOfType(ProblemOrigin.BYTECODE_COMPILE);
			AstToMethodTransformer transformer = new AstToMethodTransformer(classInfo.getName(), unit);
			transformer.visit();
			MethodNode methodAssembled = transformer.get();
			if (config().bytecodeValidation) {
				BytecodeValidator bytecodeValidator = new BytecodeValidator(classInfo.getName(), methodAssembled);
				try {
					bytecodeValidator.visit();
				} catch (BytecodeException ex) {
					// Fatal bytecode validation exception
					int line = ex.getLine();
					String msg = ex.getMessage();
					ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
					problemTracking.addProblem(line, problem);
					return SaveResult.FAILURE;
				}
				// Check for bytecode validation problems
				if (reportErrors(ProblemOrigin.BYTECODE_COMPILE, bytecodeValidator)) {
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
			ProblemInfo problem = new ProblemInfo(ProblemOrigin.BYTECODE_COMPILE, ProblemLevel.ERROR, line, msg);
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
		// no-op, represents an actual member so nothing to select
	}

	private static AssemblerConfig config() {
		return Configs.assembler();
	}
}
