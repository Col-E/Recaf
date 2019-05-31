package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Event;

/**
 * Event for when a class is recompiled.
 * 
 * @author Matt
 */
public class ClassRecompileEvent extends Event {
	private final ClassNode oldValue, newValue;

	public ClassRecompileEvent(ClassNode oldValue, ClassNode newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	/**
	 * @return Old instance of node.
	 */
	public ClassNode getOldNode() {
		return oldValue;
	}
	
	/**
	 * @return New instance of node.
	 */
	public ClassNode getNewNode() {
		return newValue;
	}
}