package me.coley.recaf.graph.flow;

import me.coley.recaf.Recaf;
import org.objectweb.asm.*;

import java.util.*;

/**
 * ClassVisitor that collects outbound calls from a given host method.
 *
 * @author Matt
 */
public class OutboundCollector extends ClassVisitor {
	private final FlowGraph graph;
	// Host method definition
	private final String hostName;
	private final String hostDesc;
	// Collection of calls, using a insertion-order set for ordered iteration in later usages.
	private final Set<FlowReference> outbound = new LinkedHashSet<>();

	/**
	 * Constructs an outbound method collector.
	 *
	 * @param graph
	 * 		Graph to pull data from.
	 * @param hostName
	 * 		Name of method to search for outbound calls in.
	 * @param hostDesc
	 * 		Descriptor of method to search for outbound calls in.
	 */
	public OutboundCollector(FlowGraph graph, String hostName, String hostDesc) {
		super(Recaf.ASM_VERSION);
		this.graph = graph;
		this.hostName = hostName;
		this.hostDesc = hostDesc;
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] exc) {
		// Only visit/parse if the visited method is the host method
		if (name.equals(hostName) && desc.equals(hostDesc))
			return new MethodVisitor(api) {
				@Override
				public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
					// Get vertex containing the method & add reference
					FlowVertex vertex = graph.getVertex(owner, name, desc);
					// If the vertex cannot be loaded it's assumed to be a core class.
					// If the user has their workspace setup (with libraries) this will be
					// the only null case.
					if (vertex != null)
						outbound.add(new FlowReference(vertex, name, desc));
				}
			};
		return null;
	}

	/**
	 * @return Set of outbound method calls from the current host method.
	 */
	public Set<FlowReference> getOutbound() {
		return outbound;
	}
}
