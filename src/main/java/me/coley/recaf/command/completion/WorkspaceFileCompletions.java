package me.coley.recaf.command.completion;

import java.util.regex.Pattern;

/**
 * Picocli completion for supported workspace files.
 *
 * @author Matt
 */
public class WorkspaceFileCompletions extends FileCompletions {
	private static final Pattern PATTERN = Pattern.compile(".+\\.(class|jar|json)");

	/**
	 * Picocli completion for supported workspace files.
	 */
	public WorkspaceFileCompletions() {
		super(f -> {
			String name = f.getName().toLowerCase();
			return PATTERN.matcher(name).matches();
		});
	}
}
