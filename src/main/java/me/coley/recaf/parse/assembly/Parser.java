package me.coley.recaf.parse.assembly;

import java.util.List;

/**
 * Parser base.
 *
 * @author Matt
 */
public abstract class Parser {
	private final String id;
	private Parser next;
	private Parser prev;
	private Object value;

	/**
	 * @param id
	 * 		Parser identifier.
	 */
	public Parser(String id) {
		this.id = id;
	}

	/**
	 * Appends a parser to the chain.
	 *
	 * @param next
	 * 		Next section parser.
	 */
	public void append(Parser next) {
		if(this.next == null)
			this.next = next.with(this);
		else
			this.next.append(next);
	}

	/**
	 * @param prev
	 * 		Parser to set as prior link in parse chain.
	 *
	 * @return Parser with the previous parser set.
	 */
	private Parser with(Parser prev) {
		this.prev = prev;
		return this;
	}

	/**
	 * Visit parser chain with the given text.
	 *
	 * @param text
	 * 		Text to parse.
	 * @param values
	 * 		List to add values to.
	 *
	 * @throws LineParseException
	 * 		When the segment being analyzed by the current parser could not be minimally analyzed
	 * 		in order to generate suggestions.
	 */
	public void consume(String text, List<Object> values) throws LineParseException {
		values.add(value = parse(text));
		if(next != null)
			next.consume(text.substring(endIndex(text)), values);
	}

	/**
	 * @param text
	 * 		Text to get suggestions for.
	 *
	 * @return List of suggestions to complete the text.
	 *
	 * @throws LineParseException
	 * 		When the segment being analyzed by the current parser could not be minimally analyzed
	 * 		in order to generate suggestions.
	 */
	public List<String> suggest(String text) throws LineParseException {
		// Suggest if we're at the end of the parse chain
		if(next == null)
			return getSuggestions(text);
		try {
			// Get subtext for next parser in chain
			String sub = text.substring(endIndex(text));
			if (!sub.trim().isEmpty()) {
				// Try to suggest for the next parser.
				// - If it throws an exception we will try the current parser instead.
				return next.suggest(sub);
			}
		} catch(LineParseException ex) {
			// Expected
		}
		return getSuggestions(text);
	}

	/**
	 * @return Parsed value. {@code null} if {{@link #parse(String)}} has not been called
	 * previously.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @return Parser identification.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return Next parser in the chain.
	 */
	public Parser getNext() {
		return next;
	}

	/**
	 * @return Previous parser in the chain.
	 */
	public Parser getPrev() {
		return prev;
	}

	/**
	 * @param id
	 * 		Parser identifier.
	 *
	 * @return Parser in chain matching the identifier.
	 */
	public Parser getById(String id) {
		if (this.id.equals(id))
			return this;
		if (next == null)
			return null;
		return next.getById(id);
	}

	/**
	 * @return Root parser in the chain.
	 */
	public Parser getRoot() {
		Parser p = this;
		while(p.prev != null)
			p = p.prev;
		return p;
	}

	/**
	 * @param text
	 * 		Text to match.
	 *
	 * @return Value of match.
	 *
	 * @throws LineParseException
	 * 		When the segment being analyzed by the current parser does not match the expected
	 * 		content.
	 */
	protected abstract Object parse(String text) throws LineParseException;

	/**
	 * Finds the end of the parser match so the next parser can be fed a substring starting at the
	 * end of the current match.
	 *
	 * @param text
	 * 		Text to match.
	 *
	 * @return End index of match.
	 *
	 * @throws LineParseException
	 * 		When the segment being analyzed by the current parser does not match the expected
	 * 		content.
	 */
	protected abstract int endIndex(String text) throws LineParseException;

	/**
	 * Generates suggestions to complete
	 *
	 * @param text
	 * 		Text to match.
	 *
	 * @return List of suggestions on how to complete the text.
	 *
	 * @throws LineParseException
	 * 		When the segment being analyzed by the current parser could not be minimally analyzed
	 * 		in order to generate suggestions.
	 */
	protected abstract List<String> getSuggestions(String text) throws LineParseException;
}
