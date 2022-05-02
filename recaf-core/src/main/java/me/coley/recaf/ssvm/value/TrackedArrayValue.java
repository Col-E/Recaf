package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.DelegatingArrayValue;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * An array value where all elements are tracked.
 *
 * @author Matt Coley
 */
public class TrackedArrayValue extends DelegatingArrayValue<ArrayValue> implements TrackedValue {
	private final List<AbstractInsnNode> contributing = new ArrayList<>();
	private final List<AbstractInsnNode> associatedPops = new ArrayList<>();
	private final List<TrackedValue> clonedValues = new ArrayList<>();
	private final BitSet constants;

	/**
	 * @param delegate
	 * 		Wrapped array value.
	 */
	public TrackedArrayValue(ArrayValue delegate) {
		super(delegate);
		constants = new BitSet(delegate.getLength());
	}

	public void setConstant(int index) {
		constants.set(index);
	}

	public void clearConstant(int index) {
		constants.clear(index);
	}

	public boolean isConstant(int index) {
		return constants.get(index);
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
	public TrackedArrayValue clone() {
		return new TrackedArrayValue(getDelegate());
	}

	@Override
	public String toString() {
		return "TrackedArrayValue[" + getDelegate().toString() + "]";
	}
}
