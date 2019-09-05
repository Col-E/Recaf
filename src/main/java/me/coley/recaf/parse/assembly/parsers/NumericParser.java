package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.RegexUtil;

import java.util.Collections;
import java.util.List;

/**
 * Number value parser.
 *
 * @author Matt
 */
public class NumericParser extends Parser {
	private static final String NUMBER_PATTERN = "-?\\d+\\.?\\d*[fFdDlLbB]*";

	/**
	 * Construct an number parser.
	 *
	 * @param id Parser identifier.
	 */
	public NumericParser(String id) {
		super(id);
	}

	@Override
	public Object parse(String text) throws LineParseException {
		String token = getToken(text);
		try {
			if(token.endsWith("F"))
				return Float.parseFloat(token);
			if(token.endsWith("D") || token.contains("."))
				return Double.parseDouble(token);
			if(token.endsWith("L"))
				return Long.parseLong(token);
			if(token.endsWith("B"))
				return Byte.parseByte(token);
			if(token.endsWith("S"))
				return Short.parseShort(token);
			return Integer.parseInt(token);
		} catch(NumberFormatException ex) {
			LineParseException parseEx = new LineParseException(text, "Failed to parse number format");
			parseEx.addSuppressed(ex);
			throw parseEx;
		}
	}

	@Override
	public int endIndex(String text) throws LineParseException {
		String token = getToken(text);
		return text.indexOf(token) + token.length();
	}

	@Override
	public List<String> getSuggestions(String text) throws LineParseException {
		return Collections.emptyList();
	}

	private String getToken(String text) throws LineParseException {
		String token = RegexUtil.getFirstToken(NUMBER_PATTERN, text);
		if (token == null)
			throw new LineParseException(text, "No word to match");
		return token.toUpperCase();
	}
}
