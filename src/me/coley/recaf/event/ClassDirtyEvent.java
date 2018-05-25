package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Event;

/**
 * Event for when a class is marked as dirty <i>(next save-state will update
 * this class)</i>
 * 
 * @author Matt
 */
public class ClassDirtyEvent extends Event {
	private final ClassNode node;

	public ClassDirtyEvent(ClassNode node) {
		this.node = node;
	}

	/**
	 * @return Node updated.
	 */
	public ClassNode getNode() {
		return node;
	}
}