package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.DelegatingInstanceValue;
import dev.xdark.ssvm.value.InstanceValue;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracked instance value.
 *
 * @author xDark
 */
public class TrackedInstanceValue extends DelegatingInstanceValue<InstanceValue> implements TrackedValue {
	private final List<AbstractInsnNode> contributing = new ArrayList<>();
	private final List<AbstractInsnNode> associatedPops = new ArrayList<>();
	private final List<TrackedValue> parentValues = new ArrayList<>();
	private final List<TrackedValue> clonedValues = new ArrayList<>();

	/**
	 * @param delegate
	 * 		Wrapped instance value.
	 */
	public TrackedInstanceValue(InstanceValue delegate) {
		super(delegate);
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
	public TrackedValue clone() {
		return new TrackedInstanceValue(getDelegate());
	}
}
