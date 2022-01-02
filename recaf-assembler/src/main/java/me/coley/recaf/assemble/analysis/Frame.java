package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * Snapshot of computed local/stack data.
 *
 * @author Matt Coley
 * @see Value
 */
public class Frame {
	private final Map<String, Value> locals = new HashMap<>();
	private final List<Value> stack = new ArrayList<>();
	private boolean visited;
	private boolean wonky;

	/**
	 * @param selfTypeName
	 * 		Type of the class defining the method being analyzed.
	 * @param definition
	 * 		The method definition.
	 */
	public void initialize(String selfTypeName, MethodDefinition definition) {
		stack.clear();
		locals.clear();
		// Add "this"
		if (!AccessFlag.isStatic(definition.getModifiers().value())) {
			locals.put("this", new Value.ObjectValue(Type.getObjectType(selfTypeName)));
		}
		// Add parameters
		for (MethodParameter parameter : definition.getParams()) {
			String desc = parameter.getDesc();
			Type paramType = Type.getType(desc);
			int sort = paramType.getSort();
			Value v;
			if (sort <= Type.DOUBLE) {
				v = new Value.NumericValue(paramType);
			} else if (Types.isBoxedPrimitive(desc)) {
				v = new Value.NumericValue(paramType, null, false);
			} else {
				v = new Value.ObjectValue(Type.getType(parameter.getDesc()));
			}
			setLocal(parameter.getName(), v);
		}
	}

	/**
	 * @param otherFrame
	 * 		Frame to merge with.
	 * @param analyzer
	 * 		Analyzer for context and type hierarchy lookups.
	 *
	 * @return {@code true} when the merge changed the stack or local states.
	 * {@code false} if no changes were made after the merge was completed.
	 */
	public boolean merge(Frame otherFrame, Analyzer analyzer) {
		wonky |= otherFrame.isWonky();
		if (visited) {
			boolean modified = false;
			// Check for type conflicts on the stack and in locals, merging into a common type

			// TODO: compare types, using analyzer to resolve hierarchy
			return modified;
		} else {
			copy(otherFrame);
			return false;
		}
	}

	/**
	 * @param frame
	 * 		Frame to copy the state of.
	 */
	public void copy(Frame frame) {
		stack.clear();
		locals.clear();
		stack.addAll(frame.stack);
		locals.putAll(frame.locals);
	}

	/**
	 * Set a local variable value.
	 *
	 * @param name
	 * 		Variable name.
	 * @param value
	 * 		Variable value.
	 */
	public void setLocal(String name, Value value) {
		locals.put(name, value);
	}

	/**
	 * @param name
	 * 		Variable name.
	 *
	 * @return Variable value.
	 */
	public Value getLocal(String name) {
		return locals.get(name);
	}

	/**
	 * @return Local variables by name.
	 */
	public Map<String, Value> getLocals() {
		return locals;
	}

	/**
	 * @param value
	 * 		Value to push onto the stack.
	 */
	public void push(Value value) {
		stack.add(value);
	}

	/**
	 * @return Top value from the stack.
	 */
	public Value peek() {
		if (stack.isEmpty())
			return new Value.EmptyPoppedValue();
		return stack.get(stack.size() - 1);
	}

	/**
	 * @return Top value from the stack.
	 */
	public Value pop() {
		if (stack.isEmpty())
			return new Value.EmptyPoppedValue();
		return stack.remove(stack.size() - 1);
	}

	/**
	 * @return Top wide value from the stack.
	 */
	public Value popWide() {
		// Discard wide reserved value
		pop();
		// The actual value we want
		return pop();
	}

	/**
	 * @return Stack values.
	 */
	public List<Value> getStack() {
		return stack;
	}

	/**
	 * @return {@code true} when the frame has been modified by an analyzer going through code flow.
	 * {@code false} indicates dead code that does not get called.
	 */
	public boolean isVisited() {
		return visited;
	}

	/**
	 * See {@link #isVisited()}.
	 */
	public void markVisited() {
		this.visited = true;
	}

	/**
	 * @return {@code true} when the frame is not necessarily depicting accurate values.
	 * This occurs when illegal bytecode is used and causes some behavior to happen that should
	 * not be possible otherwise.
	 * {@code false} if the code at this frame is normal.
	 */
	public boolean isWonky() {
		return wonky;
	}

	/**
	 * See {@link #isWonky()}.
	 */
	public void markWonky() {
		wonky = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Frame frame = (Frame) o;
		return locals.equals(frame.locals) &&
				stack.equals(frame.stack);
	}

	@Override
	public int hashCode() {
		return Objects.hash(locals, stack);
	}
}
