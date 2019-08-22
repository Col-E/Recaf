package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.RegexUtil;

import java.util.*;

/**
 * List name parser.
 *
 * @author Matt
 */
public class ListParser extends Parser {
	private static final String LIST_PATTERN = "(?<=\\[).*?(?=\\])";

	/**
	 * Construct an list parser.
	 *
	 * @param id Parser identifier.
	 */
	public ListParser(String id) {
		super(id);
	}

	@Override
	public Object parse(String text) throws LineParseException {
		List<String> list = Arrays.asList(getToken(text).split("[, ]+"));
		if (list.isEmpty())
			throw new LineParseException(text, "List must not be empty!");
		return list;
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
		String token = RegexUtil.getFirstToken(LIST_PATTERN, text);
		if (token == null)
			throw new LineParseException(text, "No list to match");
		return token;
	}
}
