package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.util.InheritanceChecker;
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
	private String wonkyReason;

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
				Type type = Type.getType(parameter.getDesc());
				if (type.getSort() == Type.ARRAY) {
					v = new Value.ArrayValue(type.getDimensions(), type.getElementType());
				} else {
					v = new Value.ObjectValue(type);
				}
			}
			setLocal(parameter.getName(), v);
		}
	}

	/**
	 * @param otherFrame
	 * 		Frame to merge with.
	 * @param typeChecker
	 * 		For hierarchy look-ups.
	 *
	 * @return {@code true} when the merge changed the stack or local states.
	 * {@code false} if no changes were made after the merge was completed.
	 *
	 * @throws FrameMergeException
	 * 		When the frames cannot be merged for some reason.
	 * 		Exception message will elaborate on exact cause.
	 */
	public boolean merge(Frame otherFrame, InheritanceChecker typeChecker) throws FrameMergeException {
		wonky |= otherFrame.isWonky();
		if (visited) {
			boolean modified = false;
			for (Map.Entry<String, Value> e : locals.entrySet()) {
				String name = e.getKey();
				Value value = e.getValue();
				Value otherValue = otherFrame.getLocal(name);
				if (otherValue != null) {
					// If the values between this frame and the other do not match, we need to merge.
					boolean valuesDifferAfterMerge = !value.equals(otherValue);
					if (valuesDifferAfterMerge) {
						Value newValue = mergeValue(value, otherValue, typeChecker);
						setLocal(name, newValue);
						modified = true;
					}
				}
			}
			int max = getStack().size();
			int otherMax = otherFrame.getStack().size();
			if (max != otherMax) {
				throw new FrameMergeException("Unmatched stack size during AST frame merge, " + max + " != " + otherMax);
			}
			for (int i = 0; i < max; i++) {
				Value value = getStack().get(i);
				Value otherValue = otherFrame.getStack().get(i);
				// If the values between this frame and the other do not match, we need to merge.
				boolean valuesDifferAfterMerge = !value.equals(otherValue);
				if (valuesDifferAfterMerge) {
					Value newValue = mergeValue(value, otherValue, typeChecker);
					getStack().set(i, newValue);
					modified = true;
				}
			}
			return modified;
		}
		return false;
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
				Type commonType = Type.getObjectType(common);
				// Update the local with the new type
				return new Value.TypeValue(commonType);
			} else {
				// TODO: Check
				//    LDC Type
				//    GOTO D
				//  C:
				//    ACONST_NULL
				//    GOTO D
				//  D:
				//    ASTORE x
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
			} else if (otherValue instanceof Value.ArrayValue) {
				return new Value.ObjectValue(Types.OBJECT_TYPE);
			} else if (otherValue instanceof Value.NullValue) {
				return value;
			} else {
				throw new FrameMergeException("Values not objects/arrays in both frames!");
			}
		} else if (value instanceof Value.NumericValue) {
			if (otherValue instanceof Value.NumericValue) {
				Value.NumericValue numeric = (Value.NumericValue) value;
				Value.NumericValue otherNumeric = (Value.NumericValue) otherValue;
				// Select widest type
				Type widest = NumberUtil.getWidestType(numeric.getType(), otherNumeric.getType());
				if (Objects.equals(numeric.getNumber(), otherNumeric.getNumber())) {
					numeric = new Value.NumericValue(widest, otherNumeric.getNumber());
				} else {
					numeric = new Value.NumericValue(widest);
				}
				return numeric;
			} else {
				throw new FrameMergeException("Values not numeric in both frames!");
			}
		} else if (value instanceof Value.ArrayValue) {
			if (otherValue instanceof Value.ArrayValue) {
				Value.ArrayValue array = (Value.ArrayValue) value;
				Value.ArrayValue otherArray = (Value.ArrayValue) otherValue;
				// Merge array types
				Type arrayType = array.getArrayType();
				Type otherArrayType = otherArray.getArrayType();
				if (Types.isPrimitive(arrayType)) {
					Type widest = arrayType.getSort() > otherArrayType.getSort() ? arrayType : otherArrayType;
					return new Value.ArrayValue(array.getDimensions(), widest);
				} else {
					String common = typeChecker.getCommonType(
							arrayType.getInternalName(),
							otherArray.getElementType().getInternalName());
					Type commonType = Type.getObjectType(common);
					return new Value.ArrayValue(array.getDimensions(), commonType);
				}
			} else if (otherValue instanceof Value.ObjectValue) {
				return new Value.ObjectValue(Types.OBJECT_TYPE);
			} else if (otherValue instanceof Value.NullValue) {
				return new Value.ObjectValue(Types.OBJECT_TYPE);
			} else {
				throw new FrameMergeException("Values not arrays/objects in both frames!");
			}
		} else if (value instanceof Value.NullValue) {
			if (otherValue instanceof Value.ObjectValue) {
				return otherValue;
			} else if (otherValue instanceof Value.ArrayValue) {
				return new Value.ObjectValue(Types.OBJECT_TYPE);
			} else if (otherValue instanceof Value.NullValue) {
				return value;
			} else {
				throw new FrameMergeException("Values not objects/arrays in both frames!");
			}
		}
		return value;
	}

	/**
	 * @return Copy of the current frame.
	 */
	public Frame copy() {
		Frame copy = new Frame();
		copy.copy(this);
		return copy;
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
		// TODO: Should we copy other properties?
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
		return peek(0);
	}

	/**
	 * @param offset
	 * 		Offset from the top of the stack.
	 *
	 * @return Top offset value from the stack.
	 */
	public Value peek(int offset) {
		if (stack.isEmpty()) {
			markWonky("Cannot peek off empty stack!");
			return new Value.EmptyPoppedValue();
		}
		int index = stack.size() - (1 + offset);
		if (index < 0) {
			markWonky("Cannot peek offset(" + offset + ") from stack top!");
			return new Value.EmptyPoppedValue();
		}
		return stack.get(index);
	}

	/**
	 * @return Top value from the stack.
	 */
	public Value pop() {
		if (stack.isEmpty()) {
			markWonky("Cannot pop off empty stack!");
			return new Value.EmptyPoppedValue();
		}
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
	 * @return Reason for the frame being marked as {@link #isWonky()}.
	 */
	public String getWonkyReason() {
		return wonkyReason;
	}

	/**
	 * See {@link #isWonky()}.
	 */
	public void markWonky(String reason) {
		wonky = true;
		wonkyReason = reason;
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

	@Override
	public String toString() {
		return "Frame{" +
				"locals=" + locals +
				", stack=" + stack +
				'}';
	}
}
