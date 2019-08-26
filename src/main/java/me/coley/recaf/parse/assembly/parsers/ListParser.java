package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;
import java.util.*;

/**
 * List parser.
 *
 * @author Matt
 */
public class ListParser extends Parser {
	/**
	 * Construct an list parser.
	 *
	 * @param id
	 * 		Parser identifier.
	 */
	public ListParser(String id) {
		super(id);
	}

	@Override
	public Object parse(String text) throws LineParseException {
		List<String> list = Arrays.asList(getToken(text).split("\\s*,\\s*"));
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

	/**
	 * Match balanced "[...]".
	 * <hr>Normally we
	 *
	 * @param text
	 * 		Text to match.
	 *
	 * @return Content between braces.
	 *
	 * @throws LineParseException Thrown when the text does not match a valid list.
	 */
	private String getToken(String text) throws LineParseException {
		int i = 0;
		int start = -1;
		int end = -2;
		Stack<Character> stack = new Stack<>();
		for(char ch : text.toCharArray()) {
			if(ch == '[') {
				if(stack.isEmpty())
					start = i;
				stack.push(ch);
			} else if(ch == ']') {
				stack.pop();
				if(stack.isEmpty()) {
					end = i;
					break;
				}
			}
			i++;
		}
		if (end < start)
			throw new LineParseException(text, "No list to match");
		return text.substring(start + 1, end);
	}
}
