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
				"boolean",
				"break",
				// "bridge",
				"byte",
				"case",
				"catch",
				"char",
				"class",
				"const",
				"continue",
				"default",
				"do",
				"double",
				"else",
				"enum",
				"extends",
				"final",
				"finally",
				"float",
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
				// "open",
				"private",
				"protected",
				"public",
				"static",
				"strictfp",
				"super",
				"synchronized",
				// "synthetic",
				"transient",
				"throws",
				// "transitive",
				// "var",
				// "varargs",
				"volatile"
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
