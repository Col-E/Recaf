package software.coley.recaf.util.assembler.resolve;

/**
 * Common resolution type of some <i>"selected"</i> text in JASM assembler AST.
 * See implementations for possible resolved contents.
 *
 * @author Matt Coley
 */
public interface AssemblyResolution {
	/**
	 * Shared empty resolution instance.
	 */
	EmptyResolution EMPTY = new EmptyResolution();
}
