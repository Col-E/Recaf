package me.coley.recaf.parse.bytecode;

import org.objectweb.asm.Type;

public class RVirtual {
	private final Type type;

	public RVirtual(Type type) {
		this.type = type;
	}

	public boolean isArray() {
		return type.getSort() == Type.ARRAY;
	}

	@Override
	public String toString() {
		return type.getInternalName();
	}
}
