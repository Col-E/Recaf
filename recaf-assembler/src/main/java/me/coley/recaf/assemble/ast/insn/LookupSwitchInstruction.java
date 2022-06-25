package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.assemble.ast.meta.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lookup switch instruction.
 *
 * @author Matt Coley
 */
public class LookupSwitchInstruction extends AbstractInstruction implements FlowControl {
	private final List<Entry> entries;
	private final String defaultIdentifier;

	/**
	 * @param opcode
	 * 		Lookup switch opcode.
	 * @param entries
	 * 		Switch entries.
	 * @param defaultIdentifier
	 * 		Default label identifier.
	 */
	public LookupSwitchInstruction(int opcode, List<Entry> entries, String defaultIdentifier) {
		super(opcode);
		this.entries = entries;
		this.defaultIdentifier = defaultIdentifier;
	}

	/**
	 * @return Switch entries.
	 */
	public List<Entry> getEntries() {
		return entries;
	}

	/**
	 * @return Default label identifier.
	 */
	public String getDefaultIdentifier() {
		return defaultIdentifier;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.LOOKUP;
	}

	@Override
	public List<Label> getTargets(Map<String, Label> labelMap) throws IllegalAstException {
		List<Label> labels = new ArrayList<>();
		for (Entry entry : getEntries()) {
			Label label = labelMap.get(entry.getName());
			if (label == null)
				throw new IllegalAstException(this, "Could not find instance for label: " + entry.getName());
			labels.add(label);
		}
		Label label = labelMap.get(defaultIdentifier);
		if (label == null)
			throw new IllegalAstException(this, "Could not find instance for label: " + defaultIdentifier);
		labels.add(label);
		return labels;
	}

	@Override
	public String print(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(getOpcode()).append("\n");
		for (Entry entry : getEntries()) {
			sb
					.append("\t\t")
					.append(context.fmtKeyword("case "))
					.append(entry.key)
					.append(" ")
					.append(entry.identifier)
					.append("\n");
		}
		sb.append("\t\t").append(context.fmtKeyword("default ")).append(getDefaultIdentifier());
		return sb.toString();
	}

	@Override
	public boolean isForced() {
		// A switch must go to one of the flow targets
		return true;
	}

	/**
	 * Lookup entry.
	 */
	public static class Entry implements Printable {
		private final int key;
		private final String identifier;

		/**
		 * @param key
		 * 		Lookup value.
		 * @param identifier
		 * 		Label identifier associated with key.
		 */
		public Entry(int key, String identifier) {
			this.key = key;
			this.identifier = identifier;
		}

		/**
		 * @return Lookup value.
		 */
		public int getKey() {
			return key;
		}

		/**
		 * @return Label identifier associated with key.
		 */
		public String getName() {
			return identifier;
		}

		@Override
		public String print(PrintContext context) {
			return context.fmtKeyword("case ") + key + " " + identifier;
		}

		@Override
		public String toString() {
			return print(PrintContext.DEFAULT_CTX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Entry entry = (Entry) o;
			return key == entry.key && Objects.equals(identifier, entry.identifier);
		}

		@Override
		public int hashCode() {
			return Objects.hash(key, identifier);
		}
	}
}
