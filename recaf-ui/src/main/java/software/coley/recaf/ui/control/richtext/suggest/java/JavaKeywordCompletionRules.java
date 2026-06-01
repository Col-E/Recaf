package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.LocalScopeLookup;
import software.coley.recaf.util.Keywords;
import software.coley.sourcesolver.model.AnnotationExpressionModel;
import software.coley.sourcesolver.model.AssignmentExpressionModel;
import software.coley.sourcesolver.model.ClassModel;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.MethodBodyModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keyword filtering rules for Java source completion.
 *
 * @author Matt Coley
 */
public final class JavaKeywordCompletionRules {
	private static final LocalScopeLookup MODEL_LOOKUP = new LocalScopeLookup();
	private static final Set<String> KEYWORDS_EXCLUDED_FROM_IDENTIFIER_COMPLETION = Set.of(
			"const",
			"goto",
			"mandated",
			"module",
			"open",
			"varargs"
	);
	private static final Set<String> IDENTIFIER_KEYWORDS = Keywords.getKeywords().stream()
			.filter(keyword -> !KEYWORDS_EXCLUDED_FROM_IDENTIFIER_COMPLETION.contains(keyword))
			.collect(Collectors.toUnmodifiableSet());
	private static final Set<String> TOP_LEVEL_DECLARATION_START_KEYWORDS = Set.of(
			"abstract",
			"class",
			"enum",
			"final",
			"import",
			"interface",
			"non-sealed",
			"package",
			"public",
			"record",
			"sealed",
			"strictfp"
	);
	private static final Set<String> TYPE_DECLARATION_KEYWORDS = Set.of(
			"class",
			"enum",
			"extends",
			"implements",
			"interface",
			"non-sealed",
			"permits",
			"record",
			"sealed"
	);
	private static final Set<String> TYPE_HEADER_KEYWORDS = Set.of(
			"extends",
			"implements",
			"permits"
	);
	private static final Set<String> DECLARATION_MODIFIER_KEYWORDS = Set.of(
			"abstract",
			"final",
			"native",
			"private",
			"protected",
			"public",
			"static",
			"strictfp",
			"synchronized",
			"transient",
			"volatile"
	);
	private static final Set<String> STATEMENT_KEYWORDS = Set.of(
			"assert",
			"break",
			"case",
			"catch",
			"continue",
			"default",
			"do",
			"else",
			"finally",
			"for",
			"if",
			"return",
			"switch",
			"throw",
			"try",
			"while",
			"yield"
	);
	private static final Set<String> EXPRESSION_KEYWORDS = Set.of(
			"false",
			"instanceof",
			"new",
			"null",
			"super",
			"this",
			"true"
	);
	private static final Map<KeywordSite, Set<String>> IDENTIFIER_KEYWORDS_BY_SITE = buildIdentifierKeywordsBySite();

	private JavaKeywordCompletionRules() {}

	/**
	 * @param session
	 * 		Current completion session.
	 * @param context
	 * 		Caret-local lexical context.
	 *
	 * @return Keywords that should be offered during generic identifier completion.
	 */
	@Nonnull
	public static Set<String> getIdentifierKeywords(@Nonnull JavaCompletionSession session,
	                                                @Nonnull JavaLexicalContext context) {
		return IDENTIFIER_KEYWORDS_BY_SITE.get(resolveKeywordSite(session, context));
	}

	/**
	 * @return Map of keyword sites to offered keywords.
	 */
	@Nonnull
	private static Map<KeywordSite, Set<String>> buildIdentifierKeywordsBySite() {
		EnumMap<KeywordSite, Set<String>> bySite = new EnumMap<>(KeywordSite.class);
		for (KeywordSite site : KeywordSite.values())
			bySite.put(site, filterKeywords(site));
		return Collections.unmodifiableMap(bySite);
	}

	/**
	 * @param site
	 * 		Keyword site to filter for.
	 *
	 * @return Set of keywords that should be offered for generic identifier completion at the given site.
	 */
	@Nonnull
	private static Set<String> filterKeywords(@Nonnull KeywordSite site) {
		return switch (site) {
			case TOP_LEVEL_DECLARATION -> TOP_LEVEL_DECLARATION_START_KEYWORDS;
			case TYPE_HEADER -> TYPE_HEADER_KEYWORDS;
			case METHOD_BODY, EXPRESSION_LIKE ->
					excludeKeywords(union(TYPE_DECLARATION_KEYWORDS, DECLARATION_MODIFIER_KEYWORDS,
							Set.of("import", "package", "throws")));
			case TYPE_BODY_DECLARATION ->
					excludeKeywords(union(STATEMENT_KEYWORDS, Set.of("import", "instanceof", "new", "package", "super", "this")));
			case METHOD_HEADER -> excludeKeywords(union(STATEMENT_KEYWORDS, TYPE_DECLARATION_KEYWORDS,
					union(EXPRESSION_KEYWORDS, Set.of("import", "package"))));
			case UNKNOWN -> IDENTIFIER_KEYWORDS;
		};
	}

	@Nonnull
	private static Set<String> excludeKeywords(@Nonnull Set<String> excluded) {
		return IDENTIFIER_KEYWORDS.stream()
				.filter(keyword -> !excluded.contains(keyword))
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * @param session
	 * 		Current completion session.
	 * @param context
	 * 		Lexical context of the completion.
	 *
	 * @return Keyword site to use for filtering keyword completions.
	 */
	@Nonnull
	private static KeywordSite resolveKeywordSite(@Nonnull JavaCompletionSession session,
	                                              @Nonnull JavaLexicalContext context) {
		KeywordSite lexicalSite = context.keywordSite();
		KeywordSite astSite = resolveAstKeywordSite(session);

		// If the AST can't give us any information, then we have to trust the lexical context.
		if (astSite == KeywordSite.UNKNOWN)
			return lexicalSite;

		// If the AST gives us a very specific site, then we should trust it over the lexical context, which is more coarse-grained.
		if (astSite == KeywordSite.EXPRESSION_LIKE || astSite == KeywordSite.METHOD_BODY)
			return astSite;

		// The lexical context is based on the live editor text, while the AST can lag behind and use broader model ranges.
		// Unless the AST can specifically prove expression-like or method-body context, prefer the lexical site we just parsed.
		if (lexicalSite != KeywordSite.UNKNOWN)
			return lexicalSite;
		return astSite;
	}

	/**
	 * @param session
	 * 		Current completion session.
	 *
	 * @return Keyword site to use for filtering keyword completions.
	 */
	@Nonnull
	private static KeywordSite resolveAstKeywordSite(@Nonnull JavaCompletionSession session) {
		CompilationUnitModel unit = session.unit();
		int caret = session.caretPosition();

		// Sanity check the unit/bounds first.
		if (unit == null || caret < 0)
			return KeywordSite.UNKNOWN;
		int astPos = session.completionContext().mapCurrentPositionToAst(caret);
		if (astPos < 0)
			return KeywordSite.UNKNOWN;

		// Check the AST to see if we can get a more specific context.
		// This should be more accurate than the lexical context, but may not always be available.
		Model leaf = MODEL_LOOKUP.findDeepestModelAt(unit, astPos);
		if (leaf.getParentOfType(AnnotationExpressionModel.class) != null ||
				leaf.getParentOfType(AssignmentExpressionModel.class) != null)
			return KeywordSite.EXPRESSION_LIKE;
		if (leaf.getParentOfType(MethodBodyModel.class) != null)
			return KeywordSite.METHOD_BODY;

		// If the deepest AST is the method model itself, then we're in the method header.
		MethodModel method = leaf instanceof MethodModel currentMethod ? currentMethod : leaf.getParentOfType(MethodModel.class);
		if (method != null)
			return KeywordSite.METHOD_HEADER;

		// Similarly, if the deepest AST is the class model itself, then we're in the type body declaration.
		ClassModel declaredClass = leaf instanceof ClassModel currentClass ? currentClass : leaf.getParentOfType(ClassModel.class);
		if (declaredClass != null)
			return KeywordSite.TYPE_BODY_DECLARATION;

		// If nothing else works we're probably at the top-level declaration (before the class declaration).
		return KeywordSite.TOP_LEVEL_DECLARATION;
	}

	@Nonnull
	private static Set<String> union(@Nonnull Set<String> first, @Nonnull Set<String> second) {
		Set<String> union = new HashSet<>(first);
		union.addAll(second);
		return Collections.unmodifiableSet(union);
	}

	@Nonnull
	private static Set<String> union(@Nonnull Set<String> first, @Nonnull Set<String> second, @Nonnull Set<String> third) {
		return union(union(first, second), third);
	}
}
