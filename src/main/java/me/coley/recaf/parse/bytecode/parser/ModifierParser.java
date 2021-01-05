package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AccessFlag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link DefinitionModifierAST} parser.
 *
 * @author Matt
 */
public class ModifierParser extends AbstractParser<DefinitionModifierAST> {
	private static final List<String> ALLOWED_NAMES = Arrays.stream(AccessFlag.values())
			.map(AccessFlag::getName)
			.collect(Collectors.toList());

	@Override
	public DefinitionModifierAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim().toLowerCase();
			if(!ALLOWED_NAMES.contains(trim))
				throw new ASTParseException(lineNo, "Illegal method modifier '" + trim + "'");
			int start = line.indexOf(trim);
			return new DefinitionModifierAST(lineNo, getOffset() + start, trim);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for modifier");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		String trim = text.trim();
		return ALLOWED_NAMES.stream().filter(n -> n.startsWith(trim)).collect(Collectors.toList());
	}

}
