package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.AutoCompleteUtil;
import me.coley.recaf.util.RegexUtil;

import java.util.List;

/**
 * Internal name parser.
 *
 * @author Matt
 */
public class InternalNameParser extends Parser {
	private static final String NAME_PATTERN = "[\\$\\w+\\/]+";

	/**
	 * Construct an internal name.
	 *
	 * @param id Parser identifier.
	 */
	public InternalNameParser(String id) {
		super(id);
	}

	@Override
	protected Object parse(String text) throws LineParseException {
		return getToken(text);
	}

	@Override
	protected int endIndex(String text) throws LineParseException {
		String token = getToken(text);
		return text.indexOf(token) + token.length();
	}

	@Override
	protected List<String> getSuggestions(String text) throws LineParseException {
		return AutoCompleteUtil.internalName(getToken(text));
	}

	private String getToken(String text) throws LineParseException {
		String token = RegexUtil.getFirstToken(NAME_PATTERN, text);
		if (token == null)
			throw new LineParseException(text, "No word to match");
		return token;
	}
}
