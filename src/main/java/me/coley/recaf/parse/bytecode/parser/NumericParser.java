package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

/**
 * {@link NumberAST} parser.
 *
 * @author Matt
 */
public class NumericParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String content = line.trim();
			NumberAST ast = null;
			if(content.endsWith("F") || content.endsWith("f")) {
				// Float
				FloatParser parser = new FloatParser();
				ast = parser.visit(lineNo, content);
			} else if(content.endsWith("L") || content.endsWith("l") ||
					  content.endsWith("J") || content.endsWith("j")) {
				// Long
				LongParser parser = new LongParser();
				ast = parser.visit(lineNo, content);
			} else if(content.contains(".")) {
				// Double
				DoubleParser parser = new DoubleParser();
				ast = parser.visit(lineNo, content);
			} else {
				// Integer
				IntParser parser = new IntParser();
				ast = parser.visit(lineNo, content);
			}
			return ast;
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
