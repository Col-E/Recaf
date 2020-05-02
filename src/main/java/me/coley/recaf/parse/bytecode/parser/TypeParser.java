package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.ast.TypeAST;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link TypeAST} parser.
 *
 * @author Matt
 */
public class TypeParser extends AbstractParser<TypeAST> {
	private static final char[] ILLEGAL_CHARS = {';', ',', '.', ' ', '\t'};
	private static final char[] ILLEGAL_CHARS_ARRAY = {',', '.', ' ', '\t'};

	@Override
	public TypeAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if (trim.charAt(0) == '[') {
				// Handle array types
				for(char c : ILLEGAL_CHARS_ARRAY)
					if(trim.indexOf(c) >= 0)
						throw new ASTParseException(lineNo, "Contains illegal character '" + c + "'");
			} else {
				// Handle normal types
				for(char c : ILLEGAL_CHARS)
					if(trim.indexOf(c) >= 0)
						throw new ASTParseException(lineNo, "Contains illegal character '" + c + "'");
			}
			int start = line.indexOf(trim);
			return new TypeAST(lineNo, getOffset() + start, trim);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for type");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		String trim = text.trim();
		if (trim.isEmpty())
			return Collections.emptyList();
		return AutoCompleteUtil.internalName(trim);
	}
}
