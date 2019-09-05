package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;

/**
 * Comment parser.
 *
 * @author Matt
 */
public class CommentVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public CommentVisitor(AssemblyVisitor asm) {
		super(asm);
	}

	@Override
	public void visit(String text) throws LineParseException {}
}
