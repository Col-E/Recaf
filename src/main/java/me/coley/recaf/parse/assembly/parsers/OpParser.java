package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.RegexUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Opcode parser.
 *
 * @author Matt
 */
public class OpParser extends Parser {
	/**
	 * Construct an opcode parser.
	 */
	public OpParser() {
		super("Opcode");
	}

	@Override
	protected Object parse(String text) throws LineParseException {
		String token = getToken(text).toUpperCase();
		if (OpcodeUtil.getInsnNames().contains(token))
			return OpcodeUtil.nameToOpcode(token);
		return OpcodeUtil.nameToOpcode(token);
	}

	@Override
	protected int endIndex(String text) throws LineParseException {
		String token = getToken(text);
		return text.indexOf(token) + token.length();
	}

	@Override
	protected List<String> getSuggestions(String text) throws LineParseException {
		String token = getToken(text).toUpperCase();
		return OpcodeUtil.getInsnNames().stream()
				.filter(op -> op.startsWith(token) && !op.equalsIgnoreCase(token))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
	}

	private String getToken(String text) throws LineParseException {
		String token = RegexUtil.getFirstWord(text);
		if (token == null)
			throw new LineParseException(text, "No word to match");
		return token;
	}
}
