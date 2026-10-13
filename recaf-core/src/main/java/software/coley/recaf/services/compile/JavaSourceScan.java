package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java source scanning utilities for phantom refinement.
 *
 * @author jus7n
 * @see JavacDiagnosticHints
 */
final class JavaSourceScan {
	private static final Pattern PACKAGE_DECLARATION = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
	private static final Pattern STAR_IMPORT = Pattern.compile("(?m)^\\s*import\\s+(?!static\\b)([\\w.]+)\\.\\*\\s*;");

	private JavaSourceScan() {}

	/**
	 * @param source
	 * 		Source of a compilation unit.
	 *
	 * @return Packages named by non-static on-demand imports in the source, in import order, dotted.
	 */
	@Nonnull
	public static List<String> starImportPackages(@Nonnull String source) {
		List<String> packages = new ArrayList<>();
		Matcher star = STAR_IMPORT.matcher(source);
		while (star.find())
			packages.add(star.group(1));
		return packages;
	}

	/**
	 * @param sources
	 * 		Sources of compilation units.
	 *
	 * @return Packages named by non-static on-demand imports in any of the sources, dotted.
	 */
	@Nonnull
	public static Set<String> starImportPackages(@Nonnull Collection<String> sources) {
		Set<String> packages = new TreeSet<>();
		for (String source : sources)
			packages.addAll(starImportPackages(source));
		return packages;
	}

	/**
	 * @param source
	 * 		Source of a compilation unit.
	 * @param packages
	 * 		Dotted packages whose on-demand imports to remove.
	 *
	 * @return The source with its on-demand imports of the given packages removed.
	 */
	@Nonnull
	public static String withoutStarImports(@Nonnull String source, @Nonnull Set<String> packages) {
		Matcher star = STAR_IMPORT.matcher(source);
		StringBuilder out = new StringBuilder();
		while (star.find())
			star.appendReplacement(out, packages.contains(star.group(1)) ? "" : Matcher.quoteReplacement(star.group()));
		star.appendTail(out);
		return out.toString();
	}

	/**
	 * @param source
	 * 		Source of a compilation unit.
	 *
	 * @return The internal package prefix of the source, including the trailing slash, or an empty string for the
	 * default package.
	 */
	@Nonnull
	public static String packagePrefix(@Nonnull String source) {
		Matcher declaration = PACKAGE_DECLARATION.matcher(source);
		return declaration.find() ? declaration.group(1).replace('.', '/') + "/" : "";
	}

	/**
	 * @param source
	 * 		Source of a compilation unit.
	 *
	 * @return Source split into lines, preserving a trailing empty line.
	 */
	@Nonnull
	public static String[] lines(@Nonnull String source) {
		return source.split("\\n", -1);
	}

	/**
	 * @param lines
	 * 		Lines of one compilation unit, as returned by {@link #lines(String)}.
	 * @param line
	 * 		One-based line number.
	 *
	 * @return The text of the line followed by a line break, or an empty string when the line is out of range.
	 */
	@Nonnull
	public static String lineAt(@Nonnull String[] lines, int line) {
		return line >= 1 && line <= lines.length ? lines[line - 1] + '\n' : "";
	}

	/**
	 * @param linesBySource
	 * 		Cached lines keyed by internal compilation-unit name.
	 * @param sourceName
	 * 		Internal name of the diagnostic's compilation unit, or {@code null} when unknown.
	 * @param line
	 * 		One-based line number.
	 *
	 * @return The text of the line in the named source followed by a line break,
	 * or an empty string when the source is unknown or the line is out of range.
	 */
	@Nonnull
	public static String lineAt(@Nonnull Map<String, String[]> linesBySource, @Nullable String sourceName, int line) {
		if (sourceName == null)
			return "";
		String[] lines = linesBySource.get(sourceName);
		return lines == null ? "" : lineAt(lines, line);
	}
}
