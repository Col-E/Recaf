package me.coley.recaf.mapping;

import com.google.common.collect.Sets;
import me.coley.recaf.graph.flow.*;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

	public void analyze() {
		Set<FlowVertex> baseEntryPoints = getEntryPoints(base);
		Set<FlowVertex> targetEntryPoints = getEntryPoints(target);
		if (baseEntryPoints.size() != targetEntryPoints.size()) {
			return;
		}

		Function<FlowVertex, FlowBuilder> mapper = vertex -> {
			FlowBuilder builder = new FlowBuilder();
			builder.build(vertex);
			return builder;
		};
		Map<FlowVertex, FlowBuilder> baseBuilders = baseEntryPoints.stream()
				.collect(Collectors.toMap(identity(), mapper));
		Map<FlowVertex, FlowBuilder> targetBuilders = targetEntryPoints.stream()
				.collect(Collectors.toMap(identity(), mapper));
		//
		Set<FlowBuilder.GeneralVertex> a = baseBuilders.values().stream()
				.flatMap(builder -> builder.getVertices().values().stream())
				.collect(Collectors.toSet());
		Set<FlowBuilder.GeneralVertex> b = targetBuilders.values().stream()
				.flatMap(builder -> builder.getVertices().values().stream())
				.collect(Collectors.toSet());
		// TODO: Fix equality algorithms used.
		System.out.println(a);
		System.out.println(b);
		System.out.println();
		//
		Sets.SetView<FlowBuilder.GeneralVertex> diff = Sets.difference(a, b);
		if(diff.isEmpty()) {
			System.out.println("SAME");
		} else {
			System.out.println(diff.stream().sorted().collect(Collectors.toList()));
		}
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
		AtomicBoolean contains = new AtomicBoolean(false);
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM7) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
											 String signature, String[] exceptions) {
				if(name.equals("main") && descriptor.equals("([Ljava/lang/String;)V"))
					contains.set(true);
				return null;
			}
		};
		reader.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return contains.get();
	}
}
