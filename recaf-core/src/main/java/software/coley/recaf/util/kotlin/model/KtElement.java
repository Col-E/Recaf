package software.coley.recaf.util.kotlin.model;

/**
 * Common interface for top-level kotlin metadata structures.
 *
 * @author Matt Coley
 */
public sealed interface KtElement permits KtClass, KtVariable, KtFunction {}
