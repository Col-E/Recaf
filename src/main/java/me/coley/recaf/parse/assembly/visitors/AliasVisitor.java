package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;

import java.util.List;

/**
 * Alias parser.
 *
 * @author Matt
 */
public class AliasVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public AliasVisitor(AssemblyVisitor asm) {
		super(asm);
		// Format: ALIAS hello "Hello World!"
		// Reference: LDC "${name}"
		addSection(new NameParser(NameParser.VarType.VARIABLE));
		addSection(new StringParser());
	}

	@Override
	public void visitPre(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		String content = (String) args.get(2);
		asm.getAliases().add(name, content);
	}

	@Override
	public void visit(String text) throws LineParseException {}
}
