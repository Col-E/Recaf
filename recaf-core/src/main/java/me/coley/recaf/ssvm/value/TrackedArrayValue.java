package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.DelegatingArrayValue;
import dev.xdark.ssvm.value.Value;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * An array value where all elements are tracked.
 *
 * @author Matt Coley
 */
public class TrackedArrayValue extends DelegatingArrayValue<ArrayValue> implements TrackedValue {
	private final List<AbstractInsnNode> contributing = new ArrayList<>();
	private final List<AbstractInsnNode> associatedPops = new ArrayList<>();
	private final List<TrackedValue> parentValues = new ArrayList<>();
	private final List<TrackedValue> clonedValues = new ArrayList<>();
	private final Value[] values;
	private final IntRef constantCount;
	private boolean constantLength;

	/**
	 * @param delegate
	 * 		Wrapped array value.
	 * @param values
	 * 		Tracked values of this array.
	 * @param constantCount
	 * 		Current amount of constant values.
	 */
	private TrackedArrayValue(ArrayValue delegate, Value[] values, IntRef constantCount) {
		super(delegate);
		this.values = values;
		this.constantCount = constantCount;
	}

	/**
	 * @param delegate
	 * 		Wrapped array value.
	 */
	public TrackedArrayValue(ArrayValue delegate) {
		super(delegate);
		values = new Value[delegate.getLength()];
		constantCount = new IntRef();
	}

	/**
	 * @return {@code true} if length of this array
	 * was determined from a constant value.
	 */
	public boolean isConstantLength() {
		return constantLength;
	}

	/**
	 * @param constantLength
	 * 		Whether the length is constant or not.
	 */
	public void setConstantLength(boolean constantLength) {
		this.constantLength = constantLength;
	}

	/**
	 * @param index
	 * 		Index to get tracked value from.
	 *
	 * @return Tracked value.
	 */
	public Value getTrackedValue(int index) {
		return values[index];
	}

	/**
	 * Tracks a value.
	 *
	 * @param index
	 * 		Index at which value was set.
	 * @param value
	 * 		Value to track.
	 */
	public void trackValue(int index, Value value) {
		Value[] values = this.values;
		Value current = values[index];
		values[index] = value;
		if (current == null) {
			if (value instanceof ConstValue) {
				constantCount.increment();
			}
		} else {
			if (current instanceof ConstValue) {
				if (!(value instanceof ConstValue)) {
					constantCount.decrement();
				}
			} else if (value instanceof ConstValue) {
				constantCount.increment();
			}
		}
		if (value instanceof TrackedValue) {
			addContributing((TrackedValue) value);
		}
	}

	/**
	 * @return {@code true} if all values of this
	 * array are constant values.
	 */
	public boolean areAllValuesConstant() {
		return constantCount.value == values.length;
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
	public List<TrackedValue> getParentValues() {
		return parentValues;
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
	public void addParentValue(TrackedValue value) {
		parentValues.add(value);
	}

	@Override
	public void addClonedValue(TrackedValue value) {
		clonedValues.add(value);
	}

	@Override
	public TrackedArrayValue clone() {
		return new TrackedArrayValue(getDelegate(), values, constantCount);
	}

	@Override
	public String toString() {
		return "TrackedArrayValue[" + getDelegate().toString() + "]";
	}

	private static final class IntRef {
		int value;

		void increment() {
			value++;
		}

		void decrement() {
			value--;
		}
	}
}
