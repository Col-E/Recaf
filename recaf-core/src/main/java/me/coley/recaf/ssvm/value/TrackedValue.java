package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.Value;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Collection;
import java.util.List;

/**
 * A {@link dev.xdark.ssvm.value.Value} that also holds the instructions that contribute to it.
 *
 * @author Matt Coley
 */
public interface TrackedValue extends Value {
	/**
	 * @return The instructions that contribute to this value.
	 */
	List<AbstractInsnNode> getContributingInstructions();

	/**
	 * @return Any pop instructions that may operate on this value in the future.
	 */
	List<AbstractInsnNode> getAssociatedPops();

	/**
	 * @return Values that contributed to this one as a result of a clone operation such as {@code DUP}.
	 */
	List<TrackedValue> getParentValues();

	/**
	 * @return Values that spawned from this one as a result of a clone operation such as {@code DUP}.
	 */
	List<TrackedValue> getClonedValues();

	/**
	 * @param insn
	 * 		Instruction that contributes to this value.
	 */
	void addContributing(AbstractInsnNode insn);

	/**
	 * @param insns
	 * 		Instructions that contribute to this value.
	 */
	default void addContributing(Collection<AbstractInsnNode> insns) {
		insns.forEach(this::addContributing);
	}

	/**
	 * @param value
	 * 		Value to that contribute to this value.
	 */
	default void addContributing(TrackedValue value) {
		addParentValue(value);
		addContributing(value.getContributingInstructions());
		value.getAssociatedPops().forEach(this::addAssociatedPop);
		value.getClonedValues().forEach(this::addClonedValue);
	}

	/**
	 * @param values
	 * 		Values to that contribute to this value.
	 */
	default void addContributing(TrackedValue... values) {
		for (TrackedValue value : values) {
			addContributing(value);
		}
	}

	/**
	 * @param pop
	 *        {@code POP} or {@code POP2} instruction that may operate on this value in the future.
	 */
	void addAssociatedPop(AbstractInsnNode pop);

	/**
	 * @param value
	 * 		Parent of this value.
	 */
	void addParentValue(TrackedValue value);

	/**
	 * @param value
	 * 		Clone of this value.
	 */
	void addClonedValue(TrackedValue value);

	/**
	 * @return Copy of the current value.
	 */
	TrackedValue clone();
}
