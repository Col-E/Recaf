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
	private static final Set<String> keywords = new HashSet<>();

	/**
	 * @return Set of reserved keywords.
	 */
	@Nonnull
	public static Set<String> getKeywords() {
		return Collections.unmodifiableSet(keywords);
	}

	static {
		// Commented out items are 'keywords' but can be used as names.
		keywords.addAll(Arrays.asList("abstract",
				"assert",
				"break",
				// "bridge",
				"case",
				"catch",
				"class",
				"const",
				"continue",
				"default",
				"do",
				"else",
				"enum",
				"extends",
				"final",
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
				"native",
				"new",
				// "open",
				"private",
				"protected",
				"public",
				"record",
				"return",
				"static",
				"strictfp",
				"super",
				"synchronized",
				// "synthetic",
				"transient",
				"try",
				"throw",
				"throws",
				// "transitive",
				// "var",
				// "varargs",
				"volatile",
				"yield"
		));
		// Primitive types
		keywords.addAll(Arrays.asList(
				"boolean",
				"byte",
				"char",
				"short",
				"int",
				"long",
				"float",
				"double",
				"void",
				"null"
		));
	}
}
