package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A constant numeric value.
 *
 * @author Matt Coley
 */
public class ConstNumericValue extends SimpleDelegatingValue<NumericValue> implements ConstValue {
	private final List<AbstractInsnNode> contributing = new ArrayList<>();
	private final List<AbstractInsnNode> associatedPops = new ArrayList<>();
	private final List<TrackedValue> parentValues = new ArrayList<>();
	private final List<TrackedValue> clonedValues = new ArrayList<>();

	/**
	 * @param delegate
	 * 		Wrapped numeric value.
	 */
	public ConstNumericValue(NumericValue delegate) {
		super(delegate);
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofInt(int value) {
		return new ConstNumericValue(IntValue.of(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofLong(long value) {
		return new ConstNumericValue(LongValue.of(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofFloat(float value) {
		return new ConstNumericValue(new FloatValue(value));
	}

	/**
	 * @param value
	 * 		Value to wrap.
	 *
	 * @return SSVM value.
	 */
	public static ConstNumericValue ofDouble(double value) {
		return new ConstNumericValue(new DoubleValue(value));
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
	public ConstNumericValue clone() {
		return new ConstNumericValue(getDelegate());
	}

	@Override
	public String toString() {
		return "ConstNumericValue[" + getDelegate().toString() + "]";
	}
}
