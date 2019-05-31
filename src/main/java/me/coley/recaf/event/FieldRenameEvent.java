package me.coley.recaf.event;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import me.coley.event.Event;

/**
 * Event for when a field is renamed.
 * 
 * @author Matt
 */
public class FieldRenameEvent extends Event {
	private final ClassNode owner;
	private final FieldNode field;
	private final String newName;
	private final String originalName;

	public FieldRenameEvent(ClassNode owner, FieldNode field, String originalName, String newName) {
		this.owner = owner;
		this.field = field;
		this.originalName = originalName;
		this.newName = newName;
	}

	/**
	 * @return ClassNode containing the field.
	 */
	public ClassNode getOwner() {
		return owner;
	}

	/**
	 * @return Field being renamed.
	 */
	public FieldNode getField() {
		return field;
	}

	/**
	 * @return New name for the field.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 * @return Original name of the field.
	 */
	public String getOriginalName() {
		return originalName;
	}

}