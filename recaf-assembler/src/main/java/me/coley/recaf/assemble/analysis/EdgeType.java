package me.coley.recaf.assemble.analysis;

/**
 * Type of control flow edge between frames.
 *
 * @author Matt Coley
 * @see Edge
 */
public enum EdgeType {
	EXCEPTION_HANDLER,
	JUMP,
	THROW
}
