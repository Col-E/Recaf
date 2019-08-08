package me.coley.recaf.mapping;

import me.coley.recaf.graph.flow.FlowBuilder;
import me.coley.recaf.graph.flow.FlowVertex;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import me.coley.recaf.graph.inheritance.HierarchyVertex;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;

import java.util.*;

/**
 * Correlation analysis result for some entry point. The {@link #getBase() base} represents the
 * entry point in the reference program. The {@link #getTarget() target} represents the entry point in
 * another program, assumed to be the same but with renamed identifiers.
 *
 * @author Matt
 */
public class CorrelationResult {
	private final Workspace workspace;
	private final JavaResource baseResource;
	private final JavaResource targetResource;
	private final FlowBuilder.Flow base;
	private final FlowBuilder.Flow target;
	private Set<FlowBuilder.Flow> difference;

	/**
	 * Constructs a correlation result.
	 *
	 * @param workspace
	 * 		The workspace to reference.
	 * @param baseResource
	 * 		Resource for base references.
	 * @param targetResource
	 * 		Resource for target references.
	 * @param base
	 * 		Simplified flow graph of an entry point in the base resource.
	 * @param target
	 * 		Simplfiied flow graph of an entry point in the target resource.
	 */
	public CorrelationResult(Workspace workspace, JavaResource baseResource, JavaResource targetResource,
							 FlowBuilder.Flow base, FlowBuilder.Flow target) {
		this.workspace = workspace;
		this.baseResource = baseResource;
		this.targetResource = targetResource;
		this.base = base;
		this.target = target;
	}

	/**
	 * @return Simplified flow graph of an entry point in the target resource.
	 */
	public FlowBuilder.Flow getBase() {
		return base;
	}

	/**
	 * @return Simplified flow graph of an entry point in the target resource.
	 */
	public FlowBuilder.Flow getTarget() {
		return target;
	}

	/**
	 * @return The set of vertices attached to the base flow that do not have
	 * mappings to the vertices connected to the target flow. An empty set indicates
	 * the flow vertices model the same structure / call-graph.
	 */
	public Set<FlowBuilder.Flow> getDifference() {
		if (difference == null)
			difference = base.getDifference(target);
		return difference;
	}

	/**
	 * Generates ASM formatted mappings based on the assumed equality of control flow among the
	 * {@link #getBase() base} and {@link #getTarget() target} call graphs.
	 * <br>
	 * See the
	 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)} docs for more
	 * information.
	 *
	 * @return ASM formatted mappings.
	 */
	public Map<String, String> getMappings() {
		Map<String, String> map = new HashMap<>();
		map(map, getDifference(), getBase(), getTarget());
		return map;
	}

	private void map(Map<String, String> map, Set<FlowBuilder.Flow> diff,
					 FlowBuilder.Flow baseFlow, FlowBuilder.Flow targetFlow) {
		// Abandon if the base flow is in the difference set.
		// All further mappings cannot be trusted, so we return.
		if(diff.contains(baseFlow))
			return;
		// base values
		FlowVertex baseVal = baseFlow.getValue();
		String baseOwner = baseVal.getOwner();
		String baseName = baseVal.getName();
		// target values
		FlowVertex targetVal = targetFlow.getValue();
		String targetOwner = targetVal.getOwner();
		String targetName = targetVal.getName();
		String targetDesc = targetVal.getDesc();
		// generate mappings
		if (!baseOwner.equals(targetOwner))
			map.put(targetOwner, baseOwner);
		if(!baseName.equals(targetName)) {
			// Update the entire hierarchy if the entry does not exist
			String key = targetOwner + "." + targetName + targetDesc;
			if(!map.containsKey(key)) {
				HierarchyGraph hierarchyGraph = workspace.getHierarchyGraph();
				// Creating the classloader from the resource instead of the workspace is INTENTIONAL
				// For overlapping names the workspace will always defer to the primary resource.
				HierarchyVertex targetVert = hierarchyGraph.getVertex(
						new ClassReader(targetResource.getClasses().get(targetOwner)));
				Set<HierarchyVertex> hierarchy = hierarchyGraph.getHierarchy(targetVert);
				for(HierarchyVertex vertex : hierarchy) {
					if(!ClassUtil.containsMethod(vertex.getData(), targetName, targetDesc))
						continue;
					map.put(vertex.getData().getClassName() + "." + targetName + targetDesc, baseName);
				}
			}
		}
		// TODO: since we're sure the flows/classes are the same why not just map out all properties?
		/*
		// TODO: generate additional mappings from matching method descriptors
		Type baseType = Type.getMethodType(baseDesc);
		Type targetType = Type.getMethodType(targetDesc);
		if (baseType.getReturnType().getSort() == Type.OBJECT) {
			map.put(baseType.getReturnType().getInternalName(),
					targetType.getReturnType().getInternalName());
		}
		*/
		// map children in flow
		for (int i = 0; i < baseFlow.getChildren().size(); i++) {
			map(map, diff, baseFlow.getChildren().get(i), targetFlow.getChildren().get(i));
		}
	}
}