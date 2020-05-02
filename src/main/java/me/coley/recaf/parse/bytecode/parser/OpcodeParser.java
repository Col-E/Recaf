package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.OpcodeUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link OpcodeAST} parser.
 *
 * @author Matt
 */
public class OpcodeParser extends AbstractParser<OpcodeAST> {
	public static final Set<String> ALLOWED_NAMES = OpcodeUtil.getInsnNames();

	@Override
	public OpcodeAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim().toUpperCase();
			if(!ALLOWED_NAMES.contains(trim))
				throw new ASTParseException(lineNo, "Illegal opcode '" + trim + "'");
			int start = line.indexOf(trim);
			return new OpcodeAST(lineNo, getOffset() + start, trim);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for opcode");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		String trim = text.trim();
		return ALLOWED_NAMES.stream().filter(n -> n.startsWith(trim)).collect(Collectors.toList());
	}

}
