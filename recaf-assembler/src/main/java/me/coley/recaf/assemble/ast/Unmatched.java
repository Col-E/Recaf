package me.coley.recaf.assemble.ast;

/**
 * Handles any unmatched text.
 *
 * @author Matt Coley
 */
public class Unmatched extends BaseElement implements CodeEntry {
	private String raw;

	/**
	 * @param raw
	 * 		Raw text of unmatched value.
	 */
	public Unmatched(String raw) {
		this.raw = raw;
	}

	/**
	 * @param text
	 * 		Text to append.
	 */
	public void append(String text) {
		raw += text;
	}

	@Override
	public void insertInto(Code code) {
		code.addUnmatched(this);
	}

	@Override
	public String print() {
		return raw;
	}
}
