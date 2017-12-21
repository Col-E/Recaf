package me.coley.recaf.event.impl;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.event.Event;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

/**
 * Created when: User opens a class.
 * 
 * @author Matt
 */
public class EClassSelect extends Event {
	private final ClassDisplayPanel cdp;

	public EClassSelect(ClassDisplayPanel cdp) {
		this.cdp = cdp;
	}

	/**
	 * @return Panel containing class editing UI for the {@link #getClassNode()
	 *         class}.
	 */
	public ClassDisplayPanel getDisplay() {
		return cdp;
	}

	/**
	 * @return ClassNode selected.
	 */
	public ClassNode getClassNode() {
		return cdp.getNode();
	}
}
