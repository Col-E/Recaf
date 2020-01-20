package me.coley.recaf.command.completion;

import java.util.regex.Pattern;

/**
 * Picocli completion for archives.
 *
 * @author Matt
 */
public class ArchiveFileCompletions extends FileCompletions {
	private static final Pattern PATTERN = Pattern.compile(".+\\.(zip|jar)");

	/**
	 * Picocli completion for archives.
	 */
	public ArchiveFileCompletions() {
		super(f -> {
			String name = f.getName().toLowerCase();
			return PATTERN.matcher(name).matches();
		});
	}
}
