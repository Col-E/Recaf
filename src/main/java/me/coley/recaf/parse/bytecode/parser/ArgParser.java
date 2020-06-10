package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link DefinitionArgAST} parser.
 *
 * @author Matt
 */
public class ArgParser extends AbstractParser<DefinitionArgAST> {
	@Override
	public DefinitionArgAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if (!trim.matches(".+\\s+.+"))
				throw new IllegalStateException();
			int start = line.indexOf(trim);
			String[] split = trim.split("\\s+");
			String typeStr = split[0];
			String nameStr = split[1];
			DescParser descParser = new DescParser();
			descParser.setOffset(start);
			DescAST descAST = descParser.visit(lineNo, typeStr);
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(nameStr));
			NameAST nameAST = nameParser.visit(lineNo, nameStr);
			return new DefinitionArgAST(lineNo, getOffset() + start, descAST, nameAST);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for arg, expected \"<type> <name>\"");
		}
	}
}
