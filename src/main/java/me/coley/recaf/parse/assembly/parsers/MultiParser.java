package me.coley.recaf.parse.assembly.parsers;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.Parser;

import java.util.Collections;
import java.util.List;

/**
 * Deferring parser that supports multiple sub-parsers.
 *
 * @author Matt
 */
public class MultiParser extends Parser {
	private final Parser[] subparsers;

	/**
	 * @param id
	 * 		Parser identifier.
	 * @param subparsers
	 * 		Parsers to defer to.
	 */
	public MultiParser(String id, Parser... subparsers) {
		super(id);
		this.subparsers = subparsers;
	}

	@Override
	public Object parse(String text) throws LineParseException {
		for(Parser sub : subparsers) {
			try {
				return sub.parse(text);
			} catch(LineParseException ex) {
				// Expected
			}
		}
		throw new LineParseException(text, "Could not determine a supbarser to use!");
	}

	@Override
	public int endIndex(String text) throws LineParseException {
		for(Parser sub : subparsers) {
			try {
				return sub.endIndex(text);
			} catch(LineParseException ex) {
				// Expected
			}
		}
		throw new LineParseException(text, "Could not determine a supbarser to use!");
	}

	@Override
	public List<String> getSuggestions(String text) throws LineParseException {
		List<String> suggestions = Collections.emptyList();
		for(Parser sub : subparsers) {
			try {
				suggestions = sub.suggest(text);
				if(!suggestions.isEmpty())
					break;
			} catch(LineParseException ex) {
				// Expected
			}
		}
		return suggestions;
	}
}
