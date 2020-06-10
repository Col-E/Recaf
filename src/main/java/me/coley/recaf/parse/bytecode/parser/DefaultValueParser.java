package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.ast.AST;
import me.coley.recaf.parse.bytecode.ast.DefaultValueAST;
import me.coley.recaf.parse.bytecode.ast.RootAST;

import java.util.Collections;
import java.util.List;

/**
 * {@link DefaultValueAST} parser.
 *
 * @author Matt
 */
public class DefaultValueParser extends AbstractParser<DefaultValueAST> {
	private static final String PREFIX = "VALUE ";

	@Override
	public DefaultValueAST visit(int lineNo, String line) throws ASTParseException {
		try {
			int offset = PREFIX.length();
			String content = line.substring(offset).trim();
			AST ast = null;
			if(content.contains("\"")) {
				// String
				StringParser parser = new StringParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			} else if(content.contains("[") || content.contains(";")) {
				// Type
				DescParser parser = new DescParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			} else if(content.endsWith("F") || content.endsWith("f")) {
				// Float
				FloatParser parser = new FloatParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			} else if(content.endsWith("L") || content.endsWith("l") ||
					  content.endsWith("J") || content.endsWith("j")) {
				// Long
				LongParser parser = new LongParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			} else if(content.contains(".")) {
				// Double
				DoubleParser parser = new DoubleParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			} else {
				// Integer
				IntParser parser = new IntParser();
				parser.setOffset(offset);
				ast = parser.visit(lineNo, content);
			}
			return new DefaultValueAST(lineNo, 0, ast);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for LDC");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		return Collections.emptyList();
	}
}
