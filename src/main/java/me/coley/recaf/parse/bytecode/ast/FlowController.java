package me.coley.recaf.parse.bytecode.ast;

import java.util.List;

/**
 * Common to all instructions that modify flow control.
 *
 * @author Matt
 */
public interface FlowController {
	/**
	 * @return List of label names this instruction may branch to.
	 */
	List<String> targets();
}
