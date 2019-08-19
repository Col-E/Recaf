package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import me.coley.recaf.util.AutoCompleteUtil;
import me.coley.recaf.util.RegexUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Variable / field / method name parser.
 *
 * @author Matt
 */
public class NameParser extends Parser {
	private static final String FIELD_NAME_PATTERN = "[\\w\\$]+(?=\\s|$)";
	private static final String METHOD_NAME_PATTERN = "[\\w\\$]+(?=\\(|$)";
	private final VarType type;

	/**
	 * Construct a name parser.
	 *
	 * @param type
	 * 		Type of name being parsed.
	 **/
	public NameParser(VarType type) {
		super("name");
		this.type = type;
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
		// Check if suggestions are even supported
		if (!type.isSupported())
			return Collections.emptyList();
		// Get the root parser and apply suggestions
		Parser root = getRoot();
		String token = getToken(text);
		return type.suggest(root, token);
	}

	private String getToken(String text) throws LineParseException {
		String token;
		if(type == VarType.METHOD)
			token = RegexUtil.getFirstToken(METHOD_NAME_PATTERN, text);
		else if(type == VarType.FIELD)
			token = RegexUtil.getFirstToken(FIELD_NAME_PATTERN, text);
		else
			token = RegexUtil.getFirstWord(text);
		if (token == null)
			throw new LineParseException(text, "No word to match");
		return token;
	}

	/**
	 * Kind of name reference.
	 *
	 * @author Matt
	 */
	public enum VarType {
		VARIABLE(false, (owner, token) -> Collections.emptyList()),
		FIELD(true, AutoCompleteUtil::field),
		METHOD(true, AutoCompleteUtil::method);

		private final boolean supported;
		private final BiFunction<Parser, String, List<String>> completion;

		VarType(boolean supported, BiFunction<Parser, String, List<String>> completion) {
			this.supported = supported;
			this.completion = completion;
		}

		boolean isSupported() {
			return supported;
		}

		List<String> suggest(Parser root, String token) {
			return completion.apply(root, token);
		}
	}
}
