package me.coley.recaf.parse.assembly;

import me.coley.recaf.parse.assembly.exception.AssemblyParseException;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base for assembling an instruction from a line of text.
 *
 * @author Matt
 */
public abstract class AbstractAssembler {
	/**
	 * Opcode to base assembling off of.
	 */
	protected final int opcode;

	public AbstractAssembler(int opcode) {
		this.opcode = opcode;
	}

	/**
	 * Parse the instruction text.
	 *
	 * @param text
	 * 		Instruction text <i>(Opcode prefix removed)</i>
	 *
	 * @return Instruction instance.
	 */
	public abstract AbstractInsnNode parse(String text);


	/**
	 * @param text
	 * 		Instruction text <i>(Opcode prefix removed)</i>
	 *
	 * @return List of suggestions for the end of the line.
	 */
	public List<String> suggest(String text) {
		// Default implementation has no suggestions.
		return Collections.emptyList();
	}

	/**
	 * Throws an exception with the given failure information.
	 *
	 * @param text
	 * 		Input text that failed to be parsed.
	 *
	 * @return Dummy return to satisfy usage in {@link #parse(String)}.
	 */
	protected AbstractInsnNode fail(String text) {
		return fail(text, null);
	}

	/**
	 * Throws an exception with the given failure information.
	 *
	 * @param text
	 * 		Input text that failed to be parsed.
	 * @param details
	 * 		Additional failure information.
	 *
	 * @return Dummy return to satisfy usage in {@link #parse(String)}.
	 */
	protected AbstractInsnNode fail(String text, String details) {
		String clazz = this.getClass().getSimpleName() + "InsnNode";
		StringBuilder sb = new StringBuilder(clazz + " parse failure: " + text);
		if(details != null)
			sb.append('\n').append(details);
		throw new AssemblyParseException(sb.toString());
	}
}