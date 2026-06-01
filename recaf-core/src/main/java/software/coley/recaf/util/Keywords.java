package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Java language keywords.
 *
 * @author Matt Coley
 */
public class Keywords {
	private static final Set<String> keywords;
	private static final Set<String> keywordsWithoutVarSafe;

	/**
	 * @return Set of Java keywords.
	 */
	@Nonnull
	public static Set<String> getKeywords() {
		return keywords;
	}

	/**
	 * @return Set of Java reserved keywords that cannot be used as variable names. This is a subset of {@link #getKeywords()}.
	 */
	@Nonnull
	public static Set<String> getKeywordsWithoutVarSafe() {
		return keywordsWithoutVarSafe;
	}

	static {
		// Edge cases that can be used as variable names.
		// Mostly module system words and a few underlying JVM constructs like bridge and synthetic.
		Set<String> allowedVarNames = new TreeSet<>(Arrays.asList(
				"bridge",
				"mandated",
				"module",
				"open",
				"permits",
				"record",
				"sealed",
				"synthetic",
				"transitive",
				"var",
				"varargs",
				"yield"
		));

		// Misc language constructs
		Set<String> words = new TreeSet<>();
		words.addAll(Arrays.asList(
				"assert",
				"break",
				"case",
				"catch",
				"class",
				"continue",
				"default",
				"do",
				"else",
				"enum",
				"extends",
				"finally",
				"while",
				"for",
				"goto",
				"if",
				"implements",
				"import",
				"instanceof",
				"interface",
				"mandated",
				"module",
				"new",
				"non-sealed",
				"open",
				"package",
				"permits",
				"record",
				"return",
				"sealed",
				"super",
				"switch",
				"try",
				"this",
				"throw",
				"throws",
				"var",
				"varargs",
				"yield"
		));

		// Modifiers - Commented out are not reserved but represent underlying JVM constructs.
		words.addAll(Arrays.asList(
				"abstract",
				// "bridge",
				"const",
				"final",
				"native",
				"private",
				"protected",
				"public",
				"static",
				"strictfp",
				"synchronized",
				// "synthetic",
				"transient",
				// "transitive",
				"volatile"
		));

		// Primitive types
		words.addAll(Arrays.asList(
				"boolean",
				"byte",
				"char",
				"short",
				"int",
				"long",
				"float",
				"double",
				"void"
		));

		// Primitive values
		words.addAll(Arrays.asList(
				"true",
				"false",
				"null"
		));

		keywords = Collections.unmodifiableSet(words);
		keywordsWithoutVarSafe = words.stream()
				.filter(k -> !allowedVarNames.contains(k))
				.collect(Collectors.toUnmodifiableSet());
	}
}
