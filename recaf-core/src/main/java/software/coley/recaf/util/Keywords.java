package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Java language keywords.
 *
 * @author Matt Coley
 */
public class Keywords {
	private static final Set<String> keywords;

	/**
	 * @return Set of reserved keywords.
	 */
	@Nonnull
	public static Set<String> getKeywords() {
		return keywords;
	}

	// Commented out items are 'keywords' but can be used as names.
	static {
		// Misc language constructs
		Set<String> words = new HashSet<>();
		words.addAll(Arrays.asList(
				"assert",
				"break",
				// "bridge",
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
				// "mandated",
				// "module",
				"new",
				// "open",
				"package",
				"record",
				"return",
				"super",
				"switch",
				"try",
				"this",
				"throw",
				"throws"
				// "var",
				// "varargs",
				// "yield"
		));

		// Modifiers
		words.addAll(Arrays.asList(
				"abstract",
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
	}
}
