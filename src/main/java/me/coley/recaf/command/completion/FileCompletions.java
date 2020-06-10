package me.coley.recaf.command.completion;

import me.coley.recaf.util.RegexUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Picocli completion for files.
 *
 * @author Matt
 */
public class FileCompletions implements Iterable<String> {
	private final Path currentDir;
	private final Predicate<Path> filter;

	/**
	 * Picocli completion for files.
	 */
	public FileCompletions() {
		this(p -> true);
	}

	/**
	 * Picocli completion for files.
	 *
	 * @param filter File inclusion filter.
	 */
	public FileCompletions(Predicate<Path> filter) {
		this(Paths.get(System.getProperty("user.dir")), filter);
	}

	/**
	 * Picocli completion for files.
	 *
	 * @param path Path to use.
	 * @param filter File inclusion filter.
	 */
	public FileCompletions(Path path, Predicate<Path> filter) {
		this.currentDir = path;
		this.filter = filter;
	}

	protected Collection<Path> files() {
		try {
			return Files.list(currentDir).collect(Collectors.toList());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public Iterator<String> iterator() {
		return files().stream().filter(filter).map(Path::getFileName).map(Path::toString).iterator();
	}

	/**
	 * Creates new path inclusion filter
	 * by it's name
	 *
	 * @param pattern path name pattern
	 * @return new filter
	 */
	protected static Predicate<Path> pathNamePattern(String pattern) {
		return f -> RegexUtil.matches(pattern, f.getFileName().toString().toLowerCase());
	}
}
