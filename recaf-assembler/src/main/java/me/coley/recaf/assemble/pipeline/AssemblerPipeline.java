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
import me.coley.recaf.util.logging.Logging;
import me.darknet.assembler.parser.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * All in one utility for using the assembler from start to finish.
 *
 * @author Matt Coley
 */
public class AssemblerPipeline {
	private static final Logger logger = Logging.get(AssemblerPipeline.class);
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
	// States
	private boolean textDirty = true;
	private boolean unitOutdated = true;
	private boolean outputOutdated = true;
	private boolean doUseAnalysis = true;
	// Outputs
	private Unit unit;
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
			logger.trace("Assembler text updated");
		}
	}

	/**
	 * @return Last used assembly text.
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return {@code true} if there are changes seen since the last call to {@link #updateAst()}.
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
		return outputOutdated || (isMethod() ? lastMethod != null : lastField != null);
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
	 * Analysis is only done when {@link }
	 *
	 * @return Last analysis from {@link #generateMethod()}.
	 */
	public Analysis getLastAnalysis() {
		return lastAnalysis;
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
	 * @param lineNo
	 * 		Line number.
	 *
	 * @return {@link Element} of latest AST at the given line.
	 * {@code null} if no AST is available.
	 */
	public Element getElementOnLine(int lineNo) {
		// TODO: Use unit.getChildOnLine(lineNo)?
		//  - may need to tweak how it works so that we get relevant data types
		//    if the child-most types are too generic.
		if (unit == null || !unit.isMethod())
			return null;
		Code code = unit.getMethod().getCode();
		if (code == null)
			return null;
		return code.getChildOnLine(lineNo);
	}

	/**
	 * @return {@code true} if the {@link #getUnit() unit} has been updated.
	 * {@code false} indicates the unit has not changed.
	 */
	public boolean updateAst() {
		// Skip if no changes in the text have been made
		if (!textDirty)
			return false;
		textDirty = false;
		String code = getText();
		if (code == null)
			return false;
		// JASM tokenize
		logger.trace("Assembler AST updating: [JASM tokenize]");
		Parser parser = new Parser();
		List<Token> tokens = parser.tokenize("<assembler>", code);
		parserCompletionListeners.forEach(l -> l.onCompleteTokenize(tokens));
		// JASM parse
		logger.trace("Assembler AST updating: [JASM parse]");
		ParserContext ctx = new ParserContext(new LinkedList<>(tokens), parser); // convert to linked list to get a queue
		List<Group> parsed;
		try {
			parsed = new ArrayList<>(ctx.parse());
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
		logger.trace("Assembler AST updating: [JASM --> AST transform]");
		JasmToAstTransformer transformer = new JasmToAstTransformer(parsed);
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
		logger.trace("Assembler AST up-to-date!");
		return true;
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
			AstToClassTransformer transformer = new AstToClassTransformer(unit.getClassDefinition());
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
		transformer.setDefinition(unit.getField());
		FieldNode fieldAssembled = transformer.buildField();
		if (bytecodeValidationListeners.size() > 0) {
			BytecodeValidator bytecodeValidator = new BytecodeValidator(type, fieldAssembled);
			try {
				bytecodeValidator.visit();
			} catch (BytecodeException ex) {
				// Fatal bytecode validation exception
				bytecodeFailureListeners.forEach(l -> l.onValidationFailure(fieldAssembled, ex));
				return false;
			}
			// Check for bytecode validation problems
			bytecodeValidationListeners.forEach(l -> l.onBytecodeValidationComplete(fieldAssembled, bytecodeValidator));
		}
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
			transformer.setDefinition(unit.getMethod());
			transformer.visit();
			MethodNode methodAssembled = transformer.buildMethod();
			if (bytecodeValidationListeners.size() > 0) {
				BytecodeValidator bytecodeValidator = new BytecodeValidator(type, methodAssembled);
				try {
					bytecodeValidator.visit();
				} catch (BytecodeException ex) {
					// Fatal bytecode validation exception
					bytecodeFailureListeners.forEach(l -> l.onValidationFailure(methodAssembled, ex));
					return false;
				}
				// Check for bytecode validation problems
				bytecodeValidationListeners.forEach(l -> l.onBytecodeValidationComplete(methodAssembled, bytecodeValidator));
			}
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
