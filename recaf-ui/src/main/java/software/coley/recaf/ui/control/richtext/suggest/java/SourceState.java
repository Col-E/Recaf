package software.coley.recaf.ui.control.richtext.suggest.java;

/**
 * State of the source at a given offset.
 *
 * @author Matt Coley
 */
public enum SourceState {
	CODE,
	LINE_COMMENT,
	BLOCK_COMMENT,
	STRING,
	CHARACTER
}
