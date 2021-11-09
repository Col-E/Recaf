package me.coley.recaf.assemble.bytecode;

import me.coley.recaf.assemble.ast.Unit;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into normal bytecode.
 *
 * @author Matt Coley
 */
public class BytecodeGenerator {
	private final Unit unit;

	public BytecodeGenerator(Unit unit) {
		this.unit = unit;
	}

	// TODO: All the things
}
