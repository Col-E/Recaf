package me.xdark.recaf.jvm;

public final class Field extends Member {
	private final int slot;

	protected Field(String name, String descriptor, Class declaringClass, int modifiers, boolean synthetic, int slot) {
		super(name, descriptor, declaringClass, modifiers, synthetic);
		this.slot = slot;
	}

	public int getSlot() {
		return slot;
	}
}
