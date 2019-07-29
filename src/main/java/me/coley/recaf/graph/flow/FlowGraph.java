package me.coley.recaf.graph.flow;

import me.coley.recaf.graph.WorkspaceGraph;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;

/**
 * Graph model to represent the method call graph starting at some specified method <i>(Modled by
 * {@link me.coley.recaf.graph.flow.FlowVertex})</i>.
 *
 * @author Matt
 */
public class FlowGraph extends WorkspaceGraph<FlowVertex> {
	/**
	 * Constructs a flow graph from the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public FlowGraph(Workspace workspace) {
		super(workspace);
	}

	/**
	 * @param owner
	 * 		Class name.
	 * @param name
	 * 		Name of method in class.
	 * @param descriptor
	 * 		Descriptor of method in class.
	 *
	 * @return FlowVertex outlining the given method.
	 */
	public FlowVertex getVertex(String owner, String name, String descriptor) {
		if(getWorkspace().hasClass(owner)) {
			ClassReader reader = getWorkspace().getClassReader(owner);
			return getVertex(reader, name, descriptor);
		}
		return null;
	}

	/**
	 * @param reader
	 * 		Class reader of the class containing the given method.
	 * @param name
	 * 		Name of method in class.
	 * @param descriptor
	 * 		Descriptor of method in class.
	 *
	 * @return FlowVertex outlining the given method.
	 */
	public FlowVertex getVertex(ClassReader reader, String name, String descriptor) {
		return new FlowVertex(this, reader, name, descriptor);
	}

	@Override
	public FlowVertex getVertex(ClassReader key) {
		throw new UnsupportedOperationException("'getVertex' is not supported by FlowGraph, see documentation");
	}
}
