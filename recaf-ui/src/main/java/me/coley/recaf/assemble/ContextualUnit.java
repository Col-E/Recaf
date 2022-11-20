package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.AbstractDefinition;
import me.coley.recaf.assemble.ast.arch.ClassDefinition;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;

/**
 * Tracks the current selected definition <i>(field/method)</i> within a unit holding a {@link ClassDefinition}.
 * For {@link FieldDefinition} and {@link MethodDefinition} units, the standard behavior is retained.
 *
 * @author Matt Coley
 */
public class ContextualUnit extends Unit {
	private final Unit unit;
	private AbstractDefinition currentDefinition;

	/**
	 * @param unit
	 * 		Wrapped unit.
	 */
	public ContextualUnit(Unit unit) {
		super(unit.getDefinition());
		this.unit = unit;
	}

	/**
	 * @param currentDefinition
	 * 		New selection within the class.
	 */
	@SuppressWarnings("all")
	void setCurrentDefinition(AbstractDefinition currentDefinition) {
		if (isClass()) {
			// Must be a valid definition within the class.
			ClassDefinition classDefinition = getDefinitionAsClass();
			if (!classDefinition.getDefinedFields().contains(currentDefinition) &&
					!classDefinition.getDefinedMethods().contains(currentDefinition))
				return;
			this.currentDefinition = currentDefinition;
		}
	}

	/**
	 * @return If the unit definition is {@link ClassDefinition class},
	 * then this yields the current definition where the user has interacted with.
	 * If the unit definition is not a class, then this yields whatever the {@link Unit#getDefinition()} is.
	 */
	AbstractDefinition getCurrentDefinition() {
		if (unit.isClass()) {
			AbstractDefinition currentSelection = currentDefinition;
			return currentSelection == null ? unit.getDefinition() : currentSelection;
		} else return unit.getDefinition();
	}

	/**
	 * @return {@code true} when the {@link #getCurrentDefinition() current active definition} is a method.
	 */
	boolean isCurrentMethod() {
		return getCurrentDefinition().isMethod();
	}

	/**
	 * @return {@code true} when the {@link #getCurrentDefinition() current active definition} is a field.
	 */
	boolean isCurrentField() {
		return getCurrentDefinition().isField();
	}

	/**
	 * @return {@code true} when the {@link #getCurrentDefinition() current active definition} is a class.
	 */
	boolean isCurrentClass() {
		return getCurrentDefinition().isClass();
	}

	/**
	 * @return {@link #getCurrentDefinition() Current active definition} as a method.
	 */
	MethodDefinition getCurrentMethod() {
		AbstractDefinition def = getCurrentDefinition();
		if (!def.isMethod())
			throw new IllegalStateException("Not a method");
		return (MethodDefinition) def;
	}

	/**
	 * @return {@link #getCurrentDefinition() Current active definition} as a field.
	 */
	FieldDefinition getCurrentField() {
		AbstractDefinition def = getCurrentDefinition();
		if (!def.isField())
			throw new IllegalStateException("Not a field");
		return (FieldDefinition) def;
	}
}
