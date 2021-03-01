package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.ast.TypeAST;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;
import me.coley.recaf.util.EscapeUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link TypeAST} parser.
 *
 * @author Matt
 */
public class TypeParser extends AbstractParser<TypeAST> {
	@Override
	public TypeAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			trim = EscapeUtil.unescape(trim);
			if (trim.charAt(0) == '[') {
				// Handle array types
				if (!trim.matches("\\S+"))
					throw new ASTParseException(lineNo, "Name cannot contain whitespace characters");
			} else {
				// Handle normal types, cannot have any '[' or ';' in it
				if(!trim.matches("[^\\[;]+"))
					throw new ASTParseException(lineNo, "Contains illegal characters");
				if (!trim.matches("\\S+"))
					throw new ASTParseException(lineNo, "Name cannot contain whitespace characters");
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
