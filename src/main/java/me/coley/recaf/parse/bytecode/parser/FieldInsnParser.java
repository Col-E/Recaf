package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link FieldInsnAST} parser.
 *
 * @author Matt
 */
public class FieldInsnParser extends AbstractParser<FieldInsnAST> {
	@Override
	public FieldInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 3)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// owner & name
			String typeAndName = trim[1];
			int dot = typeAndName.indexOf('.');
			if (dot == -1)
				throw new ASTParseException(lineNo, "Format error: expecting '<Owner>.<Name> <Desc>'" +
						" - missing '.'");
			String typeS = typeAndName.substring(0, dot);
			String nameS = typeAndName.substring(dot + 1);
			// owner
			TypeParser typeParser = new TypeParser();
			typeParser.setOffset(line.indexOf(typeAndName));
			TypeAST owner = typeParser.visit(lineNo, typeS);
			// name
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf('.'));
			NameAST name = nameParser.visit(lineNo, nameS);
			// desc
			DescParser descParser = new DescParser();
			descParser.setOffset(line.lastIndexOf(trim[2]));
			DescAST desc = descParser.visit(lineNo, trim[2]);
			return new FieldInsnAST(lineNo, start, op, owner, name, desc);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for field instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// DEFINE owner.name desc
		int space = text.indexOf(' ');
		if (space >= 0) {
			String sub = text.substring(space + 1);
			int dot = sub.indexOf('.');
			if (dot == -1)
				return new TypeParser().suggest(lastParse, sub);
			return AutoCompleteUtil.field(sub);
		}
		return Collections.emptyList();
	}
}
