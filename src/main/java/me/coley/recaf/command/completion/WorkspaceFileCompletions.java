package me.coley.recaf.command.completion;

/**
 * Picocli completion for supported workspace files.
 *
 * @author Matt
 */
public class WorkspaceFileCompletions extends FileCompletions {
	/**
	 * Picocli completion for supported workspace files.
	 */
	public WorkspaceFileCompletions() {
		super(fileNamePattern(".+\\.(class|jar|json)"));
	}
}
