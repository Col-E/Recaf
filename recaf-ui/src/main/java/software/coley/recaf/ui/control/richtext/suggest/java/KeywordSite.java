package software.coley.recaf.ui.control.richtext.suggest.java;

/**
 * Coarse context buckets for filtering keyword completions.
 *
 * @author Matt Coley
 */
public enum KeywordSite {
	UNKNOWN,
	/** Context before writing the top-level class declaration. */
	TOP_LEVEL_DECLARATION,
	/** Context within 'public class Foo extends Bar implements FizzBuzz' region. */
	TYPE_HEADER,
	/** Context within a class declaration <i>(For fields and such)</i>. */
	TYPE_BODY_DECLARATION,
	/** Context within a method declaration <i>(For parameters and such)</i>. */
	METHOD_HEADER,
	/** Context within a method body. */
	METHOD_BODY,
	/** Context within an expression. */
	EXPRESSION_LIKE
}
