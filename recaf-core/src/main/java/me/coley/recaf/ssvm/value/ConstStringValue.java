package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.DelegatingInstanceValue;
import dev.xdark.ssvm.value.InstanceValue;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A constant {@code String} value.
 *
 * @author Matt Coley
 */
public class ConstStringValue extends DelegatingInstanceValue<InstanceValue> implements ConstValue {
	private final List<AbstractInsnNode> contributing = new ArrayList<>();
	private final List<AbstractInsnNode> associatedPops = new ArrayList<>();
	private final List<TrackedValue> clonedValues = new ArrayList<>();

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
	public List<AbstractInsnNode> getContributingInstructions() {
		return contributing;
	}

	@Override
	public List<AbstractInsnNode> getAssociatedPops() {
		return associatedPops;
	}

	@Override
	public List<TrackedValue> getClonedValues() {
		return clonedValues;
	}

	@Override
	public void addContributing(AbstractInsnNode insn) {
		contributing.add(insn);
	}

	@Override
	public void addAssociatedPop(AbstractInsnNode pop) {
		associatedPops.add(pop);
	}

	@Override
	public void addClonedValue(TrackedValue value) {
		clonedValues.add(value);
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
