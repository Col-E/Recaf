package me.coley.recaf.parse.bytecode;

import org.objectweb.asm.Type;

import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RVirtual))
			return false;
		return Objects.equals(type, ((RVirtual) o).type);
	}
}
