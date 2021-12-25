package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Snapshot of computed local/stack data.
 *
 * @author Matt Coley
 * @see Edge
 * @see Value
 */
public class Frame {
	private final Map<String, Value> locals = new HashMap<>();
	private final List<Value> stack = new ArrayList<>();
	private final List<Edge> edges = new ArrayList<>();

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
	 * Adds an edge between the current frame to a given frame of a handler block start label.
	 *
	 * @param handlerFrame
	 * 		Target frame of a handler block.
	 */
	public void addHandlerEdge(Frame handlerFrame) {
		Edge edge = new Edge(this, handlerFrame, EdgeType.EXCEPTION_HANDLER);
		edges.add(edge);
		handlerFrame.edges.add(edge);
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

	/**
	 * @return All edges to and from this frame.
	 */
	public List<Edge> getEdges() {
		return edges;
	}

	/**
	 * @return Edges where the flow source is this frame.
	 */
	public List<Edge> getOutboundEdges() {
		return getEdges().stream()
				.filter(e -> e.getFrom() == this)
				.collect(Collectors.toList());
	}

	/**
	 * @return Edges where the flow source is another frame.
	 */
	public List<Edge> getInboundEdges() {
		return getEdges().stream()
				.filter(e -> e.getTo() == this)
				.collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Frame frame = (Frame) o;
		return locals.equals(frame.locals) &&
				stack.equals(frame.stack) &&
				edges.equals(frame.edges);
	}

	@Override
	public int hashCode() {
		return Objects.hash(locals, stack, edges);
	}
}
