package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

/**
 * {@link MethodInsnAST} parser.
 *
 * @author Matt
 */
public class MethodInsnParser extends AbstractParser<MethodInsnAST> {
	@Override
	public MethodInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough paramters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// owner & name & desc
			String data = trim[1];
			int dot = data.indexOf('.');
			if (dot == -1)
				throw new ASTParseException(lineNo, "Format error: expecting '<Owner>.<Name><Desc>'" +
						" - missing '.'");
			int parenthesis = data.indexOf('(');
			if (parenthesis < dot)
				throw new ASTParseException(lineNo, "Format error: Missing valid method descriptor");
			String typeS = data.substring(0, dot);
			String nameS = data.substring(dot + 1, parenthesis);
			String descS = data.substring(parenthesis);
			// owner
			TypeParser typeParser = new TypeParser();
			typeParser.setOffset(line.indexOf(data));
			TypeAST owner = typeParser.visit(lineNo, typeS);
			// name
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf('.'));
			NameAST name = nameParser.visit(lineNo, nameS);
			// desc
			DescParser descParser = new DescParser();
			descParser.setOffset(line.indexOf('('));
			DescAST desc = descParser.visit(lineNo, descS);
			// itf
			ItfAST itf = null;
			if (op.getOpcode() == Opcodes.INVOKESTATIC) {
				if (trim.length > 2 && "itf".equals(trim[2])) {
					itf = new ItfAST(lineNo, line.indexOf("itf", desc.getStart() + desc.getDesc().length()));
				}
			}
			return new MethodInsnAST(lineNo, start, op, owner, name, desc, itf);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for method instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// METHOD owner.name+desc [itf]
		int space = text.indexOf(' ');
		if (space >= 0) {
			int secondSpace = text.indexOf(' ', space + 1);
			if (secondSpace >= 0) {
				if ("INVOKESTATIC".equals(text.substring(0, space))) {
					return Collections.singletonList("itf");
				} else {
					return Collections.emptyList();
				}
			}

			String sub = text.substring(space + 1);
			int dot = sub.indexOf('.');
			if (dot == -1)
				return new TypeParser().suggest(lastParse, sub);
			return AutoCompleteUtil.method(sub);
		}
		return Collections.emptyList();
	}
}
