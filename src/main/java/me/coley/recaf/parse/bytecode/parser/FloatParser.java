package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.NumberAST;

/**
 * {@link NumberAST} parser for floats.
 *
 * @author Matt
 */
public class FloatParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			int start = line.indexOf(trim);
			if(!trim.matches("-?[.\\d]+[Ff]?"))
				if (trim.matches("Infinity[Ff]"))
					return new NumberAST(lineNo, getOffset() + start, Float.POSITIVE_INFINITY);
				else if (trim.matches("-Infinity[Ff]"))
					return new NumberAST(lineNo, getOffset() + start, Float.NEGATIVE_INFINITY);
				else if (trim.matches("NaN[Ff]"))
					return new NumberAST(lineNo, getOffset() + start, Float.NaN);
				else if (!trim.matches("-?[\\d.]+(?:[eE]-?\\d+)?[Ff]?"))
					throw new ASTParseException(lineNo, "Invalid float: " + trim);
			return new NumberAST(lineNo, getOffset() + start, Float.valueOf(trim));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
