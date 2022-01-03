package me.coley.recaf.assemble.analysis;

/**
 * Exception to wrap anything going wrong in {@link Frame#merge(Frame, Analyzer)}.
 *
 * @author Matt Coley
 */
public class FrameMergeException extends Exception {
	/**
	 * @param message
	 * 		Details of the merge failure.
	 */
	public FrameMergeException(String message) {
		super(message);
	}
}
