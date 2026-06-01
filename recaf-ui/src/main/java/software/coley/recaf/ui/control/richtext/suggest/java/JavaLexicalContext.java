package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;

/**
 * Minimal caret-local state used to decide which completion strategy should run.
 *
 * @param kind
 * 		Context kind.
 * @param partialText
 * 		Current partial text to complete.
 * @param receiverText
 * 		Receiver text for member completion.
 * @param receiverResolveOffset
 * 		Offset to resolve receiver against the AST.
 * @param annotationOnly
 * 		Flag for annotation-only type completion.
 * @param keywordSite
 * 		Coarse keyword filtering bucket inferred from lexical context.
 *
 * @author Matt Coley
 */
public record JavaLexicalContext(@Nonnull ContextKind kind,
                                 @Nonnull String partialText,
                                 @Nonnull String receiverText,
                                 int receiverResolveOffset,
                                 boolean annotationOnly,
                                 @Nonnull KeywordSite keywordSite) {

	// For unit tests where we don't care about the keyword site.
	public JavaLexicalContext(@Nonnull ContextKind kind,
	                          @Nonnull String partialText,
	                          @Nonnull String receiverText,
	                          int receiverResolveOffset,
	                          boolean annotationOnly) {
		this(kind, partialText, receiverText, receiverResolveOffset, annotationOnly, KeywordSite.UNKNOWN);
	}

	/**
	 * @return Empty context with no completion strategy.
	 */
	@Nonnull
	public static JavaLexicalContext none() {
		return new JavaLexicalContext(ContextKind.NONE, "", "", -1, false, KeywordSite.UNKNOWN);
	}
}
