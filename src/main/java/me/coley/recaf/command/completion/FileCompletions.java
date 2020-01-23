package me.coley.recaf.command.completion;

import me.coley.recaf.util.RegexUtil;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

/**
 * Picocli completion for files.
 *
 * @author Matt
 */
public class FileCompletions implements Iterable<String> {
	private final File currentDir = new File(System.getProperty("user.dir"));
	private final Predicate<File> filter;

	/**
	 * Picocli completion for files.
	 */
	public FileCompletions() {
		this(f -> true);
	}

	/**
	 * Picocli completion for files.
	 *
	 * @param filter File inclusion filter.
	 */
	public FileCompletions(Predicate<File> filter) {
		this.filter = filter;
	}

	protected Collection<File> files() {
		return Arrays.asList(Objects.requireNonNull(currentDir.listFiles()));
	}

	@Override
	public Iterator<String> iterator() {
		return files().stream().filter(filter).map(File::getName).iterator();
	}

	/**
	 * Creates new file inclusion filter
	 * by it's name
	 *
	 * @param pattern file name pattern
	 * @return new filter
	 */
	protected static Predicate<File> fileNamePattern(String pattern) {
		return f -> RegexUtil.matches(pattern, f.getName().toLowerCase());
	}
}
