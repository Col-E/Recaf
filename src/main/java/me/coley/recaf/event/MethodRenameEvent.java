package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Event;

/**
 * Event for when a method is renamed.
 * 
 * @author Matt
 */
public class MethodRenameEvent extends Event {
	private final ClassNode owner;
	private final MethodNode method;
	private final String newName;
	private final String originalName;

	public MethodRenameEvent(ClassNode owner, MethodNode method, String originalName, String newName) {
		this.owner = owner;
		this.method = method;
		this.originalName = originalName;
		this.newName = newName;
	}

	/**
	 * @return ClassNode containing the method.
	 */
	public ClassNode getOwner() {
		return owner;
	}

	/**
	 * @return Method being renamed.
	 */
	public MethodNode getMethod() {
		return method;
	}

	/**
	 * @return New name for the method.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 * @return Original name of the method.
	 */
	public String getOriginalName() {
		return originalName;
	}

}