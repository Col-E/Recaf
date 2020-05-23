package me.coley.recaf.command.completion;

/**
 * Picocli completion for archives.
 *
 * @author Matt
 */
public class ArchiveFileCompletions extends FileCompletions {

	/**
	 * Picocli completion for archives.
	 */
	public ArchiveFileCompletions() {
		super(pathNamePattern(".+\\.(zip|jar)"));
	}
}
