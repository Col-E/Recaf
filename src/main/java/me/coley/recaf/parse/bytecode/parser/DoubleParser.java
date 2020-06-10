package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.NumberAST;

/**
 * {@link NumberAST} parser for doubles.
 *
 * @author Matt
 */
public class DoubleParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			// Check standard numbers, then exponential form form if that fails
			int start = line.indexOf(trim);
			if(!trim.matches("-?[.\\d]+[Dd]?"))
				if (trim.equals("Infinity"))
					return new NumberAST(lineNo, getOffset() + start, Double.POSITIVE_INFINITY);
				else if (trim.equals("-Infinity"))
					return new NumberAST(lineNo, getOffset() + start, Double.NEGATIVE_INFINITY);
				else if (trim.equals("NaN"))
					return new NumberAST(lineNo, getOffset() + start, Double.NaN);
				else if (!trim.matches("-?[\\d.]+(?:[eE]-?\\d+)?[dD]?"))
					throw new ASTParseException(lineNo, "Invalid double: " + trim);
			return new NumberAST(lineNo, getOffset() + start, Double.valueOf(trim));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
