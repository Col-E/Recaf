package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.NumberUtil;
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
	 *
	 * @throws FrameMergeException
	 * 		When the frames cannot be merged for some reason.
	 * 		Exception message will elaborate on exact cause.
	 */
	public boolean merge(Frame otherFrame, Analyzer analyzer) throws FrameMergeException {
		wonky |= otherFrame.isWonky();
		if (visited) {
			boolean modified = false;
			InheritanceChecker typeChecker = analyzer.getInheritanceChecker();
			for (Map.Entry<String, Value> e : locals.entrySet()) {
				String name = e.getKey();
				Value value = e.getValue();
				Value newValue = mergeValue(value, otherFrame.getLocal(name), typeChecker);
				setLocal(name, newValue);
				modified |= !value.equals(newValue);
			}
			int max = getStack().size();
			int otherMax = otherFrame.getStack().size();
			if (max != otherMax) {
				throw new FrameMergeException("Unmatched stack size, " + max + " != " + otherMax);
			}
			for (int i = 0; i < max; i++) {
				Value value = getStack().get(i);
				Value otherValue = otherFrame.getStack().get(i);
				Value newValue = mergeValue(value, otherValue, typeChecker);
				getStack().set(i, newValue);
				modified |= !value.equals(newValue);
			}
			return modified;
		} else {
			copy(otherFrame);
			return false;
		}
	}

	private static Value mergeValue(Value value, Value otherValue, InheritanceChecker typeChecker) throws FrameMergeException {
		if (value instanceof Value.TypeValue) {
			if (otherValue instanceof Value.TypeValue) {
				Value.TypeValue type = (Value.TypeValue) value;
				Value.TypeValue otherType = (Value.TypeValue) otherValue;
				// Get common type
				String common = typeChecker.getCommonType(
						type.getType().getInternalName(),
						otherType.getType().getInternalName());
				Type commonType = Type.getType(common);
				// Update the local with the new type
				return new Value.TypeValue(commonType);
			} else {
				throw new FrameMergeException("Values not types in both frames!");
			}
		} else if (value instanceof Value.ObjectValue) {
			if (otherValue instanceof Value.ObjectValue) {
				Value.ObjectValue object = (Value.ObjectValue) value;
				Value.ObjectValue otherObject = (Value.ObjectValue) otherValue;
				// Get common type
				String common = typeChecker.getCommonType(
						object.getType().getInternalName(),
						otherObject.getType().getInternalName());
				Type commonType = Type.getObjectType(common);
				return new Value.ObjectValue(commonType);
			} else {
				throw new FrameMergeException("Values not objects in both frames!");
			}
		} else if (value instanceof Value.NumericValue) {
			if (otherValue instanceof Value.NumericValue) {
				Value.NumericValue numeric = (Value.NumericValue) value;
				Value.NumericValue otherNumeric = (Value.NumericValue) otherValue;
				// Select widest type
				Type widest = NumberUtil.getWidestType(numeric.getType(), otherNumeric.getType());
				if (Objects.equals(numeric.getNumber(), otherNumeric.getNumber())) {
					numeric = new Value.NumericValue(widest, numeric.getNumber());
				} else {
					numeric = new Value.NumericValue(widest);
				}
				return numeric;
			} else {
				throw new FrameMergeException("Values not numeric in both frames!");
			}
		}
		return value;
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
	 *
	 * @return Prior visited value.
	 */
	public boolean markVisited() {
		boolean was = visited;
		this.visited = true;
		return was;
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
