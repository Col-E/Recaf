package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.*;
import me.coley.recaf.parse.assembly.parsers.OpParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Base visitor for method instructions.
 *
 * @author Matt
 */
public abstract class InstructionVisitor implements Visitor<String> {
	protected final AssemblyVisitor asm;
	private Parser chain;

	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public InstructionVisitor(AssemblyVisitor asm) {
		this.asm = asm;
		addSection(new OpParser());
	}

	/**
	 * Adds a parser to the instruction chain parse list.
	 *
	 * @param parser
	 * 		Parser to append.
	 */
	protected void addSection(Parser parser) {
		if(chain == null) {
			// Initial parser
			chain = parser;
		} else {
			// Append parser
			chain.append(parser);
		}
	}

	/**
	 * Collects parsed values from the chain parse list.
	 *
	 * @param text
	 * 		Text to parse.
	 *
	 * @return List of parsed values.
	 *
	 * @throws LineParseException
	 * 		When one of the parsers fails to interpret the line.
	 */
	protected List<Object> parse(String text) throws LineParseException {
		List<Object> values = new ArrayList<>();
		chain.consume(text, values);
		return values;
	}

	/**
	 * @param text
	 * 		Text to get suggestions for.
	 *
	 * @return List of suggestions for the last section of the chain parse list.
	 *
	 * @throws LineParseException
	 * 		When one of the parsers fails to interpret the line.
	 */
	public List<String> suggest(String text) throws LineParseException {
		return chain.suggest(text);
	}
}
