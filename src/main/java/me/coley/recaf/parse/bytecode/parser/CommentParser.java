package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.CommentAST;

/**
 * {@link CommentAST} parser.
 *
 * @author Matt
 */
public class CommentParser extends AbstractParser<CommentAST> {
	@Override
	public CommentAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			String content = trim.substring(2);
			int start = line.indexOf(trim);
			return new CommentAST(lineNo, start, content);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for comment, expected \"//\" at beginning");
		}
	}
}
