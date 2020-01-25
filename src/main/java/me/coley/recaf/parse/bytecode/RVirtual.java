package me.coley.recaf.parse.bytecode;

import org.objectweb.asm.Type;

/**
 * Minimal virtualized object.
 *
 * @author Matt
 */
public class RVirtual {
	private final Type type;

	/**
	 * @param type Type of virtualized object.
	 */
	public RVirtual(Type type) {
		this.type = type;
	}

	/**
	 * @return {@code true} when wirtualized object is an array.
	 */
	public boolean isArray() {
		return type.getSort() == Type.ARRAY;
	}

	@Override
	public String toString() {
		return type.getInternalName();
	}
}
