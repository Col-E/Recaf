package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.*;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveClassSupplier;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.coley.recaf.assemble.validation.MessageLevel;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.coley.recaf.assemble.validation.bytecode.BytecodeValidator;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import me.darknet.assembler.exceptions.arguments.TooManyArgumentException;
import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.parser.*;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.groups.BodyGroup;
import me.darknet.assembler.parser.groups.declaration.*;
import me.darknet.assembler.parser.groups.instructions.InstructionGroup;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * All in one utility for using the assembler from start to finish.
 *
 * @author Matt Coley
 */
public class AssemblerPipeline {
	private static final DebuggingLogger logger = Logging.get(AssemblerPipeline.class);
	// Input config
	private ClassSupplier classSupplier = ReflectiveClassSupplier.getInstance();
	private InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.getInstance();
	// Listeners
	private final List<ParserFailureListener> parserFailureListeners = new ArrayList<>();
	private final List<AstValidationListener> astValidationListeners = new ArrayList<>();
	private final List<BytecodeFailureListener> bytecodeFailureListeners = new ArrayList<>();
	private final List<BytecodeValidationListener> bytecodeValidationListeners = new ArrayList<>();
	private final List<PipelineCompletionListener> pipelineCompletionListeners = new ArrayList<>();
	private final List<ParserCompletionListener> parserCompletionListeners = new ArrayList<>();
	// Inputs
	private String type;
	private String text;
	private boolean doUseAnalysis = true;
	// States
	private boolean textDirty = true;
	private boolean unitOutdated = true;
	private boolean outputOutdated = true;
	// Outputs
	private Unit unit;
	private List<Group> latestJasmGroups;
	private Variables lastVariables;
	private FieldNode lastField;
	private MethodNode lastMethod;
	private ClassNode lastClass;
	private Analysis lastAnalysis;

	/**
	 * @param listener
	 * 		AST listener receiving failure notifications to add.
	 */
	public void addParserFailureListener(ParserFailureListener listener) {
		if (!parserFailureListeners.contains(listener))
			parserFailureListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		AST listener receiving failure notifications to remove.
	 */
	public void removeParserFailureListener(ParserFailureListener listener) {
		parserFailureListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		AST validation listener to add.
	 */
	public void addAstValidationListener(AstValidationListener listener) {
		if (!astValidationListeners.contains(listener))
			astValidationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		AST validation listener to remove.
	 */
	public void removeAstValidationListener(AstValidationListener listener) {
		astValidationListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Bytecode failure listener to add.
	 */
	public void addBytecodeFailureListener(BytecodeFailureListener listener) {
		if (!bytecodeFailureListeners.contains(listener))
			bytecodeFailureListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Bytecode failure listener to remove.
	 */
	public void removeBytecodeFailureListener(BytecodeFailureListener listener) {
		bytecodeFailureListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Bytecode validation listener to add.
	 */
	public void addBytecodeValidationListener(BytecodeValidationListener listener) {
		if (!bytecodeValidationListeners.contains(listener))
			bytecodeValidationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Bytecode validation listener to remove.
	 */
	public void removeBytecodeValidationListener(BytecodeValidationListener listener) {
		bytecodeValidationListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Pipeline completion listener to add.
	 */
	public void addPipelineCompletionListener(PipelineCompletionListener listener) {
		if (!pipelineCompletionListeners.contains(listener))
			pipelineCompletionListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Pipeline completion listener to remove.
	 */
	public void removePipelineCompletionListener(PipelineCompletionListener listener) {
		pipelineCompletionListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Parser completion listener to add.
	 */
	public void addParserCompletionListener(ParserCompletionListener listener) {
		if (!parserCompletionListeners.contains(listener))
			parserCompletionListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Parser completion listener to remove.
	 */
	public void removeParserCompletionListener(ParserCompletionListener listener) {
		parserCompletionListeners.remove(listener);
	}

	/**
	 * @return Supplies class bytecode to systems.
	 *
	 * @see ReflectiveClassSupplier Default implementation.
	 */
	public ClassSupplier getClassSupplier() {
		return classSupplier;
	}

	/**
	 * @param classSupplier
	 * 		Supplies class bytecode to systems.
	 *
	 * @see ReflectiveClassSupplier Default implementation.
	 */
	public void setClassSupplier(ClassSupplier classSupplier) {
		this.classSupplier = classSupplier;
	}

	/**
	 * @return Supplies inheritance information to system.
	 *
	 * @see ReflectiveInheritanceChecker Default implementation.
	 */
	public InheritanceChecker getInheritanceChecker() {
		return inheritanceChecker;
	}

	/**
	 * @param inheritanceChecker
	 * 		Supplies inheritance information to system.
	 *
	 * @see ReflectiveInheritanceChecker Default implementation.
	 */
	public void setInheritanceChecker(InheritanceChecker inheritanceChecker) {
		this.inheritanceChecker = inheritanceChecker;
	}

	/**
	 * Used to track what type the <i>"this"</i> variable should be,
	 *
	 * @return Internal name of class defining the assembly text's represented member.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * 		Internal name of class defining the assembly text's represented member.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param doUseAnalysis
	 * 		Flag to enable analysis, allowing better variable typing and access to stack-frame info.
	 */
	public void setDoUseAnalysis(boolean doUseAnalysis) {
		this.doUseAnalysis = doUseAnalysis;
	}

	/**
	 * @return {@code true} for when analysis is to be done for method generation,
	 * allowing more accurate error messages, local variables, and stack analysis.
	 */
	public boolean doUseAnalysis() {
		return doUseAnalysis;
	}

	/**
	 * Updates input text. Changes the state so that
	 *
	 * @param newText
	 * 		New assembly text.
	 */
	public void setText(String newText) {
		// Input value must be different than current value.
		if (!Objects.equals(text, newText)) {
			text = newText;
			textDirty = true;
			unitOutdated = true;
			logger.debugging(l -> l.trace("Assembler text updated"));
		}
	}

	/**
	 * @return Last used assembly text.
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return {@code true} if there are changes seen since the last call to {@link #updateAst(boolean)}.
	 */
	public boolean isDirty() {
		return textDirty;
	}

	/**
	 * @return Last parsed unit.
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * @return Last jasm parse groups.
	 */
	public List<Group> getLatestJasmGroups() {
		return latestJasmGroups;
	}

	/**
	 * @return {@code true} when the {@link #getUnit() unit} has not been re-parsed since
	 * the most recent change to the assembly text.
	 * {@code false} when up-to-date.
	 */
	public boolean isUnitOutdated() {
		return unitOutdated;
	}

	/**
	 * @return {@code true} when the {@link #getLastMethod()} has not been updated with {@link #generateMethod()}.
	 * {@code false} when up-to-date.
	 */
	public boolean isOutputOutdated() {
		if (outputOutdated) return true;
		if (isMethod() && lastMethod == null) return true;
		if (isField() && lastField == null) return true;
		return isClass() && lastClass == null;
	}

	/**
	 * @return {@code true} when the {@link #getUnit() unit} represents a class.
	 */
	public boolean isClass() {
		return unit != null && unit.isClass();
	}

	/**
	 * @return {@code true} when the {@link #getUnit() unit} represents a field.
	 */
	public boolean isField() {
		return unit != null && unit.isField();
	}

	/**
	 * @return {@code true} when the {@link #getUnit() unit} represents a method.
	 */
	public boolean isMethod() {
		return unit != null && unit.isMethod();
	}

	/**
	 * @return Last collection of generated variables from {@link #generateMethod()}.
	 */
	public Variables getLastVariables() {
		return lastVariables;
	}

	/**
	 * Protected since this is intended to only be used by child implementations of {@link AssemblerPipeline}.
	 *
	 * @param lastVariables
	 * 		Last collection of generated variables.
	 */
	protected void setLastVariables(Variables lastVariables) {
		this.lastVariables = lastVariables;
	}

	/**
	 * Analysis is only done when {@link }
	 *
	 * @return Last analysis from {@link #generateMethod()}.
	 */
	public Analysis getLastAnalysis() {
		return lastAnalysis;
	}

	/**
	 * Protected since this is intended to only be used by child implementations of {@link AssemblerPipeline}.
	 *
	 * @param lastAnalysis
	 * 		Last analysis.
	 */
	protected void setLastAnalysis(Analysis lastAnalysis) {
		this.lastAnalysis = lastAnalysis;
	}

	/**
	 * @return Last generated class from {@link #generateClass()}.
	 */
	public ClassNode getLastClass() {
		return lastClass;
	}

	/**
	 * @return Last generated field from {@link #generateField()}.
	 */
	public FieldNode getLastField() {
		return lastField;
	}

	/**
	 * @return Last generated method from {@link #generateMethod()}
	 */
	public MethodNode getLastMethod() {
		return lastMethod;
	}

	/**
	 * @param position
	 * 		Position in input text.
	 *
	 * @return {@link Element} of latest AST at the given position.
	 * {@code null} if no AST is available.
	 */
	public Element getCodeElementAt(int position) {
		if (unit == null || !unit.isMethod())
			return null;
		Code code = unit.getDefinitionAsMethod().getCode();
		if (code == null)
			return null;
		return code.getChildAt(position);
	}

	/**
	 * @param lineNo
	 * 		Line number.
	 * @param colPos
	 * 		Column position.
	 *
	 * @return {@link Element} of latest AST at the given line.
	 * {@code null} if no AST is available.
	 */
	public Element getCodeElementAt(int lineNo, int colPos) {
		if (unit == null || !unit.isMethod())
			return null;
		Code code = unit.getDefinitionAsMethod().getCode();
		if (code == null)
			return null;
		return code.getChildAt(lineNo, colPos);
	}

	/**
	 * @param lineNo
	 * 		Line number.
	 *
	 * @return List of {@link Element}s of latest AST on the given line.
	 */
	public List<Element> getCodeElementsAt(int lineNo) {
		if (unit == null || !unit.isMethod())
			return Collections.emptyList();
		Code code = unit.getDefinitionAsMethod().getCode();
		if (code == null)
			return Collections.emptyList();
		return code.getChildrenAt(lineNo);
	}

	/**
	 * Deep traverse the current parser AST and find the {@link Group} at the location
	 *
	 * @param lineNo
	 * 		Line number.
	 * @param colPos
	 * 		Column position.
	 *
	 * @return {@link Group} of latest AST at the given line and column.
	 */
	public Group getASTElementAt(int lineNo, int colPos) {
		for (Group group : latestJasmGroups) {
			Group grp = getASTElementAt(lineNo, colPos, group);
			if (grp != null)
				return grp;
		}
		return null;
	}

	/**
	 * Deep traverse the given {@link Group} and find the {@link Group} at the location
	 *
	 * @param lineNo
	 * 		Line number.
	 * @param colPos
	 * 		Column position.
	 * @param root
	 * 		Root {@link Group} to start the search.
	 *
	 * @return {@link Group} that is either the {@code root} or a child at the given line and column.
	 */
	public Group getASTElementAt(int lineNo, int colPos, Group root) {
		Token token = root.getValue(); // the root token value
		if (token == null) {
			// group may still have children
			for (Group child : root.getChildren()) {
				Group grp = getASTElementAt(lineNo, colPos, child);
				if (grp != null)
					return grp;
			}
			return null;
		}
		Location location = token.getLocation(); // get the actual location
		int line = location.getLine(); // get the line number
		int startCol = location.getColumn(); // get the start column
		int endCol = startCol + token.getContent().length(); // get the end column
		if (line == lineNo && startCol <= colPos && colPos <= endCol) // if the line and column are correct
			return root;
		for (Group child : root.getChildren()) { // check all the children
			Group grp = getASTElementAt(lineNo, colPos, child); // get the child (or null)
			if (grp != null) // if there is a child at the given line and column
				return grp; // return the child
		}
		return null; // else just return null
	}

	/**
	 * Deep traverse the current parser AST and find the {@link Group} at the location
	 *
	 * @param position
	 * 		the position of the cursor
	 *
	 * @return {@link Group} of latest AST at the given line and column.
	 */
	public Group getASTElementAt(int position) {
		for (Group group : latestJasmGroups) {
			Group grp = getASTElementAt(position, group);
			if (grp != null)
				return grp;
		}
		return null;

	}

	/**
	 * Deep traverse the given {@link Group} and find the {@link Group} at the location
	 *
	 * @param position
	 * 		the position of the cursor
	 * @param root
	 * 		Root {@link Group} to start the search.
	 *
	 * @return {@link Group} that is either the {@code root} or a child at the given line and column.
	 */
	public Group getASTElementAt(int position, Group root) {
		Token token = root.getValue(); // the root token value
		if (token == null) {
			// group may still have children
			for (Group child : root.getChildren()) {
				Group grp = getASTElementAt(position, child);
				if (grp != null)
					return grp;
			}
			return null;
		}
		int start = token.getStart();
		int end = token.getEnd();
		if (start <= position && position <= end) // if the position is between the start and end
			return root;
		for (Group child : root.getChildren()) { // check all the children
			Group grp = getASTElementAt(position, child); // get the child (or null)
			if (grp != null) // if there is a child at the given line and column
				return grp; // return the child
		}
		return null; // else just return null
	}

	/**
	 * @return {@code true} if the {@link #getUnit() unit} has been updated.
	 * {@code false} indicates the unit has not changed.
	 */
	public boolean updateAst(boolean usePrefix) {
		// Skip if no changes in the text have been made
		if (!textDirty)
			return false;
		textDirty = false;
		String code = getText();
		if (code == null)
			return false;
		// JASM tokenize
		logger.debugging(l -> l.trace("Assembler AST updating: [JASM tokenize]"));
		Parser parser = new Parser(new Keywords(usePrefix ? "." : null));
		List<Token> tokens = parser.tokenize("<assembler>", code);
		parserCompletionListeners.forEach(l -> l.onCompleteTokenize(tokens));
		// JASM parse
		logger.debugging(l -> l.trace("Assembler AST updating: [JASM parse]"));
		ParserContext ctx = new ParserContext(new LinkedList<>(tokens), parser); // convert to linked list to get a queue
		ctx.setOneLine(true);
		List<Group> parsed;
		try {
			parsed = new ArrayList<>(ctx.parse());
			latestJasmGroups = parsed;
			validateGroups(parsed);
		} catch (AssemblerException ex) {
			// Parser problems are fatal
			parserFailureListeners.forEach(l -> l.onParseFail(ex));
			// this is being set to null to invalidate the unit so that the validator will fail
			// until the unit is valid again
			this.unit = null;
			return true;
		}
		if (Thread.interrupted())
			return false;

		parserCompletionListeners.forEach(l -> l.onCompleteParse(parsed));
		// Transform to our AST
		logger.debugging(l -> l.trace("Assembler AST updating: [JASM --> AST transform]"));
		JasmToUnitTransformer transformer = new JasmToUnitTransformer(parsed);
		Unit unit;
		try {
			unit = transformer.generateUnit();
			// Done creating the AST
			this.unit = unit;
			for (ParserCompletionListener listener : parserCompletionListeners)
				listener.onCompleteTransform(unit);
			unitOutdated = false;
			outputOutdated = true;
		} catch (AssemblerException ex) {
			// Parser problems are fatal
			parserFailureListeners.forEach(l -> l.onParserTransformFail(ex));
			// The unit is reset so that the validator will fail until the unit is valid once again.
			this.unit = null;
			return true;
		}
		logger.debugging(l -> l.trace("Assembler AST up-to-date!"));
		return true;
	}

	private void validateGroups(List<Group> parsed) throws AssemblerException{
		for (Group group : parsed) {
			if(group instanceof MethodDeclarationGroup) {
				MethodDeclarationGroup method = (MethodDeclarationGroup) group;
				validateBody(method.getBody());
			}
			if(group instanceof ClassDeclarationGroup) {
				ClassDeclarationGroup clazz = (ClassDeclarationGroup) group;
				for (Group child : clazz.getChildren()) {
					if(child instanceof MethodDeclarationGroup) {
						MethodDeclarationGroup method = (MethodDeclarationGroup) child;
						validateBody(method.getBody());
					}
				}
			}
		}
	}

	private void validateBody(BodyGroup body) throws AssemblerException {
		for (Group child : body.getChildren()) {
			if(child instanceof InstructionGroup) {
				InstructionGroup instruction = (InstructionGroup) child;
				validateInstruction(instruction);
			}
		}
	}

	private void validateInstruction(InstructionGroup instruction) throws AssemblerException {
		Argument[] missingArgs = instruction.getMissingArguments();
		if(missingArgs.length != 0) {
			throw new MissingArgumentsException(instruction.getEndLocation(), missingArgs);
		}
	}

	/**
	 * Requires a valid {@link #getUnit() unit} to have been generated and an
	 * {@link #addAstValidationListener(AstValidationListener) AST validation listener} to be set.
	 *
	 * @return {@code true} when the {@link #getUnit() last unit} is valid <i>(or null)</i>.
	 * {@code false} means that there were validation errors, which get passed to the
	 * {@link #addParserFailureListener(ParserFailureListener) parser failure listener}
	 */
	public boolean validateAst() {
		// Don't bother if there is no unit or if we're not checking the results
		if (unit == null || astValidationListeners.isEmpty())
			return true;
		astValidationListeners.forEach(l -> l.onAstValidationBegin(unit));
		AstValidator validator = new AstValidator(unit);
		try {
			validator.visit();
		} catch (AstException ex) {
			// Some validation processes have fatal errors.
			// These are typically rare and are a result of me having a brain fart.
			parserFailureListeners.forEach(l -> l.onAstValidationError(ex));
			return false;
		}
		astValidationListeners.forEach(l -> l.onAstValidationComplete(unit, validator));
		// Assert there are no error-level messages
		return validator.getMessages().stream().
				noneMatch(m -> m.getLevel() == MessageLevel.ERROR);
	}

	/**
	 * Requires a valid {@link #getUnit() unit} to have been generated.
	 *
	 * @return {@code true} when the class is generated.
	 * {@code false} when the class is not generated due to the {@link #getUnit() unit}
	 * not being a class, or a validation error occurring.
	 *
	 * @see #getLastClass() Output upon success.
	 */
	public boolean generateClass() {
		if (!outputOutdated && lastClass != null)
			return true;
		// Reset & sanity check
		lastField = null;
		lastMethod = null;
		lastClass = null;
		if (!isClass())
			return false;
		// Build class
		try {
			AstToClassTransformer transformer = new AstToClassTransformer(unit.getDefinitionAsClass());
			ClassNode assembledNode = transformer.buildClass();
			for (FieldNode field : assembledNode.fields) {
				if (!validateNode(field))
					return false;
			}
			for (MethodNode method : assembledNode.methods) {
				if (!validateNode(method))
					return false;
			}
			lastClass = assembledNode;
			outputOutdated = false;
			pipelineCompletionListeners.forEach(l -> l.onCompletedOutput(lastClass));
			return true;
		} catch (MethodCompileException ex) {
			// Compile problems are fatal. These should be rare since most things should be caught in the prior step.
			bytecodeFailureListeners.forEach(l -> l.onCompileFailure(unit, ex));
		}
		return false;
	}

	/**
	 * Requires a valid {@link #getUnit() unit} to have been generated.
	 *
	 * @return {@code true} when the field is generated.
	 * {@code false} when the field is not generated due to the {@link #getUnit() unit}
	 * not being a field, or a validation error occurring.
	 *
	 * @see #getLastField() Output upun success.
	 */
	public boolean generateField() {
		if (!outputOutdated && lastField != null)
			return true;
		// Reset & sanity check
		lastField = null;
		lastMethod = null;
		lastClass = null;
		if (!isField())
			return false;
		// Build field
		AstToFieldTransformer transformer = new AstToFieldTransformer();
		transformer.setDefinition(unit.getDefinitionAsField());
		FieldNode fieldAssembled = transformer.buildField();
		if (!validateNode(fieldAssembled))
			return false;
		// Done
		lastField = fieldAssembled;
		outputOutdated = false;
		pipelineCompletionListeners.forEach(l -> l.onCompletedOutput(lastField));
		return true;
	}

	/**
	 * Requires a valid {@link #getUnit() unit} to have been generated.
	 *
	 * @return {@code true} when the method is generated.
	 * {@code false} when the method is not generated due to the {@link #getUnit() unit}
	 * not being a method, or a validation error occurring.
	 *
	 * @see #getLastMethod() Output upon success.
	 */
	public boolean generateMethod() {
		if (!outputOutdated && lastMethod != null)
			return true;
		// Reset & sanity check
		lastField = null;
		lastMethod = null;
		lastClass = null;
		if (!isMethod())
			return false;
		// Build method
		try {
			AstToMethodTransformer transformer = new AstToMethodTransformer(classSupplier, type);
			transformer.setUseAnalysis(doUseAnalysis);
			transformer.setInheritanceChecker(inheritanceChecker);
			transformer.setDefinition(unit.getDefinitionAsMethod());
			transformer.visit();
			MethodNode methodAssembled = transformer.buildMethod();
			if (!validateNode(methodAssembled))
				return false;
			// Done
			lastMethod = methodAssembled;
			lastAnalysis = transformer.getAnalysis();
			lastVariables = transformer.getVariables();
			outputOutdated = false;
			pipelineCompletionListeners.forEach(l -> l.onCompletedOutput(lastMethod));
			return true;
		} catch (MethodCompileException ex) {
			// Compile problems are fatal. These should be rare since most things should be caught in the prior step.
			bytecodeFailureListeners.forEach(l -> l.onCompileFailure(unit, ex));
		}
		return false;
	}

	/**
	 * @param field
	 * 		Field node to validate.
	 *
	 * @return {@code true} for a valid field.
	 */
	public boolean validateNode(FieldNode field) {
		if (bytecodeValidationListeners.size() > 0) {
			BytecodeValidator bytecodeValidator = new BytecodeValidator(type, field);
			try {
				bytecodeValidator.visit();
			} catch (BytecodeException ex) {
				// Fatal bytecode validation exception
				bytecodeFailureListeners.forEach(l -> l.onValidationFailure(field, ex));
				return false;
			}
			// Check for bytecode validation problems
			bytecodeValidationListeners.forEach(l -> l.onBytecodeValidationComplete(field, bytecodeValidator));
		}
		// Done
		return true;
	}

	/**
	 * @param method
	 * 		Method node to validate.
	 *
	 * @return {@code true} for a valid method.
	 */
	public boolean validateNode(MethodNode method) {
		if (bytecodeValidationListeners.size() > 0) {
			BytecodeValidator bytecodeValidator = new BytecodeValidator(type, method);
			try {
				bytecodeValidator.visit();
			} catch (BytecodeException ex) {
				// Fatal bytecode validation exception
				bytecodeFailureListeners.forEach(l -> l.onValidationFailure(method, ex));
				return false;
			}
			// Check for bytecode validation problems
			bytecodeValidationListeners.forEach(l -> l.onBytecodeValidationComplete(method, bytecodeValidator));
		}
		// Done
		return true;
	}
}
