package me.coley.recaf.mapping;

import me.coley.recaf.Recaf;
import me.coley.recaf.graph.flow.*;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Utility for analyzing the similarity between two resources.
 *
 * @author Matt
 */
public class Correlation {
	private final Workspace workspace;
	private final JavaResource base;
	private final JavaResource target;

	/**
	 * Constructs a correlation mapper between two resources.
	 *
	 * @param workspace
	 *  The workspace containing the resources.
	 * @param base
	 * 		The base comparison resource.
	 * @param target
	 * 		The target resource analyzed for similarities against the base.
	 */
	public Correlation(Workspace workspace, JavaResource base, JavaResource target) {
		this.workspace = workspace;
		this.base = base;
		this.target = target;
	}

	/**
	 * @return Set of {@link me.coley.recaf.mapping.CorrelationResult} for each common entry point
	 * <i>(Main method)</i> discovered among the base and target sources.
	 */
	public Set<CorrelationResult> analyze() {
		// Collect entry points "main(String[])"
		Set<FlowVertex> baseEntryPoints = getEntryPoints(base);
		Set<FlowVertex> targetEntryPoints = getEntryPoints(target);
		if (baseEntryPoints.size() != targetEntryPoints.size())
			throw new IllegalArgumentException("Base and target resources must have the same number " +
					"of entry points! Found " + baseEntryPoints.size() + " and " +
					targetEntryPoints.size() + " respectively");
		if (baseEntryPoints.isEmpty())
			throw new IllegalArgumentException("No entry points found in either resource!");
		return analyze(baseEntryPoints, targetEntryPoints);
	}

	/**
	 * @param baseEntry
	 * 		Entry point in the base resource.
	 * @param targetEntry
	 * 		Entry point in the target resource, assumed to outline the same data flow.
	 *
	 * @return {@link me.coley.recaf.mapping.CorrelationResult} for the common entry point given.
	 */
	public CorrelationResult analyze(FlowVertex baseEntry, FlowVertex targetEntry) {
		Set<CorrelationResult> results = analyze(Collections.singleton(baseEntry),
				Collections.singleton(targetEntry));
		if (results.size() != 1)
			throw new IllegalStateException("Analyzed a single entry point, but returned "
					+ results.size() + " results");
		return results.iterator().next();
	}

	/**
	 * @param baseEntries
	 * 		Entry points in the base resource.
	 * @param targetEntries
	 * 		Entry points in the target resource, assumed to outline the same data flow.
	 *
	 * @return Set of {@link me.coley.recaf.mapping.CorrelationResult} for each common entry point.
	 */
	public Set<CorrelationResult> analyze(Set<FlowVertex> baseEntries, Set<FlowVertex> targetEntries) {
		// Map CFG vertex to simplified vertex builder
		Function<FlowVertex, FlowBuilder> mapper = vertex -> {
			FlowBuilder builder = new FlowBuilder();
			builder.build(vertex);
			return builder;
		};
		// Collect simplified, non-generative graph layouts
		List<FlowBuilder.Flow> baseRoots = baseEntries.stream()
				.collect(Collectors.toMap(identity(), mapper))
				.entrySet().stream()
				.map(e -> e.getValue().getVertices().get(e.getKey().toString()))
				.collect(Collectors.toList());
		List<FlowBuilder.Flow> targetRoots = targetEntries.stream()
				.collect(Collectors.toMap(identity(), mapper))
				.entrySet().stream()
				.map(e -> e.getValue().getVertices().get(e.getKey().toString()))
				.collect(Collectors.toList());
		Collections.sort(baseRoots);
		Collections.sort(targetRoots);
		// Compare and collect results
		Set<CorrelationResult> results = new HashSet<>();
		for(int i = 0; i < baseRoots.size(); i++) {
			FlowBuilder.Flow baseFlow = baseRoots.get(i);
			FlowBuilder.Flow targetFlow = targetRoots.get(i);
			baseFlow.sort();
			targetFlow.sort();
			// Difference on demand in result structure
			results.add(new CorrelationResult(workspace, base, target, baseFlow, targetFlow));
		}
		return results;
	}

	private Set<FlowVertex> getEntryPoints(JavaResource resource) {
		FlowGraph flow = workspace.getFlowGraph();
		Function<ClassReader, FlowVertex> readerToVert =
				reader -> flow.getVertex(reader, "main", "([Ljava/lang/String;)V");
		return resource.getClasses().values().stream()
				.map(ClassReader::new)
				.filter(Correlation::containsEntry)
				.map(readerToVert)
				.collect(Collectors.toSet());
	}

	private static boolean containsEntry(ClassReader reader) {
		boolean[] contains = {false};
		ClassVisitor cv = new ClassVisitor(Recaf.ASM_VERSION) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
											 String signature, String[] exceptions) {
				if(name.equals("main") && descriptor.equals("([Ljava/lang/String;)V"))
					contains[0] = true;
				return null;
			}
		};
		reader.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return contains[0];
	}
}
