package me.coley.recaf.assemble.ast;

/**
 * Represents some disassemble-able content.
 *
 * @author Matt Coley
 */
public interface Printable {
	/**
	 * @return Disassembled representation.
	 */
	String print();
}
