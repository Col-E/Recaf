package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Try catch parser.
 *
 * @author Matt
 */
public class CatchVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public CatchVisitor(AssemblyVisitor asm) {
		super(asm);
		// Format: CATCH ExceptionInternalType L_START L_END L_HANDLER
		addSection(new InternalNameParser("type"));
		addSection(new NameParser(NameParser.VarType.VARIABLE));
		addSection(new NameParser(NameParser.VarType.VARIABLE));
		addSection(new NameParser(NameParser.VarType.VARIABLE));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String type = (String) args.get(1);
		String start = (String) args.get(2);
		LabelNode lblStart = asm.getLabels().get(start);
		if (lblStart == null)
			throw new LineParseException(text, "No label by the given name: " + start);
		String end = (String) args.get(3);
		LabelNode lblEnd = asm.getLabels().get(end);
		if (lblEnd == null)
			throw new LineParseException(text, "No label by the given name: " + end);
		String handler = (String) args.get(4);
		LabelNode lblHandler = asm.getLabels().get(handler);
		if (lblHandler == null)
			throw new LineParseException(text, "No label by the given name: " + handler);
		asm.appendCatch(new TryCatchBlockNode(lblStart, lblEnd, lblHandler, type));
	}
}
