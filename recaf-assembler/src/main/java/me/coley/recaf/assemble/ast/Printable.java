package me.coley.recaf.assemble.ast;

/**
 * Represents some disassemble-able content.
 *
 * @author Matt Coley
 */
public interface Printable {
	/**
	 * @param context
	 * 		Print context.
	 *
	 * @return Disassembled representation.
	 */
	String print(PrintContext context);
}
