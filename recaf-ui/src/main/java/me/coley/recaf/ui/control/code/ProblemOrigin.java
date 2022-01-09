package me.coley.recaf.ui.control.code;

/**
 * Type to indicate where the problem originated from.
 *
 * @author Matt Coley
 */
public enum ProblemOrigin {
	JAVA_COMPILE,
	JAVA_SYNTAX,
	BYTECODE_PARSING,
	BYTECODE_VALIDATION,
	BYTECODE_COMPILE
}
