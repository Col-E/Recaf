package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal recreation of {@link Frame} for subtype relationship inference in {@link PhantomMethodConstraintAnalysis}.
 *
 * @author Matt Coley
 * @see SubtypeValue
 */
public class SubtypeFrameState {
	private final Int2ObjectMap<SubtypeValue> locals;
	private final List<SubtypeValue> stack;

	/**
	 * @param locals
	 * 		Local variable state.
	 * @param stack
	 * 		Operand stack state.
	 */
	public SubtypeFrameState(@Nonnull Int2ObjectMap<SubtypeValue> locals, @Nonnull List<SubtypeValue> stack) {
		this.locals = new Int2ObjectMap<>(locals);
		this.stack = new ArrayList<>(stack);
	}

	/**
	 * @return Mutable local state.
	 */
	@Nonnull
	public Int2ObjectMap<SubtypeValue> getLocals() {
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
	 * 		Other state to compare against.
	 *
	 * @return {@code true} when both states carry operand stacks of different, non-empty heights.
	 * Such states cannot be merged, see {@link #merge(SubtypeFrameState)}.
	 */
	public boolean stackHeightsConflict(@Nonnull SubtypeFrameState other) {
		return !stack.isEmpty() && !other.stack.isEmpty() && stack.size() != other.stack.size();
	}

	/**
	 * @param other
	 * 		Incoming state to merge.
	 * 		Must not {@link #stackHeightsConflict(SubtypeFrameState) conflict} with this state.
	 *
	 * @return Merged state.
	 */
	@Nonnull
	public SubtypeFrameState merge(@Nonnull SubtypeFrameState other) {
		if (stackHeightsConflict(other))
			throw new IllegalStateException("Cannot merge operand stacks of different heights");

		Int2ObjectMap<SubtypeValue> mergedLocals = new Int2ObjectMap<>(locals);
		other.locals.forEach((index, value) -> {
			Objects.requireNonNull(value, "Cannot merge null local variable value");
			SubtypeValue existing = mergedLocals.get(index);
			mergedLocals.put(index, existing == null ? value : SubtypeValue.merge(existing, value));
		});

		List<SubtypeValue> mergedStack;
		if (stack.isEmpty()) {
			mergedStack = new ArrayList<>(other.stack);
		} else if (other.stack.isEmpty()) {
			mergedStack = new ArrayList<>(stack);
		} else {
			mergedStack = new ArrayList<>(stack.size());
			for (int i = 0; i < stack.size(); i++)
				mergedStack.add(SubtypeValue.merge(stack.get(i), other.stack.get(i)));
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
