package software.coley.recaf.services.assembler;

/**
 * Assembler dialect a {@link Snippet} is written for.
 *
 * @author Matt Coley
 */
public enum SnippetTarget {
	/** JVM bytecode assembly. */
	JVM,
	/** Dalvik bytecode assembly. */
	DALVIK
}
