package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import regexodus.Matcher;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities for augmenting Java source code to facilitate script compilation.
 *
 * @author Matt Coley
 */
public final class ScriptSourceAugmentation {
	private static final String PATTERN_PACKAGE = "package ([\\w\\.\\*]+);?";
	private static final String PATTERN_IMPORT = "import ([\\w\\.\\*]+);?";
	private static final String PATTERN_CLASS_NAME = "(?<=class)\\s+(\\w+)\\s+(?:implements|extends|\\{)";
	private static final String SCRIPT_PACKAGE_NAME = "software.coley.recaf.generated";
	private static final List<String> DEFAULT_IMPORTS = Arrays.asList(
			"java.io.*",
			"java.nio.file.*",
			"java.util.*",
			"software.coley.recaf.*",
			"software.coley.recaf.analytics.logging.*",
			"software.coley.recaf.info.*",
			"software.coley.recaf.info.annotation.*",
			"software.coley.recaf.info.builder.*",
			"software.coley.recaf.info.member.*",
			"software.coley.recaf.info.properties.*",
			"software.coley.recaf.services.*",
			"software.coley.recaf.services.analysis.android.*",
			"software.coley.recaf.services.analysis.antitamper.*",
			"software.coley.recaf.services.analysis.entry.*",
			"software.coley.recaf.services.analysis.metadata.*",
			"software.coley.recaf.services.analysis.structure.*",
			"software.coley.recaf.services.assembler.*",
			"software.coley.recaf.services.attach.*",
			"software.coley.recaf.services.callgraph.*",
			"software.coley.recaf.services.comment.*",
			"software.coley.recaf.services.compile.*",
			"software.coley.recaf.services.config.*",
			"software.coley.recaf.services.decompile.*",
			"software.coley.recaf.services.file.*",
			"software.coley.recaf.services.inheritance.*",
			"software.coley.recaf.services.mapping.*",
			"software.coley.recaf.services.phantom.*",
			"software.coley.recaf.services.plugin.*",
			"software.coley.recaf.services.script.*",
			"software.coley.recaf.services.search.*",
			"software.coley.recaf.services.source.*",
			"software.coley.recaf.services.transform.*",
			"software.coley.recaf.services.workspace.*",
			"software.coley.recaf.services.workspace.io.*",
			"software.coley.recaf.workspace.model.*",
			"software.coley.recaf.workspace.model.bundle.*",
			"software.coley.recaf.util.*",
			"software.coley.recaf.util.android.*",
			"software.coley.recaf.util.collect.*",
			"software.coley.recaf.util.io.*",
			"software.coley.recaf.util.kotlin.*",
			"software.coley.recaf.util.kotlin.model.*",
			"software.coley.recaf.util.threading.*",
			"software.coley.recaf.util.visitors.*",
			"org.objectweb.asm.*",
			"org.objectweb.asm.tree.*",
			"jakarta.annotation.*",
			"jakarta.enterprise.context.*",
			"jakarta.inject.*",
			"org.slf4j.Logger"
	);

	private ScriptSourceAugmentation() {}

	/**
	 * @param scriptSource
	 * 		Source to inspect.
	 *
	 * @return {@code true} when the script already declares a class.
	 */
	public static boolean isClassScript(@Nonnull String scriptSource) {
		return RegexUtil.matchesAny(PATTERN_CLASS_NAME, scriptSource);
	}

	/**
	 * @param source
	 * 		Source to inspect.
	 *
	 * @return Declared top-level class name, or {@code null} when none is present.
	 */
	@Nullable
	public static String extractClassName(@Nonnull String source) {
		Matcher matcher = RegexUtil.getMatcher(PATTERN_CLASS_NAME, source);
		if (matcher.find())
			return matcher.group(1);
		return null;
	}

	/**
	 * @param scriptSource
	 * 		Class-shaped script source.
	 *
	 * @return Augmented source with the default script package and imports applied.
	 */
	@Nonnull
	public static AugmentedSource augmentClassScript(@Nonnull String scriptSource) {
		String packageName = SCRIPT_PACKAGE_NAME;
		List<Insertion> insertions = new ArrayList<>(2);

		// Extract package name if present, otherwise insert synthetic package declaration.
		Matcher packageMatcher = RegexUtil.getMatcher(PATTERN_PACKAGE, scriptSource);
		int importOffset = 0;
		if (packageMatcher.find()) {
			packageName = packageMatcher.group(1);
			importOffset = packageMatcher.end();
		} else {
			insertions.add(new Insertion(0, "package " + packageName + ";\n"));
		}

		// Add default imports.
		String imports = "\nimport " + String.join(";\nimport ", DEFAULT_IMPORTS) + ";\n";
		insertions.add(new Insertion(importOffset, imports));
		return build(scriptSource, packageName, insertions);
	}

	/**
	 * @param scriptSource
	 * 		Class-shaped script source.
	 *
	 * @return Augmented source for compilation, or {@code null} when the class name could not be determined.
	 */
	@Nullable
	public static AugmentedSourceUnit augmentStandardClass(@Nonnull String scriptSource) {
		AugmentedSource augmented = augmentClassScript(scriptSource);
		String compileSource = augmented.augmentedSource();

		// Extract class name.
		Matcher matcher = RegexUtil.getMatcher(PATTERN_CLASS_NAME, compileSource);
		if (!matcher.find())
			return null;
		String originalName = matcher.group(1);
		String modifiedName = originalName + Math.abs(compileSource.hashCode());
		String internalClassName = augmented.packageInternalName() + "/" + modifiedName;

		// Replace name in script:
		//  - Class definition
		//  - Constructors
		compileSource = StringUtil.replaceRange(compileSource, matcher.start(1), matcher.end(1), modifiedName);
		compileSource = compileSource.replace(" " + originalName + "(", " " + modifiedName + "(");
		compileSource = compileSource.replace("\t" + originalName + "(", "\t" + modifiedName + "(");

		return new AugmentedSourceUnit(internalClassName, augmented.withAugmentedSource(compileSource));
	}

	/**
	 * Used when the script immediately starts with the code.
	 * This will wrap that content in a basic class.
	 *
	 * @param className
	 * 		Simple generated class name.
	 * @param scriptSource
	 * 		Snippet-style script source.
	 *
	 * @return Augmented source for compilation.
	 */
	@Nonnull
	public static AugmentedSourceUnit augmentSnippetClass(@Nonnull String className, @Nonnull String scriptSource) {
		Set<String> imports = new HashSet<>(DEFAULT_IMPORTS);
		Matcher matcher = RegexUtil.getMatcher(PATTERN_IMPORT, scriptSource);
		String sanitizedScript = scriptSource;
		while (matcher.find()) {
			// Record import statement
			String importIdentifier = matcher.group(1);
			imports.add(importIdentifier);

			// Replace text with spaces to maintain script character offsets
			String importMatch = sanitizedScript.substring(matcher.start(), matcher.end());
			sanitizedScript = sanitizedScript.replace(importMatch, " ".repeat(importMatch.length()));
		}

		// Create code (just a basic class with a static 'run' method)
		// This all gets shoved onto a single line to minimize line number shifts for the original script content.
		StringBuilder code = new StringBuilder();
		code.append("package " + SCRIPT_PACKAGE_NAME + "; ");
		for (String imp : imports)
			code.append("import ").append(imp).append("; ");
		code.append("@Dependent public class ").append(className)
				.append(" implements Runnable, Opcodes { ")
				.append("private static final Logger log = Logging.get(\"script\"); ")
				.append("private Workspace workspace; ")
				.append("@Inject ").append(className).append("(Workspace workspace) { this.workspace = workspace; } ")
				.append("public void run() {\n");
		int bodyOffset = code.length();
		code.append(sanitizedScript).append("\n").append("}").append("}");

		// Build class name, and final unit model.
		String internalClassName = SCRIPT_PACKAGE_NAME.replace('.', '/') + "/" + className;
		return new AugmentedSourceUnit(internalClassName, snippetAugmentedSource(scriptSource, code.toString(), bodyOffset));
	}

	/**
	 * Builds an augmented source for a snippet-style script, where the original source is wrapped in a basic class structure.
	 *
	 * @param originalSource
	 * 		The original snippet source provided by the user.
	 * @param compileSource
	 * 		The full augmented source that will be compiled. Includes the surrounding class structure and imports.
	 *
	 * @return Augmented source for compilation.
	 *
	 * @see #augmentSnippetClass(String, String)
	 */
	@Nonnull
	private static AugmentedSource snippetAugmentedSource(@Nonnull String originalSource,
	                                                      @Nonnull String compileSource,
	                                                      int bodyOffset) {
		return new AugmentedSource(
				originalSource,
				compileSource,
				SCRIPT_PACKAGE_NAME,
				shiftedOffsets(originalSource.length(), bodyOffset),
				shiftedLineMap(originalSource, 1)
		);
	}

	/**
	 * Builds the augmented source by applying the given insertions to the original source, and calculating the offset and line mappings.
	 *
	 * @param originalSource
	 * 		The original source text.
	 * @param packageName
	 * 		The final package name of the source. Assumed to already be present in the original source.
	 * @param insertions
	 * 		List of insertions to apply to the original source.
	 *
	 * @return Augmented source with the insertions applied and mappings calculated.
	 */
	@Nonnull
	private static AugmentedSource build(@Nonnull String originalSource,
	                                     @Nonnull String packageName,
	                                     @Nonnull List<Insertion> insertions) {
		// Sort insertions by their original offset to ensure they are applied in the correct order.
		List<Insertion> ordered = insertions.stream()
				.sorted(Comparator.comparingInt(Insertion::offset))
				.toList();
		int totalInsertedLength = ordered.stream().mapToInt(insertion -> insertion.text().length()).sum();
		StringBuilder augmented = new StringBuilder(originalSource.length() + totalInsertedLength);
		int[] originalToAugmented = new int[originalSource.length() + 1];
		List<Integer> augmentedLineToOriginalLine = new ArrayList<>();
		augmentedLineToOriginalLine.add(0); // unused placeholder to keep line math 1-based.
		augmentedLineToOriginalLine.add(1);

		// Add each insertion in order, while also tracking the modified
		// offsets from these insertions to build our mapping tables.
		int originalOffset = 0;
		int originalLine = 1;
		int augmentedOffset = 0;
		for (Insertion insertion : ordered) {
			// Append original text up to the insertion point.
			while (originalOffset < insertion.offset()) {
				originalToAugmented[originalOffset] = augmentedOffset;
				char c = originalSource.charAt(originalOffset++);
				augmented.append(c);
				augmentedOffset++;
				if (c == '\n') {
					originalLine++;
					augmentedLineToOriginalLine.add(originalLine);
				}
			}

			// Append the insertion text.
			for (int i = 0; i < insertion.text().length(); i++) {
				char c = insertion.text().charAt(i);
				augmented.append(c);
				augmentedOffset++;
				if (c == '\n')
					augmentedLineToOriginalLine.add(originalLine);
			}
		}

		// Last append of original text after all insertions are done.
		while (originalOffset < originalSource.length()) {
			originalToAugmented[originalOffset] = augmentedOffset;
			char c = originalSource.charAt(originalOffset++);
			augmented.append(c);
			augmentedOffset++;
			if (c == '\n') {
				originalLine++;
				augmentedLineToOriginalLine.add(originalLine);
			}
		}
		originalToAugmented[originalSource.length()] = augmentedOffset;

		// Map the list to an array and wrap it all up.
		int[] lineMap = augmentedLineToOriginalLine.stream().mapToInt(Integer::intValue).toArray();
		return new AugmentedSource(originalSource,
				augmented.toString(),
				packageName,
				originalToAugmented,
				lineMap);
	}

	/**
	 * Create a simple identity mapping for <i>"augmented"</i> code has a flat offset shift from the original source.
	 *
	 * @param length
	 * 		Source length to create the mapping for.
	 * @param offsetShift
	 * 		Number of characters that the augmented source is shifted from the original source.
	 *
	 * @return Original to augmented offset mapping with a flat shift applied.
	 *
	 * @see #augmentSnippetClass(String, String) Snippets insert all boilerplate before the original source, so it's just a flat shift of all offsets.
	 */
	@Nonnull
	private static int[] shiftedOffsets(int length, int offsetShift) {
		int[] offsets = new int[length + 1];
		for (int i = 0; i <= length; i++)
			offsets[i] = i + offsetShift;
		return offsets;
	}

	/**
	 * Creates a line mapping that shifts all lines by a specified amount.
	 *
	 * @param originalSource
	 * 		Original source to determine the number of lines for the mapping.
	 * @param lineShift
	 * 		Number of lines to shift.
	 *
	 * @return Line mapping array where the index is the augmented line number and the value is the corresponding original line number.
	 */
	@Nonnull
	private static int[] shiftedLineMap(@Nonnull String originalSource, int lineShift) {
		// Count lines in the original source to determine the size of the line mapping table.
		int originalLineCount = 1;
		for (int i = 0; i < originalSource.length(); i++)
			if (originalSource.charAt(i) == '\n')
				originalLineCount++;

		// Create a line mapping that shifts all lines by the specified amount,
		// while ensuring that lines beyond the original source still map to the last original line.
		int[] lineMap = new int[originalLineCount + lineShift + 2];
		lineMap[0] = 0;
		for (int generatedLine = 1; generatedLine < lineMap.length; generatedLine++) {
			int originalLine = generatedLine <= lineShift ? 1 : generatedLine - lineShift;
			lineMap[generatedLine] = Math.min(originalLine, originalLineCount);
		}
		return lineMap;
	}

	/**
	 * Simple insertion model.
	 *
	 * @param offset
	 * 		The offset in some text where an insertion should occur.
	 * @param text
	 * 		The text to insert.
	 */
	private record Insertion(int offset, @Nonnull String text) {}
}
