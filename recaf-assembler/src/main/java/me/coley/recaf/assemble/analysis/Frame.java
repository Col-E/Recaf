package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.util.AccessFlag;
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
			locals.put("this", new Value.ObjectValue(selfTypeName));
		}
		// Add parameters
		for (MethodParameter parameter : definition.getParams()) {
			setLocal(parameter.getName(), parameter.getDesc());
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
	 * @param desc
	 * 		Variable type.
	 */
	public void setLocal(String name, String desc) {
		Type paramType = Type.getType(desc);
		String paramTypeName = paramType.getInternalName();
		locals.put(name, new Value.ObjectValue(paramTypeName));
	}

	/**
	 * @return Local variables by name.
	 */
	public Map<String, Value> getLocals() {
		return locals;
	}

	/**
	 * @return Stack values.
	 */
	public List<Value> getStack() {
		return stack;
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
