package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.DefinitionAST;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

/**
 * Wrapper parser for all parsers using the {@code DEFINE} keyword.
 *
 * @author Matt
 * @see FieldDefinitionParser
 * @see MethodDefinitionParser
 */
public class DefinitionParser extends AbstractParser<DefinitionAST> {
	public static final String DEFINE = "DEFINE";
	public static final int DEFINE_LEN = (DEFINE + " ").length();

	@Override
	public DefinitionAST visit(int lineNo, String text) throws ASTParseException {
		if (text.contains("("))
			return new MethodDefinitionParser().visit(lineNo, text);
		else
			return new FieldDefinitionParser().visit(lineNo, text);
	}
}
