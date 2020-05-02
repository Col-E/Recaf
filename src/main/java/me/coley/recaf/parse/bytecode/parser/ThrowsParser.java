package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link ThrowsAST} parser.
 *
 * @author Matt
 */
public class ThrowsParser extends AbstractParser<ThrowsAST> {
	private static final int THROWS_LEN = "THROWS ".length();

	@Override
	public ThrowsAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			int start = line.indexOf(trim);
			// Sub-parser, set offset so it starts after THROWS's ' ', so error reporting yields
			// the correct character offset.
			TypeParser typeParser = new TypeParser();
			typeParser.setOffset(start + THROWS_LEN);
			TypeAST type = typeParser.visit(lineNo, trim.substring(THROWS_LEN));
			return new ThrowsAST(lineNo, start, type);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for THROWS");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if (text.length() <= THROWS_LEN)
			return Collections.emptyList();
		return new TypeParser().suggest(lastParse, text.substring(THROWS_LEN));
	}
}
