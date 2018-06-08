package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Event;

/**
 * Event for when a class is marked as dirty <i>(next save-state will update
 * this class)</i> due to a rename action.
 * 
 * @author Matt
 */
public class ClassRenameEvent extends Event {
	private final ClassNode node;
	private final String newName;
	private final String originalName;

	public ClassRenameEvent(ClassNode node, String originalName, String newName) {
		this.node = node;
		this.originalName = originalName;
		this.newName = newName;
	}

	/**
	 * @return Node updated.
	 */
	public ClassNode getNode() {
		return node;
	}

	/**
	 * @return New name for the class.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 * @return Original name of the class.
	 */
	public String getOriginalName() {
		return originalName;
	}

}