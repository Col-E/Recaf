package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recreation of {@link Frame} for subtype relationship inference in {@link PhantomMethodConstraintAnalysis}.
 *
 * @author Matt Coley
 * @see SubtypeValue
 */
public class SubtypeFrameState {
	private final Map<Integer, SubtypeValue> locals;
	private final List<SubtypeValue> stack;

	/**
	 * @param locals
	 * 		Local variable state.
	 * @param stack
	 * 		Operand stack state.
	 */
	public SubtypeFrameState(@Nonnull Map<Integer, SubtypeValue> locals, @Nonnull List<SubtypeValue> stack) {
		this.locals = new HashMap<>(locals);
		this.stack = new ArrayList<>(stack);
	}

	/**
	 * @return Mutable local state.
	 */
	@Nonnull
	public Map<Integer, SubtypeValue> getLocals() {
		return locals;
	}

	/**
	 * @return Mutable operand stack state.
	 */
	@Nonnull
	public List<SubtypeValue> getStack() {
		return stack;
	}

	/**
	 * @return Copy of this state.
	 */
	@Nonnull
	public SubtypeFrameState copy() {
		// The constructor already makes copies of the collections.
		return new SubtypeFrameState(locals, stack);
	}

	/**
	 * @param other
	 * 		Incoming state to merge.
	 *
	 * @return Merged state.
	 */
	@Nonnull
	public SubtypeFrameState merge(@Nonnull SubtypeFrameState other) {
		Map<Integer, SubtypeValue> mergedLocals = new HashMap<>(locals);
		for (Map.Entry<Integer, SubtypeValue> entry : other.locals.entrySet())
			mergedLocals.merge(entry.getKey(), entry.getValue(), SubtypeValue::merge);

		List<SubtypeValue> mergedStack;
		if (stack.size() == other.stack.size()) {
			mergedStack = new ArrayList<>(stack.size());
			for (int i = 0; i < stack.size(); i++)
				mergedStack.add(SubtypeValue.merge(stack.get(i), other.stack.get(i)));
		} else if (stack.isEmpty()) {
			mergedStack = new ArrayList<>(other.stack);
		} else if (other.stack.isEmpty()) {
			mergedStack = new ArrayList<>(stack);
		} else {
			mergedStack = new ArrayList<>();
		}

		return new SubtypeFrameState(mergedLocals, mergedStack);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SubtypeFrameState that))
			return false;
		return locals.equals(that.locals)
				&& stack.equals(that.stack);
	}

	@Override
	public int hashCode() {
		int result = locals.hashCode();
		result = 31 * result + stack.hashCode();
		return result;
	}
}
