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

	/**
	 * @param name
	 * 		Class name to check.
	 *
	 * @return {@code true} when the class name contains a reserved Java keyword.
	 */
	public static boolean containsClassKeyword(@Nonnull String name) {
		// package-info and module-info are allowed like that, but we don't want to match
		// the prefix keywords in those names. Quick workaround here via substitution.
		String filtered = name.indexOf('-') > 5 ?
				name.replace("package-info", "package_info").replace("module-info", "module_info") :
				name;
		return containsKeyword(filtered);
	}

	/**
	 * @param name
	 * 		Variable name to check.
	 *
	 * @return {@code true} when the variable name contains a reserved Java keyword.
	 */
	public static boolean containsVariableKeyword(@Nonnull String name) {
		return containsSetWord(name, keywordsWithoutVarSafe);
	}

	/**
	 * @param name
	 * 		Class, field, method or variable name to check.
	 *
	 * @return {@code true} when the name contains a reserved Java keyword.
	 */
	public static boolean containsKeyword(@Nonnull String name) {
		return containsSetWord(name, keywords);
	}

	/**
	 * @param name
	 * 		Name to check.
	 * @param keywordSet
	 * 		Set of keywords to check against.
	 *
	 * @return {@code true} when the name contains a reserved Java keyword.
	 */
	private static boolean containsSetWord(@Nonnull String name, @Nonnull Set<String> keywordSet) {
		int length = name.length();
		int start = 0;
		for (int i = 0; i <= length; i++) {
			if (i < length && Character.isJavaIdentifierPart(name.charAt(i)))
				continue;
			if (i > start && keywordSet.contains(name.substring(start, i)))
				return true;
			start = i + 1;
		}
		return false;
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
