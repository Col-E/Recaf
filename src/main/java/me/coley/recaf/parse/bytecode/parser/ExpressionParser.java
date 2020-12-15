package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link ExpressionAST} parser.
 *
 * @author Matt
 */
public class ExpressionParser extends AbstractParser<ExpressionAST> {
	private static final int EXPR_OFFSET = "EXPR ".length();

	@Override
	public ExpressionAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim().substring(EXPR_OFFSET);
			int start = line.indexOf(EXPR_OFFSET);
			return new ExpressionAST(lineNo, start, trim);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for expression");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		return Collections.emptyList();
	}
}
