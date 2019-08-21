package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.RegexUtil;

import java.util.Collections;
import java.util.List;

/**
 * String parser.
 *
 * @author Matt
 */
public class StringParser extends Parser {
	private static final String STRING_PATTERN = "(?<=\").*(?=\")";

	/**
	 * Construct a string parser.
	 **/
	public StringParser() {
		super("value");
	}

	@Override
	public Object parse(String text) throws LineParseException {
		return getToken(text);
	}

	@Override
	public int endIndex(String text) throws LineParseException {
		String token = getToken(text);
		return text.indexOf(token) + token.length() + 1;
	}

	@Override
	public List<String> getSuggestions(String text) throws LineParseException {
		return Collections.emptyList();
	}

	private String getToken(String text) throws LineParseException {
		String token = RegexUtil.getFirstToken(STRING_PATTERN, text);
		if(token == null)
			throw new LineParseException(text, "No word to match");
		return token;
	}
}
