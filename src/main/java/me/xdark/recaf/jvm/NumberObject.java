package me.xdark.recaf.jvm;

public abstract class NumberObject extends Object {
	protected NumberObject(VirtualMachine vm) {
		super(vm);
	}

	public abstract int intValue();

	public abstract long longValue();

	public abstract float floatValue();

	public abstract double doubleValue();

	public abstract byte byteValue();

	public abstract short shortValue();
}
