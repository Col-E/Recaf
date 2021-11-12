package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.Printable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lookup switch instruction.
 *
 * @author Matt Coley
 */
public class LookupSwitchInstruction extends AbstractInstruction {
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
	public LookupSwitchInstruction(String opcode, List<Entry> entries, String defaultIdentifier) {
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
	public String print() {
		String mapping = getEntries().stream()
				.map(Entry::print)
				.collect(Collectors.joining(", "));
		return String.format("%s mapping(%s) default(%s)", getOpcode(), mapping, getDefaultIdentifier());
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
		public String print() {
			return key + "=" + identifier;
		}

		@Override
		public String toString() {
			return print();
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
