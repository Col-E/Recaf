package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Event;

/**
 * Event for when a class is selected.
 * 
 * @author Matt
 */
public class ClassOpenEvent extends Event {
	private final ClassNode node;

	public ClassOpenEvent(ClassNode node) {
		this.node = node;
	}

	/**
	 * @return Node selected.
	 */
	public ClassNode getNode() {
		return node;
	}
}