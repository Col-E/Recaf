package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.AbstractDefinition;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.pipeline.ParserCompletionListener;
import me.coley.recaf.assemble.transformer.AstToMethodTransformer;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An extension of the {@link AssemblerPipeline} to support context-sensitive behaviors when working with
 * {@link me.coley.recaf.assemble.ast.arch.ClassDefinition} instances. For example, in order to show the variables
 * of the method a user is currently engaging with inside the class, we need to somehow track what is the currently
 * selected method, then analyze it for that information. We will track the current selection in
 * {@link ContextualUnit} and update the pipelines variables/analysis information when the selected member changes.
 *
 * @author Matt Coley
 * @see ContextualUnit Current selection tracking within the current unit.
 */
public class ContextualPipeline extends AssemblerPipeline implements ParserCompletionListener {
	private static final DebuggingLogger logger = Logging.get(ContextualPipeline.class);
	private final List<CurrentDefinitionListener> currentDefinitionListeners = new ArrayList<>();
	private ContextualUnit contextualUnit;

	public ContextualPipeline() {
		addParserCompletionListener(this);
	}

	/**
	 * @return Context sensitive unit.
	 */
	public ContextualUnit getContextualUnit() {
		return contextualUnit;
	}

	/**
	 * @param listener
	 * 		Definition selection update listener.
	 */
	public void addCurrentDefinitionListener(CurrentDefinitionListener listener) {
		if (!currentDefinitionListeners.contains(listener))
			currentDefinitionListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Definition selection update listener.
	 */
	public void removeCurrentDefinitionListener(CurrentDefinitionListener listener) {
		currentDefinitionListeners.remove(listener);
	}

	/**
	 * Called when the user updates their caret position. When the position resides within a field/method
	 * then we will track that that is the {@link ContextualUnit#getCurrentDefinition() currently active} definition.
	 *
	 * @param currentDefinition
	 * 		Current field or method definition residing within the class.
	 */
	public void setCurrentDefinition(AbstractDefinition currentDefinition) {
		if (contextualUnit != null) {
			if(Objects.equals(contextualUnit.getCurrentDefinition(), currentDefinition))
				return;
			contextualUnit.setCurrentDefinition(currentDefinition);
			if (currentDefinition.isMethod()) {
				try {
					AstToMethodTransformer transformer = new AstToMethodTransformer(getClassSupplier(), getType());
					transformer.setUseAnalysis(doUseAnalysis());
					transformer.setInheritanceChecker(getInheritanceChecker());
					transformer.setDefinition((MethodDefinition) currentDefinition);
					transformer.visit();
					setLastAnalysis(transformer.getAnalysis());
					setLastVariables(transformer.getVariables());
				} catch (MethodCompileException e) {
					logger.error("Failed to analyze current selected method: " + currentDefinition.getName());
					setLastAnalysis(null);
					setLastVariables(null);
				}
			} else {
				setLastAnalysis(null);
				setLastVariables(null);
			}
			currentDefinitionListeners.forEach(l -> l.onCurrentDefinitionUpdate(contextualUnit, currentDefinition));
		}
	}

	/**
	 * @return Delegates to {@link ContextualUnit#getCurrentDefinition()}
	 */
	public AbstractDefinition getCurrentDefinition() {
		if (contextualUnit == null) return null;
		return contextualUnit.getCurrentDefinition();
	}

	/**
	 * @return Delegates to {@link ContextualUnit#isCurrentMethod()}
	 */
	public boolean isCurrentMethod() {
		if (contextualUnit == null) return false;
		return contextualUnit.isCurrentMethod();
	}

	/**
	 * @return Delegates to {@link ContextualUnit#isCurrentField()}
	 */
	public boolean isCurrentField() {
		if (contextualUnit == null) return false;
		return contextualUnit.isCurrentField();
	}

	/**
	 * @return Delegates to {@link ContextualUnit#isCurrentClass()}
	 */
	public boolean isCurrentClass() {
		if (contextualUnit == null) return false;
		return contextualUnit.isCurrentClass();
	}

	/**
	 * @return Delegates to {@link ContextualUnit#getCurrentMethod()}
	 */
	public MethodDefinition getCurrentMethod() {
		return contextualUnit.getCurrentMethod();
	}

	/**
	 * @return Delegates to {@link ContextualUnit#getCurrentField()}
	 */
	public FieldDefinition getCurrentField() {
		return contextualUnit.getCurrentField();
	}

	@Override
	public Element getCodeElementAt(int position) {
		if (contextualUnit == null || !contextualUnit.isCurrentMethod())
			return null;
		Code code = contextualUnit.getCurrentMethod().getCode();
		if (code == null)
			return null;
		return code.getChildAt(position);
	}

	@Override
	public Element getCodeElementAt(int lineNo, int colPos) {
		if (contextualUnit == null || !contextualUnit.isCurrentMethod())
			return null;
		Code code = contextualUnit.getCurrentMethod().getCode();
		if (code == null)
			return null;
		return code.getChildAt(lineNo, colPos);
	}

	@Override
	public List<Element> getCodeElementsAt(int lineNo) {
		if (contextualUnit == null || !contextualUnit.isCurrentMethod())
			return Collections.emptyList();
		Code code = contextualUnit.getCurrentMethod().getCode();
		if (code == null)
			return Collections.emptyList();
		return code.getChildrenAt(lineNo);
	}

	@Override
	public void onCompleteTokenize(List<Token> tokens) {
		// no-op
	}

	@Override
	public void onCompleteParse(List<Group> groups) {
		// no-op
	}

	@Override
	public void onCompleteTransform(Unit unit) {
		contextualUnit = new ContextualUnit(unit);
	}
}
