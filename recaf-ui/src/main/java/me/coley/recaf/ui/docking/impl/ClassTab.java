package me.coley.recaf.ui.docking.impl;

import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.util.EscapeUtil;

/**
 * Wrapper around content representing a {@link me.coley.recaf.code.CommonClassInfo}.
 *
 * @author Matt Coley
 * @see ClassRepresentation
 */
public class ClassTab extends DockTab {
	private final ClassRepresentation classRepresentation;

	/**
	 * @param title
	 * 		Title of the tab.
	 * @param classRepresentation
	 * 		Representation of the class.
	 */
	public ClassTab(String title, ClassRepresentation classRepresentation) {
		super(EscapeUtil.escape(title), classRepresentation.getNodeRepresentation());
		this.classRepresentation = classRepresentation;
	}

	/**
	 * @return Representation of the class.
	 */
	public ClassRepresentation getClassRepresentation() {
		return classRepresentation;
	}
}
