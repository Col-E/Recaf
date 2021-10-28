package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.meta.Comment;
import me.coley.recaf.assemble.ast.meta.Label;

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
	private final List<AbstractInstruction> instructions = new ArrayList<>();
	private final List<Comment> comments = new ArrayList<>();
	private final List<CodeEntry> entries = new ArrayList<>();

	// TODO: Try-catch block support

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
		addInternal(label);
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
	 * Called by any of the public facing methods for adding entries.
	 *
	 * @param entry
	 * 		Entry to add.
	 */
	private void addInternal(CodeEntry entry) {
		entries.add(entry);
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
