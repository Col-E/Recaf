package software.coley.recaf.ui.control.richtext.suggest.java;

/**
 * Kind of context the caret is in.
 *
 * @author Matt Coley
 */
public enum ContextKind {
	NONE,
	MEMBER,
	METHOD_REFERENCE,
	IMPORT,
	PACKAGE,
	TYPE,
	IDENTIFIER
}
