package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.assemble.ast.arch.ConstVal;
import me.coley.recaf.assemble.ast.arch.ThrownException;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.meta.Comment;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The portion of the {@link me.coley.recaf.assemble.ast.arch.MethodDefinition} containing:
 * <ul>
 *     <li>code</li>
 *     <li>variable/exception/try-catch metadata</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class Code extends BaseElement {
	private final Map<String, Label> labels = new LinkedHashMap<>();
	private final List<CodeEntry> entries = new ArrayList<>();
	private final List<AbstractInstruction> instructions = new ArrayList<>();
	private final List<Comment> comments = new ArrayList<>();
	private final List<TryCatch> tryCatches = new ArrayList<>();
	private final List<ThrownException> thrownExceptions = new ArrayList<>();
	private final List<Annotation> annotations = new ArrayList<>();
	private final List<Expression> expressions = new ArrayList<>();
	private final List<Unmatched> unmatched = new ArrayList<>();
	private ConstVal constVal;
	private Signature signature;

	/**
	 * @param entry
	 * 		Entry to add.
	 */
	public void add(CodeEntry entry) {
		entry.insertInto(this);
	}

	/**
	 * @param instruction
	 * 		Instruction to add.
	 */
	public void addInstruction(AbstractInstruction instruction) {
		instructions.add(instruction);
		addInternal(instruction);
	}

	/**
	 * @param label
	 * 		Label to add.
	 */
	public void addLabel(Label label) {
		String name = label.getName();
		labels.put(name, label);
		addInstruction(label);
	}

	/**
	 * @param comment
	 * 		Comment to add.
	 */
	public void addComment(Comment comment) {
		comments.add(comment);
		addInternal(comment);
	}

	/**
	 * @param tryCatch
	 * 		Try catch label range to add.
	 */
	public void addTryCatch(TryCatch tryCatch) {
		tryCatches.add(tryCatch);
		addInternal(tryCatch);
	}

	/**
	 * @param thrownException
	 * 		Thrown exception of a single type.
	 */
	public void addThrownException(ThrownException thrownException) {
		thrownExceptions.add(thrownException);
		addInternal(thrownException);
	}

	/**
	 * @param annotation
	 * 		Annotation to add.
	 */
	public void addAnnotation(Annotation annotation) {
		annotations.add(annotation);
		addInternal(annotation);
	}

	/**
	 * @param expression
	 * 		Expression to add.
	 */
	public void addExpression(Expression expression) {
		expressions.add(expression);
		addInstruction(expression);
	}

	/**
	 * @param unmatchedText
	 * 		Unmatched text.
	 */
	public void addUnmatched(Unmatched unmatchedText) {
		unmatched.add(unmatchedText);
		addInternal(unmatchedText);
	}

	/**
	 * @param constVal
	 * 		New constant value.
	 */
	public void setConstVal(ConstVal constVal) {
		this.constVal = constVal;
		addInternal(constVal);
	}

	/**
	 * @param signature
	 * 		New generic signature.
	 */
	public void setSignature(Signature signature) {
		this.signature = signature;
		addInternal(signature);
	}


	/**
	 * Called by any of the public facing methods for adding entries.
	 *
	 * @param entry
	 * 		Entry to add.
	 */
	private void addInternal(CodeEntry entry) {
		entries.add(child(entry));
	}

	/**
	 * @param name
	 * 		Label identifier.
	 *
	 * @return Label instance.
	 */
	public Label getLabel(String name) {
		return labels.get(name);
	}

	/**
	 * @param entry
	 * 		Some code entry item.
	 *
	 * @return The closest label to the entry going backwards.
	 */
	public Label getPrevLabel(CodeEntry entry) {
		if (entry instanceof Label)
			return (Label) entry;
		// Get index in "all" entries
		int index = entries.indexOf(entry);
		if (index < 0)
			return null;
		// Find last label that is before the given index
		Label lastLabel = null;
		for (Label label : labels.values()) {
			int labelIndex = entries.indexOf(label);
			if (labelIndex > index)
				break;
			lastLabel = label;
		}
		return lastLabel;
	}

	/**
	 * @param entry
	 * 		Some code entry item.
	 *
	 * @return The closest label to the entry going forwards.
	 */
	public Label getNextLabel(CodeEntry entry) {
		if (entry instanceof Label)
			return (Label) entry;
		// Get index in "all" entries
		int index = entries.indexOf(entry);
		if (index < 0)
			return null;
		// Find last label that is before the given index
		for (Label label : labels.values()) {
			int labelIndex = entries.indexOf(label);
			if (labelIndex > index)
				return label;
		}
		return null;
	}

	/**
	 * @return First label.
	 */
	public Label getFirstLabel() {
		// 'LinkedHashMap' keeps insertion order so the first item should be the first label.
		return labels.values().iterator().next();
	}

	/**
	 * @return Last label.
	 */
	public Label getLastLabel() {
		//  'LinkedHashMap' keeps insertion order so the last item should be the last label.
		int last = labels.size() - 1;
		return (Label) labels.values().toArray()[last];
	}

	/**
	 * @return All code entries <i>(Contains instructions/labels/etc).</i>
	 */
	public List<CodeEntry> getEntries() {
		return entries;
	}

	/**
	 * @return All instructions of the code body.
	 */
	public List<AbstractInstruction> getInstructions() {
		return instructions;
	}

	/**
	 * @return All comments of the code body.
	 */
	public List<Comment> getComments() {
		return comments;
	}

	/**
	 * @return All thrown exceptions of the method body.
	 */
	public List<ThrownException> getThrownExceptions() {
		return thrownExceptions;
	}

	/**
	 * @return All try-catch ranges of the method body.
	 */
	public List<TryCatch> getTryCatches() {
		return tryCatches;
	}

	/**
	 * @return All annotations.
	 */
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @return All in-line expressions.
	 */
	public List<Expression> getExpressions() {
		return expressions;
	}

	/**
	 * @return All unmatched raw items.
	 */
	public List<Unmatched> getUnmatched() {
		return unmatched;
	}

	/**
	 * @return The constant value of the field. May be {@code null}.
	 */
	public ConstVal getConstVal() {
		return constVal;
	}

	/**
	 * @return Generic signature.
	 */
	public Signature getSignature() {
		return signature;
	}

	/**
	 * @return All named labels of the code body.
	 */
	public Map<String, Label> getLabels() {
		return labels;
	}

	@Override
	public String print() {
		return entries.stream()
				.map(Element::print)
				.collect(Collectors.joining("\n"));
	}
}
