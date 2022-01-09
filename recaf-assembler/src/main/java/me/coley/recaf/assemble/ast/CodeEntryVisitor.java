package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.ast.meta.Label;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Visits code entries in logical order, rather than linear order.
 *
 * @author Matt Coley
 */
public class CodeEntryVisitor {
	private final Set<CodeEntry> visited = new HashSet<>();
	private final Consumer<CodeEntry> consumer;
	private int max;

	/**
	 * @param consumer
	 * 		Delegate to pass visited entries to.
	 */
	public CodeEntryVisitor(Consumer<CodeEntry> consumer) {
		this.consumer = consumer;
	}

	/**
	 * @param code
	 * 		Code to visit.
	 *
	 * @throws AstException
	 * 		Thrown when visit ordering failed due to improper AST.
	 */
	public void visit(Code code) throws AstException {
		max = code.getEntries().size();
		visit(code, 0);
	}

	private void visit(Code code, int position) throws AstException {
		// Linearly continue while in bounds
		while (position >= 0 && position < max) {
			// Skip if already visited
			CodeEntry entry = code.getEntries().get(position);
			if (!visited.contains(entry)) {
				visited.add(entry);
				// Pass to consumer
				consumer.accept(entry);
				// Visit branch
				if (entry instanceof FlowControl) {
					for (Label branch : ((FlowControl) entry).getTargets(code.getLabels())) {
						int branchIndex = code.getEntries().indexOf(branch);
						visit(code, branchIndex);
					}
				}
			}
			// Increment position
			position++;
		}
	}
}
