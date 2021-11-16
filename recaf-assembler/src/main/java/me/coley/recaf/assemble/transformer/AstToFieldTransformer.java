package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.Unit;

import java.util.Objects;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into normal field.
 *
 * @author Matt Coley
 */
public class AstToFieldTransformer {
	private final Unit unit;

	/**
	 * @param unit
	 * 		The unit to pull data from.
	 */
	public AstToFieldTransformer(Unit unit) {
		this.unit = Objects.requireNonNull(unit);
	}
}
