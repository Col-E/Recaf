package me.xdark.recaf.jvm;

public final class IntegerObject extends NumberObject {
	private final int value;

	protected IntegerObject(VirtualMachine vm, int value) {
		super(vm);
		this.value = value;
	}

	@Override
	public int intValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public byte byteValue() {
		return (byte) value;
	}

	@Override
	public short shortValue() {
		return (short) value;
	}
}
