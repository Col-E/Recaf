package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.Named;
import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;

/**
 * An abstraction of a code offset by ASM. Used by variables and other attributes to make
 * tracking applicable ranges more legible.
 *
 * @author Matt Coley
 */
public class Label extends BaseElement implements CodeEntry, Named {
	private final String name;

	public Label(String name) {
		this.name = name;
	}

	@Override
	public void insertInto(Code code) {
		code.addLabel(this);
	}

	@Override
	public String print() {
		return name + ":";
	}

	@Override
	public String getName() {
		return name;
	}
}
