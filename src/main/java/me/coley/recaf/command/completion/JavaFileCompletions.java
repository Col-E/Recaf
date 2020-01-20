package me.coley.recaf.command.completion;

import java.util.regex.Pattern;

/**
 * Picocli completion for java program files.
 *
 * @author Matt
 */
public class JavaFileCompletions extends FileCompletions {
	private static final Pattern PATTERN = Pattern.compile(".+\\.(class|jar)");

	/**
	 * Picocli completion for java program files.
	 */
	public JavaFileCompletions() {
		super(f -> {
			String name = f.getName().toLowerCase();
			return PATTERN.matcher(name).matches();
		});
	}
}
