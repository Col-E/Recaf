package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.InstanceValue;

/**
 * A constant {@code String} value.
 *
 * @author Matt Coley
 */
public class ConstStringValue extends TrackedInstanceValue implements ConstValue {
	/**
	 * @param delegate
	 * 		Wrapped string value.
	 */
	public ConstStringValue(InstanceValue delegate) {
		super(delegate);
		// Sanity check
		assert delegate.getJavaClass().getInternalName().equals("java/lang/String");
	}

	/**
	 * @param helper
	 * 		Helper of the VM to create value in.
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstStringValue ofString(VMHelper helper, String value) {
		return new ConstStringValue((InstanceValue) helper.newUtf8(value));
	}

	@Override
	public ConstStringValue clone() {
		return new ConstStringValue(getDelegate());
	}

	@Override
	public String toString() {
		return "ConstStringValue[" + getDelegate().toString() + "]";
	}
}
