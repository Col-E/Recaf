package me.coley.recaf.command.completion;

/**
 * Picocli completion for java program files.
 *
 * @author Matt
 */
public class JavaFileCompletions extends FileCompletions {

	/**
	 * Picocli completion for java program files.
	 */
	public JavaFileCompletions() {
		super(pathNamePattern(".+\\.(class|jar)"));
	}
}
