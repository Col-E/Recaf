package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.InternalNameParser;

import java.util.List;

/**
 * Throws parser. Not to be confused with the instruction <pre>ATRHOW</pre>, this adds types to a
 * method's exceptions list.
 *
 * @author Matt
 */
public class ThrowsVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public ThrowsVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new InternalNameParser("type"));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String type = (String) args.get(1);
		asm.appendThrows(type);
	}
}
