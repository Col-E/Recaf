package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.util.List;

/**
 * Indy type instruction parser.
 *
 * @author Matt
 */
public class InvokeDynamicVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public InvokeDynamicVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NameParser(NameParser.VarType.VARIABLE));
		addSection(new DescriptorParser("desc", DescriptorParser.DescType.METHOD));
		// handle
		addSection(new HandleParser("handle"));
		// args
		addSection(new ListParser("args"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		String desc = (String) args.get(2);
		Handle handle = (Handle) args.get(3);
		// Hacky way to fetch args content
		List<String> argsStr = (List<String>) args.get(4);
		Object[] handleArgs = new Object[argsStr.size()];
		for (int i = 0; i < argsStr.size(); i++) {
			MultiParser parser = new MultiParser("value",
					new HandleParser("value"),
					new DescriptorParser("value", DescriptorParser.DescType.METHOD),
					new StringParser(),
					new NumericParser("value")
			);
			String arg = argsStr.get(i);
			Object parsed = parser.parse(arg);
			if (parser.getLastUsed() instanceof DescriptorParser)
				parsed = Type.getMethodType((String) parsed);
			handleArgs[i] = parsed;
		}
		asm.appendInsn(new InvokeDynamicInsnNode(name, desc, handle, handleArgs));
	}
}
