package software.coley.recaf.services.analysis.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.EnumConstant;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.constant.MemberConstant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.visitor.DexClassVisitor;
import me.darknet.dex.tree.visitor.DexCodeVisitor;
import me.darknet.dex.tree.visitor.DexConstantVisitor;
import me.darknet.dex.tree.visitor.DexMethodVisitor;
import me.darknet.dex.tree.visitor.DexTreeWalker;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.annotation.AnnotationEnumReference;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.analysis.entry.EntryAnalysisService;
import software.coley.recaf.services.search.similarity.PackagePurpose;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Service for grouping classes into logical application areas.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AreaAnalysisService implements Service {
	public static final String SERVICE_ID = "area-analysis";

	// Weights for reference kinds, a bit arbitrary based on some random samples with what ended up working well.
	private static final int WEIGHT_METHOD_CALL = 5;
	private static final int WEIGHT_METHOD_CALL_SYNTHETIC = 2;
	private static final int WEIGHT_INHERITANCE = 4;
	private static final int WEIGHT_FIELD_TYPE = 3;
	private static final int WEIGHT_FIELD_ACCESS = 2;
	private static final int WEIGHT_METHOD_TYPE = 3;
	private static final int WEIGHT_THROWN_TYPE = 2;
	private static final int WEIGHT_ANNOTATION = 1;
	private static final int WEIGHT_TYPE_INSTRUCTION = 1;
	private static final int WEIGHT_BOOTSTRAP = 2;
	private static final int WEIGHT_INNER_CLASS = 4;
	private static final int WEIGHT_SIGNATURE = 2;

	// Weights for merging components into groups, also a bit arbitrary based on some random samples.
	private static final int MERGE_MIN_EDGE_WEIGHT = 3;
	private static final int MERGE_OUTGOING_LEEWAY = 2;
	private static final int MERGE_INCOMING_DOMINANCE_PERCENT = 65;

	private final AreaAnalysisConfig config;
	private final EntryAnalysisService entryAnalysisService;

	@Inject
	public AreaAnalysisService(@Nonnull AreaAnalysisConfig config,
	                           @Nonnull EntryAnalysisService entryAnalysisService) {
		this.config = config;
		this.entryAnalysisService = entryAnalysisService;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource scope to analyze.
	 *
	 * @return Area analysis result for the given resource.
	 */
	@Nonnull
	public AreaAnalysisResult analyze(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		AreaGraph graph = new AreaGraph();

		// Collect all classes in the resource scope, including embedded resources.
		collectScopedClasses(workspace, resource, graph);

		// If no classes were found, return an empty result with all classes ungrouped.
		if (graph.nodes.isEmpty()) {
			List<ClassPathNode> ungrouped = graph.ungroupedClasses.stream()
					.sorted(Comparator.comparing(path -> path.getValue().getName()))
					.toList();
			return new AreaAnalysisResult(List.of(), List.of(), ungrouped, false, 0, 0, 0);
		}

		// Mark entry point classes based on flow analysis results.
		entryAnalysisService.findEntryPoints(workspace, resource).stream()
				.map(entry -> entry.classPath().getValue().getName())
				.distinct()
				.forEach(name -> {
					Node node = graph.nodes.get(name);
					if (node != null)
						node.containsEntryPoint = true;
				});

		// Scan class structure + method code to populate node content (cross-references,
		// weighted references by purpose, etc.) in the graph.
		scanDependencies(graph);

		// Compute strongly connected components (SCCs) to find tightly coupled classes that should be grouped together.
		List<Component> initialComponents = new Tarjan(graph).compute();

		// We have our own merge heuristic that considers reference weights and user-requested merge settings.
		// The gist is we can merge two strong components if they have a strong connection.
		// This is not typical of SCC-based grouping, but still provides good results in practice and reduces small group clutter.
		MergeState mergeState = buildMergeState(initialComponents, graph);
		mergeComponents(mergeState);
		List<MergedGroup> orderedGroups = mergeState.orderedGroups();

		// Build links between groups based on inter-group references.
		Map<String, Integer> rootToGroupId = new HashMap<>();
		for (int i = 0; i < orderedGroups.size(); i++)
			rootToGroupId.put(orderedGroups.get(i).rootName, i + 1);
		List<AreaLink> links = buildLinks(orderedGroups, rootToGroupId);

		// Detect if we have a "spaghetti" area that dominates the resource.
		// In practice this comes up when you have a large framework or utility area that is heavily referenced by many smaller areas.
		boolean spaghettiDetected = isSpaghettiDetected(orderedGroups, graph.nodes.size());
		String dominantRoot = spaghettiDetected ? dominantGroupRoot(orderedGroups) : null;

		// Build final groups with all the collected information, including group purpose estimation and confidence scoring.
		List<AreaGroup> groups = buildPublicGroups(orderedGroups, rootToGroupId, spaghettiDetected, dominantRoot);
		List<ClassPathNode> ungrouped = graph.ungroupedClasses.stream()
				.sorted(Comparator.comparing(path -> path.getValue().getName()))
				.toList();
		return new AreaAnalysisResult(groups, links, ungrouped, spaghettiDetected, graph.nodes.size(), groups.size(), links.size());
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AreaAnalysisConfig getServiceConfig() {
		return config;
	}

	/**
	 * Collects all classes in the resource, including any embedded resources, and adds them as nodes to the graph.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to read classes from.
	 * @param graph
	 * 		Graph to populate.
	 */
	private void collectScopedClasses(@Nonnull Workspace workspace,
	                                  @Nonnull WorkspaceResource resource,
	                                  @Nonnull AreaGraph graph) {
		ArrayDeque<WorkspaceResource> queue = new ArrayDeque<>();
		queue.add(resource);
		while (!queue.isEmpty()) {
			WorkspaceResource current = queue.removeFirst();
			for (JvmClassBundle bundle : current.jvmClassBundleStream().toList())
				collectBundleClasses(workspace, current, bundle, graph);
			for (VersionedJvmClassBundle bundle : current.versionedJvmClassBundleStream().toList())
				collectBundleClasses(workspace, current, bundle, graph);
			for (AndroidClassBundle bundle : current.androidClassBundleStream().toList())
				collectBundleClasses(workspace, current, bundle, graph);
			queue.addAll(current.getEmbeddedResources().values());
		}
	}

	/**
	 * Collects all classes in the bundle and adds them as nodes to the graph.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Bundle to read classes from.
	 * @param graph
	 * 		Graph to populate.
	 */
	private void collectBundleClasses(@Nonnull Workspace workspace,
	                                  @Nonnull WorkspaceResource resource,
	                                  @Nonnull Bundle<? extends ClassInfo> bundle,
	                                  @Nonnull AreaGraph graph) {
		for (ClassInfo classInfo : bundle.values()) {
			ClassPathNode classPath = PathNodes.classPath(workspace, resource, bundle, classInfo);

			// Modules, bruh...
			if (classInfo.hasModuleModifier() || "module-info".equals(classInfo.getName())) {
				graph.ungroupedClasses.add(classPath);
				continue;
			}

			graph.nodes.putIfAbsent(classInfo.getName(), new Node(classPath));
		}
	}

	/**
	 * Main metadata + reference scan.
	 *
	 * @param graph
	 * 		Graph to infill.
	 *
	 * @see #scanClassMetadata(AreaGraph, Node, ClassInfo)
	 * @see #scanJvmCode(AreaGraph, Node, JvmClassInfo)
	 * @see #scanAndroidCode(AreaGraph, Node, AndroidClassInfo)
	 */
	private void scanDependencies(@Nonnull AreaGraph graph) {
		for (Node node : graph.nodes.values()) {
			ClassInfo classInfo = node.path.getValue();
			scanClassMetadata(graph, node, classInfo);
			if (classInfo.isJvmClass()) {
				scanJvmCode(graph, node, classInfo.asJvmClass());
			} else if (classInfo.isAndroidClass()) {
				scanAndroidCode(graph, node, classInfo.asAndroidClass());
			}
		}
	}

	/**
	 * Scans for class structure references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param classInfo
	 * 		Class model being scanned.
	 */
	private void scanClassMetadata(@Nonnull AreaGraph graph, @Nonnull Node node, @Nonnull ClassInfo classInfo) {
		// Inherit
		addReference(graph, node, classInfo.getSuperName(), WEIGHT_INHERITANCE);
		for (String interfaceName : classInfo.getInterfaces())
			addReference(graph, node, interfaceName, WEIGHT_INHERITANCE);

		// Signatures and annotations
		addSignatureReferences(graph, node, classInfo.getSignature(), Types.SignatureContext.CLASS, WEIGHT_SIGNATURE);
		addAnnotationReferences(graph, node, classInfo, WEIGHT_ANNOTATION);

		// Outer + inner classes
		String outerClassName = classInfo.getOuterClassName();
		if (outerClassName != null) {
			addReference(graph, node, outerClassName, WEIGHT_INNER_CLASS);
			addReverseReference(graph, outerClassName, classInfo.getName(), WEIGHT_INNER_CLASS);
		}
		for (InnerClassInfo innerClass : classInfo.getInnerClasses()) {
			if (!innerClass.isExternalReference()) {
				addReference(graph, node, innerClass.getInnerClassName(), WEIGHT_INNER_CLASS);
				addReverseReference(graph, innerClass.getInnerClassName(), classInfo.getName(), WEIGHT_INNER_CLASS);
			}
		}

		// Fields
		for (FieldMember field : classInfo.getFields()) {
			addTypeDescriptorReferences(graph, node, field.getDescriptor(), WEIGHT_FIELD_TYPE);
			addSignatureReferences(graph, node, field.getSignature(), Types.SignatureContext.FIELD, WEIGHT_SIGNATURE);
			addAnnotationReferences(graph, node, field, WEIGHT_ANNOTATION);
		}

		// Methods
		for (MethodMember method : classInfo.getMethods()) {
			addMethodDescriptorReferences(graph, node, method.getDescriptor(), WEIGHT_METHOD_TYPE);
			addSignatureReferences(graph, node, method.getSignature(), Types.SignatureContext.METHOD, WEIGHT_SIGNATURE);
			for (String thrownType : method.getThrownTypes())
				addReference(graph, node, thrownType, WEIGHT_THROWN_TYPE);
			addAnnotationReferences(graph, node, method, WEIGHT_ANNOTATION);
		}
	}

	/**
	 * Scans method code for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param classInfo
	 * 		Class model being scanned.
	 */
	private void scanJvmCode(@Nonnull AreaGraph graph, @Nonnull Node node, @Nonnull JvmClassInfo classInfo) {
		classInfo.getClassReader().accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodMember method = classInfo.getDeclaredMethod(name, descriptor);
				int callWeight = method != null && method.isCompilerGenerated() ? WEIGHT_METHOD_CALL_SYNTHETIC : WEIGHT_METHOD_CALL;
				return new MethodVisitor(RecafConstants.getAsmVersion()) {
					@Override
					public void visitTypeInsn(int opcode, String type) {
						addReference(graph, node, normalizeInternalName(type), WEIGHT_TYPE_INSTRUCTION);
						super.visitTypeInsn(opcode, type);
					}

					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						addReference(graph, node, normalizeInternalName(owner), WEIGHT_FIELD_ACCESS);
						addTypeDescriptorReferences(graph, node, descriptor, WEIGHT_FIELD_TYPE);
						super.visitFieldInsn(opcode, owner, name, descriptor);
					}

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						addReference(graph, node, normalizeInternalName(owner), callWeight);
						addMethodDescriptorReferences(graph, node, descriptor, WEIGHT_METHOD_TYPE);
						super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
					}

					@Override
					public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						addMethodDescriptorReferences(graph, node, descriptor, WEIGHT_METHOD_TYPE);
						addHandleReferences(graph, node, bootstrapMethodHandle, WEIGHT_BOOTSTRAP);
						for (Object argument : bootstrapMethodArguments)
							addJvmConstantReferences(graph, node, argument, WEIGHT_BOOTSTRAP);
						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
					}

					@Override
					public void visitLdcInsn(Object value) {
						addJvmConstantReferences(graph, node, value, WEIGHT_TYPE_INSTRUCTION);
						super.visitLdcInsn(value);
					}

					@Override
					public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
						addTypeDescriptorReferences(graph, node, descriptor, WEIGHT_TYPE_INSTRUCTION);
						super.visitMultiANewArrayInsn(descriptor, numDimensions);
					}

					@Override
					public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
						addReference(graph, node, type, WEIGHT_THROWN_TYPE);
						super.visitTryCatchBlock(start, end, handler, type);
					}
				};
			}
		}, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
	}

	/**
	 * Scans method code for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param classInfo
	 * 		Class model being scanned.
	 */
	private void scanAndroidCode(@Nonnull AreaGraph graph, @Nonnull Node node, @Nonnull AndroidClassInfo classInfo) {
		DexTreeWalker.accept(classInfo.getBackingDefinition(), new DexClassVisitor() {
			@Nonnull
			@Override
			public DexMethodVisitor visitMethod(@Nonnull me.darknet.dex.tree.definitions.MethodMember method) {
				MethodMember declaredMethod = classInfo.getDeclaredMethod(method.getName(), method.getType().descriptor());
				int callWeight = declaredMethod != null && declaredMethod.isCompilerGenerated() ?
						WEIGHT_METHOD_CALL_SYNTHETIC : WEIGHT_METHOD_CALL;
				return new DexMethodVisitor() {
					@Nonnull
					@Override
					public DexCodeVisitor visitCode(@Nonnull Code code) {
						return new DexCodeVisitor() {
							@Override
							public void visitInvokeInstruction(@Nonnull InvokeInstruction instruction) {
								addReference(graph, node, normalizeInternalName(instruction.owner().internalName()), callWeight);
								addMethodDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_METHOD_TYPE);
								super.visitInvokeInstruction(instruction);
							}

							@Override
							public void visitInvokeCustomInstruction(@Nonnull InvokeCustomInstruction instruction) {
								addMethodDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_METHOD_TYPE);
								addDexHandleReferences(graph, node, instruction.handle(), WEIGHT_BOOTSTRAP);
								super.visitInvokeCustomInstruction(instruction);
							}

							@Nonnull
							@Override
							public DexConstantVisitor visitBootstrapArgument(@Nonnull InvokeCustomInstruction instruction,
							                                                 int index,
							                                                 @Nonnull Constant argument) {
								addDexConstantReferences(graph, node, argument, WEIGHT_BOOTSTRAP);
								return super.visitBootstrapArgument(instruction, index, argument);
							}

							@Override
							public void visitInstanceFieldInstruction(@Nonnull InstanceFieldInstruction instruction) {
								addReference(graph, node, normalizeInternalName(instruction.owner().internalName()), WEIGHT_FIELD_ACCESS);
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_FIELD_TYPE);
								super.visitInstanceFieldInstruction(instruction);
							}

							@Override
							public void visitStaticFieldInstruction(@Nonnull StaticFieldInstruction instruction) {
								addReference(graph, node, normalizeInternalName(instruction.owner().internalName()), WEIGHT_FIELD_ACCESS);
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_FIELD_TYPE);
								super.visitStaticFieldInstruction(instruction);
							}

							@Override
							public void visitCheckCastInstruction(@Nonnull CheckCastInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitCheckCastInstruction(instruction);
							}

							@Override
							public void visitInstanceOfInstruction(@Nonnull InstanceOfInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitInstanceOfInstruction(instruction);
							}

							@Override
							public void visitConstTypeInstruction(@Nonnull ConstTypeInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitConstTypeInstruction(instruction);
							}

							@Override
							public void visitConstMethodTypeInstruction(@Nonnull ConstMethodTypeInstruction instruction) {
								addMethodDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_METHOD_TYPE);
								super.visitConstMethodTypeInstruction(instruction);
							}

							@Override
							public void visitConstMethodHandleInstruction(@Nonnull ConstMethodHandleInstruction instruction) {
								addDexHandleReferences(graph, node, instruction.handle(), WEIGHT_BOOTSTRAP);
								super.visitConstMethodHandleInstruction(instruction);
							}

							@Override
							public void visitNewInstanceInstruction(@Nonnull NewInstanceInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.type().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitNewInstanceInstruction(instruction);
							}

							@Override
							public void visitNewArrayInstruction(@Nonnull NewArrayInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.componentType().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitNewArrayInstruction(instruction);
							}

							@Override
							public void visitFilledNewArrayInstruction(@Nonnull FilledNewArrayInstruction instruction) {
								addTypeDescriptorReferences(graph, node, instruction.componentType().descriptor(), WEIGHT_TYPE_INSTRUCTION);
								super.visitFilledNewArrayInstruction(instruction);
							}

							@Override
							public void visitTryCatchHandler(@Nonnull TryCatch tryCatch, @Nonnull Handler handler) {
								if (handler.exceptionType() != null)
									addReference(graph, node, normalizeInternalName(handler.exceptionType().internalName()), WEIGHT_THROWN_TYPE);
								super.visitTryCatchHandler(tryCatch, handler);
							}
						};
					}
				};
			}
		});
	}

	/**
	 * Scans annotated elements for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param annotated
	 * 		Annotated element to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addAnnotationReferences(@Nonnull AreaGraph graph,
	                                     @Nonnull Node node,
	                                     @Nonnull Annotated annotated,
	                                     int weight) {
		for (AnnotationInfo annotation : annotated.getAnnotations())
			addAnnotationInfoReferences(graph, node, annotation, weight);
		for (AnnotationInfo annotation : annotated.getTypeAnnotations())
			addAnnotationInfoReferences(graph, node, annotation, weight);
	}

	/**
	 * Scans an annotation for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param annotation
	 * 		Annotation to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addAnnotationInfoReferences(@Nonnull AreaGraph graph,
	                                         @Nonnull Node node,
	                                         @Nonnull AnnotationInfo annotation,
	                                         int weight) {
		addTypeDescriptorReferences(graph, node, annotation.getDescriptor(), weight);
		for (AnnotationElement element : annotation.getElements().values())
			addAnnotationElementValueReferences(graph, node, element.getElementValue(), weight);
		for (AnnotationInfo nested : annotation.getAnnotations())
			addAnnotationInfoReferences(graph, node, nested, weight);
		for (AnnotationInfo nested : annotation.getTypeAnnotations())
			addAnnotationInfoReferences(graph, node, nested, weight);
	}

	/**
	 * Scan an annotation element value for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param value
	 * 		Annotation element value to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addAnnotationElementValueReferences(@Nonnull AreaGraph graph,
	                                                 @Nonnull Node node,
	                                                 @Nonnull Object value,
	                                                 int weight) {
		switch (value) {
			case AnnotationInfo annotation -> addAnnotationInfoReferences(graph, node, annotation, weight);
			case AnnotationEnumReference enumReference ->
					addTypeDescriptorReferences(graph, node, enumReference.getDescriptor(), weight);
			case Type type -> addAsmTypeReferences(graph, node, type, weight);
			case List<?> values -> {
				for (Object nested : values)
					if (nested != null)
						addAnnotationElementValueReferences(graph, node, nested, weight);
			}
			default -> {
				// Primitive values and strings do not contribute to area analysis.
			}
		}
	}

	/**
	 * Scans a constant <i>(LDC instruction and INVOKEDYNAMIC bootstrap args)</i> for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param value
	 * 		Constant value to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addJvmConstantReferences(@Nonnull AreaGraph graph,
	                                      @Nonnull Node node,
	                                      @Nonnull Object value,
	                                      int weight) {
		switch (value) {
			case Type type -> addAsmTypeReferences(graph, node, type, weight);
			case Handle handle -> addHandleReferences(graph, node, handle, weight);
			case ConstantDynamic dynamic -> {
				addTypeDescriptorReferences(graph, node, dynamic.getDescriptor(), weight);
				addHandleReferences(graph, node, dynamic.getBootstrapMethod(), weight);
				for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++)
					addJvmConstantReferences(graph, node, dynamic.getBootstrapMethodArgument(i), weight);
			}
			default -> {
				// Primitive values and strings do not contribute to area analysis.
			}
		}
	}

	/**
	 * Scans a constant <i>(Dex constant instructions and dynamic method bootstrap args)</i> for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param constant
	 * 		Constant value to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addDexConstantReferences(@Nonnull AreaGraph graph,
	                                      @Nonnull Node node,
	                                      @Nonnull Constant constant,
	                                      int weight) {
		switch (constant) {
			case AnnotationConstant annotationConstant ->
					addDexAnnotationReferences(graph, node, annotationConstant.annotation(), weight);
			case ArrayConstant arrayConstant -> {
				for (Constant nested : arrayConstant.constants())
					addDexConstantReferences(graph, node, nested, weight);
			}
			case EnumConstant enumConstant ->
					addTypeDescriptorReferences(graph, node, enumConstant.field().descriptor(), weight);
			case HandleConstant handleConstant -> addDexHandleReferences(graph, node, handleConstant.handle(), weight);
			case MemberConstant memberConstant -> {
				addReference(graph, node, normalizeInternalName(memberConstant.owner().internalName()), weight);
				addTypeDescriptorReferences(graph, node, memberConstant.member().descriptor(), weight);
			}
			case TypeConstant typeConstant ->
					addTypeDescriptorReferences(graph, node, typeConstant.type().descriptor(), weight);
			default -> {
				// Primitive values and strings do not contribute to area analysis.
			}
		}
	}

	/**
	 * Scans an annotation for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param annotation
	 * 		Annotation to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addDexAnnotationReferences(@Nonnull AreaGraph graph,
	                                        @Nonnull Node node,
	                                        @Nonnull AnnotationPart annotation,
	                                        int weight) {
		addTypeDescriptorReferences(graph, node, annotation.type().descriptor(), weight);
		annotation.elements().values().forEach(value -> addDexConstantReferences(graph, node, value, weight));
	}

	/**
	 * Scans a method handle for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param handle
	 * 		Method handle to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addHandleReferences(@Nonnull AreaGraph graph,
	                                 @Nonnull Node node,
	                                 @Nonnull Handle handle,
	                                 int weight) {
		addReference(graph, node, normalizeInternalName(handle.getOwner()), weight);
		addTypeDescriptorReferences(graph, node, handle.getDesc(), weight);
	}

	/**
	 * Scans a method handle for references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param handle
	 * 		Method handle to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addDexHandleReferences(@Nonnull AreaGraph graph,
	                                    @Nonnull Node node,
	                                    @Nonnull me.darknet.dex.tree.definitions.constant.Handle handle,
	                                    int weight) {
		addReference(graph, node, normalizeInternalName(handle.owner().internalName()), weight);
		addTypeDescriptorReferences(graph, node, handle.type().descriptor(), weight);
	}

	/**
	 * Scans a class/field/method signature for type references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param signature
	 * 		Signature to scan.
	 * @param context
	 * 		Context of the signature, which affects how it is parsed.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addSignatureReferences(@Nonnull AreaGraph graph,
	                                    @Nonnull Node node,
	                                    @Nullable String signature,
	                                    @Nonnull Types.SignatureContext context,
	                                    int weight) {
		// Skip junk
		if (signature == null || !Types.isValidSignature(signature, context))
			return;

		SignatureVisitor visitor = new SignatureVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public void visitClassType(String name) {
				addReference(graph, node, normalizeInternalName(name), weight);
				super.visitClassType(name);
			}
		};
		try {
			SignatureReader reader = new SignatureReader(signature);
			if (context == Types.SignatureContext.FIELD)
				reader.acceptType(visitor);
			else
				reader.accept(visitor);
		} catch (IllegalArgumentException ignored) {
			// Ignore malformed generic signatures and fall back to raw descriptors.
		}
	}

	/**
	 * Scans a method descriptor for type references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param descriptor
	 * 		Method descriptor to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addMethodDescriptorReferences(@Nonnull AreaGraph graph,
	                                           @Nonnull Node node,
	                                           @Nonnull String descriptor,
	                                           int weight) {
		if (!Types.isValidDesc(descriptor))
			return;
		addAsmTypeReferences(graph, node, Type.getMethodType(descriptor), weight);
	}

	/**
	 * Scans a type descriptor for type references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param descriptor
	 * 		Type descriptor to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addTypeDescriptorReferences(@Nonnull AreaGraph graph,
	                                         @Nonnull Node node,
	                                         @Nonnull String descriptor,
	                                         int weight) {
		if (!Types.isValidDesc(descriptor))
			return;
		addAsmTypeReferences(graph, node, Type.getType(descriptor), weight);
	}

	/**
	 * Scans a type descriptor for type references.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some class to infill.
	 * @param type
	 * 		Type to scan.
	 * @param weight
	 * 		Weight to apply to any references found.
	 */
	private void addAsmTypeReferences(@Nonnull AreaGraph graph,
	                                  @Nonnull Node node,
	                                  @Nonnull Type type,
	                                  int weight) {
		switch (type.getSort()) {
			case Type.METHOD -> {
				addAsmTypeReferences(graph, node, type.getReturnType(), weight);
				for (Type argumentType : type.getArgumentTypes())
					addAsmTypeReferences(graph, node, argumentType, weight);
			}
			case Type.ARRAY -> addAsmTypeReferences(graph, node, type.getElementType(), weight);
			case Type.OBJECT -> addReference(graph, node, type.getInternalName(), weight);
			default -> {
				// Primitive types do not contribute to area analysis.
			}
		}
	}

	/**
	 * Adds a reference from the given node to the target class represented by the reference name, with the specified weight.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param node
	 * 		Node of some source class.
	 * @param referenceName
	 * 		Internal name of the target class being referenced.
	 * @param weight
	 * 		Weight to apply to the reference, which affects grouping decisions.
	 */
	private void addReference(@Nonnull AreaGraph graph,
	                          @Nonnull Node node,
	                          @Nullable String referenceName,
	                          int weight) {
		// Skip invalid references and self-references.
		String normalized = normalizeInternalName(referenceName);
		if (normalized == null || normalized.equals(node.path.getValue().getName()))
			return;

		// Update purpose hint weights for the source node based on the reference target.
		recordPurposeHint(node, normalized, weight);

		// If the target class is in our graph, add an internal reference edge.
		// Otherwise, if external references are being considered, add to the external reference weight.
		if (graph.nodes.containsKey(normalized))
			node.internalOutgoing.computeIfAbsent(normalized, key -> new EdgeAccumulator()).add(weight);
		else if (config.getIncludeExternalReferenceScores().getValue())
			node.externalOutgoing.add(weight);
	}

	/**
	 * Update the accumulated purpose weights for the node based on the reference target.
	 *
	 * @param node
	 * 		Source node of the reference.
	 * @param referenceName
	 * 		Internal name of the target class being referenced.
	 * @param weight
	 * 		Weight to apply to the reference.
	 *
	 * @see Node#purposeWeights
	 */
	private void recordPurposeHint(@Nonnull Node node, @Nonnull String referenceName, int weight) {
		String purpose = PackagePurpose.objectBucket(referenceName);
		if (!PackagePurpose.DEFAULT_BUCKET.equals(purpose))
			node.purposeWeights.merge(purpose, weight, Integer::sum);
	}

	/**
	 * Adds a reverse reference from the owner class to the target class.
	 * This creates a cycle between the owner and target, which encourages them to be grouped together.
	 *
	 * @param graph
	 * 		Graph to infill.
	 * @param ownerName
	 * 		Internal name of the owner class.
	 * @param targetName
	 * 		Internal name of the target class.
	 * @param weight
	 * 		Weight to apply to the reference, which affects grouping decisions.
	 *
	 * @see #scanClassMetadata(AreaGraph, Node, ClassInfo) Calling context, currently just on inner/outer class relationships.
	 */
	private void addReverseReference(@Nonnull AreaGraph graph,
	                                 @Nullable String ownerName,
	                                 @Nullable String targetName,
	                                 int weight) {
		String owner = normalizeInternalName(ownerName);
		String target = normalizeInternalName(targetName);
		if (owner == null || target == null || owner.equals(target))
			return;
		Node node = graph.nodes.get(owner);
		if (node != null)
			addReference(graph, node, target, weight);
	}

	/**
	 * @param name
	 * 		Internal name or descriptor to normalize into an internal name, or {@code null} to skip.
	 *
	 * @return Normalized internal name, or {@code null} if the input is not a valid class reference.
	 */
	@Nullable
	private static String normalizeInternalName(@Nullable String name) {
		if (name == null || name.isBlank())
			return null;

		// Skip primitive types and method descriptors, which are not valid class references.
		if (Types.isPrimitive(name) || Types.isPrimitiveClassName(name))
			return null;

		// Skip method types.
		if (name.startsWith("("))
			return null;

		// If it's a valid type descriptor, extract the internal name of the class it references.
		if (Types.isValidDesc(name)) {
			Type type = Type.getType(name);
			if (type.getSort() == Type.ARRAY)
				type = type.getElementType();
			return type.getSort() == Type.OBJECT ? type.getInternalName() : null;
		}
		return name.replace('.', '/');
	}

	/**
	 * Initializes the merge state with the initial components and their internal reference weights.
	 *
	 * @param initialComponents
	 * 		Initial components to build the merge state from.
	 * @param graph
	 * 		Graph containing the nodes and their reference information to populate the merge state with.
	 *
	 * @return Initialized merge state ready for the merging process.
	 */
	@Nonnull
	private MergeState buildMergeState(@Nonnull List<Component> initialComponents, @Nonnull AreaGraph graph) {
		Map<String, String> classToRoot = new HashMap<>();
		Map<String, MergedGroup> groups = new LinkedHashMap<>();

		// Initialize:
		// - Groups from initial components
		// - Class-to-root mapping
		for (Component component : initialComponents) {
			MergedGroup group = new MergedGroup(component.rootName());
			group.memberNames.addAll(component.members);
			group.representativeName = component.rootName();
			group.memberCount = component.members.size();
			group.initialComponentCount = 1;

			// Each member (class) of the component is mapped to the component's root,
			// and contributes its reference weights to the group.
			for (String member : component.members) {
				classToRoot.put(member, component.rootName());
				Node node = graph.nodes.get(member);
				if (node != null) {
					group.memberPaths.put(member, node.path);
					group.containsEntryPoint |= node.containsEntryPoint;
					node.purposeWeights.forEach((purpose, weight) -> group.purposeWeights.merge(purpose, weight, Integer::sum));
					group.externalOutgoingWeight += node.externalOutgoing.weight;
				}
			}
			groups.put(group.rootName, group);
		}

		// Determine group-to-group reference weights by examining edges from each class to its outgoing references.
		for (Node node : graph.nodes.values()) {
			// Get the root group for this class. If it doesn't belong to any group, skip it.
			String className = node.path.getValue().getName();
			String sourceRoot = classToRoot.get(className);
			if (sourceRoot == null)
				continue;
			MergedGroup source = groups.get(sourceRoot);
			if (source == null)
				continue;

			// For all edges from this class to other classes, determine if they are internal to the group or outgoing to another group.
			for (Map.Entry<String, EdgeAccumulator> edgeEntry : node.internalOutgoing.entrySet()) {
				String targetRoot = classToRoot.get(edgeEntry.getKey());
				if (targetRoot == null)
					continue;

				// Skip if the target class is not part of any known group.
				MergedGroup target = groups.get(targetRoot);
				if (target == null)
					continue;

				// If the edge is internal to the group, accumulate its weight and count as internal.
				// Otherwise, add it as an outgoing edge from the source group to the target group.
				if (sourceRoot.equals(targetRoot)) {
					source.internalWeight += edgeEntry.getValue().weight;
					source.internalEdgeCount += edgeEntry.getValue().edgeCount;
				} else {
					MergeState.connect(source, target, edgeEntry.getValue());
				}
			}
		}

		return new MergeState(groups);
	}

	/**
	 * Merges groups in the merge state based on the configured criteria until no more beneficial merges can be found.
	 *
	 * @param mergeState
	 * 		Merge state containing groups and cross-group reference information to perform merges on.
	 *
	 * @see #findMergeCandidate(List, MergeState, Map)
	 */
	private void mergeComponents(@Nonnull MergeState mergeState) {
		// Some people may not want to merge at all, and that's fine.
		// You can enjoy your clutter if you want.
		if (!config.getMergeSccGroups().getValue())
			return;

		while (true) {
			List<MergedGroup> orderedGroups = mergeState.orderedGroups();
			List<MergedGroup> order = mergeState.topologicalOrder(orderedGroups);

			// Map root names to their position in the current topological order,
			// to prioritize merges towards groups that are closer together in the order.
			Map<String, Integer> groupOrder = new HashMap<>();
			for (int i = 0; i < orderedGroups.size(); i++)
				groupOrder.put(orderedGroups.get(i).rootName, i);

			// Check if we can find a good merge candidate based on the current order and group relationships.
			// If not, we're done.
			MergeCandidate candidate = findMergeCandidate(order, mergeState, groupOrder);
			if (candidate == null)
				return;
			mergeState.merge(candidate.sourceRoot, candidate.targetRoot);
		}
	}

	/**
	 * @param order
	 * 		Topologically ordered groups to consider for merging, where earlier groups are preferred as sources and later groups are preferred as targets.
	 * @param mergeState
	 * 		Current merge state containing groups and their reference relationships to evaluate for potential merges.
	 * @param groupOrder
	 * 		Mapping of group root names to their position in the topological order.
	 *
	 * @return A merge candidate representing a source and target group that meet the criteria for merging, or {@code null} if no suitable candidate is found.
	 */
	@Nullable
	private MergeCandidate findMergeCandidate(@Nonnull List<MergedGroup> order,
	                                          @Nonnull MergeState mergeState,
	                                          @Nonnull Map<String, Integer> groupOrder) {
		// For each group in the given order, try and find a suitable edge to another group that meets the criteria for merging.
		for (MergedGroup source : order) {
			// Sort edges by topological order of the target group (prefer closer groups), then by target group name for determinism.
			List<Map.Entry<String, EdgeAccumulator>> outgoing = new ArrayList<>(source.outgoing.entrySet());
			outgoing.sort(Comparator
					.comparingInt((Map.Entry<String, EdgeAccumulator> edge) ->
							groupOrder.getOrDefault(edge.getKey(), Integer.MAX_VALUE))
					.thenComparing(Map.Entry::getKey));

			// Check if any edge is a good candidate.
			for (Map.Entry<String, EdgeAccumulator> edge : outgoing) {
				// Must know the target group.
				MergedGroup target = mergeState.groups.get(edge.getKey());
				if (target == null)
					continue;

				// Skip if the target group is too large.
				if (target.memberCount > config.getMaxMergedChildSize().getValue())
					continue;

				// Skip if the edge weight is too small to justify merging.
				if (edge.getValue().weight < MERGE_MIN_EDGE_WEIGHT)
					continue;

				// Skip if the target group has no incoming weight,
				// as that likely means it's not actually dependent on the source group and the edge is just noise.
				if (target.incomingWeight == 0)
					continue;

				// Skip if the edge weight is not dominant enough compared to the target's total incoming weight,
				// as that suggests the target group has stronger dependencies elsewhere and merging it with the
				// source would not create a cohesive area (it would be better to merge with the other stronger connection).
				if (edge.getValue().weight * 100 < target.incomingWeight * MERGE_INCOMING_DOMINANCE_PERCENT)
					continue;

				// Skip if the target group has significantly more outgoing weight than the edge weight,
				// as that suggests the target group is more of a provider to other groups and merging
				// it with the source would create a less cohesive area (it would be better to merge
				// with one of its stronger outgoing connections).
				if (target.outgoingWeight > edge.getValue().weight + MERGE_OUTGOING_LEEWAY)
					continue;

				// Everything passed, we should merge the two groups.
				return new MergeCandidate(source.rootName, target.rootName);
			}
		}
		return null;
	}

	/**
	 * Builds the final list of area links between groups based on the merged groups and their outgoing edges.
	 *
	 * @param orderedGroups
	 * 		Topologically ordered list of merged groups to build links from.
	 * @param rootToGroupId
	 * 		Mapping of group root names to their assigned group IDs.
	 *
	 * @return List of area links representing the connections between groups, sorted by source group ID then target group ID.
	 */
	@Nonnull
	private List<AreaLink> buildLinks(@Nonnull List<MergedGroup> orderedGroups,
	                                  @Nonnull Map<String, Integer> rootToGroupId) {
		List<AreaLink> links = new ArrayList<>();

		// For each group, add links for its outgoing edges to other groups,
		// using the assigned group IDs for source and target.
		for (MergedGroup source : orderedGroups) {
			TreeMap<String, EdgeAccumulator> ordered = new TreeMap<>(source.outgoing);
			for (Map.Entry<String, EdgeAccumulator> edgeEntry : ordered.entrySet()) {
				Integer sourceId = rootToGroupId.get(source.rootName);
				Integer targetId = rootToGroupId.get(edgeEntry.getKey());
				if (sourceId == null || targetId == null)
					continue;
				EdgeAccumulator value = edgeEntry.getValue();
				links.add(new AreaLink(sourceId, targetId, value.weight, value.edgeCount));
			}
		}

		// Sort for consistent output. Shouldn't really matter but, it makes testing easier and the output more readable.
		links.sort(Comparator.comparingInt(AreaLink::sourceGroupId)
				.thenComparingInt(AreaLink::targetGroupId));

		return List.copyOf(links);
	}

	/**
	 * Builds the final list of area groups based on the merged groups, their member classes, and their reference weights.
	 *
	 * @param orderedGroups
	 * 		Topologically ordered list of merged groups to build areas from.
	 * @param rootToGroupId
	 * 		Mapping of group root names to their assigned group IDs.
	 * @param spaghettiDetected
	 * 		Flag indicating whether a dominant spaghetti group was detected, which may affect confidence scores.
	 * @param dominantRoot
	 * 		Internal name of the dominant spaghetti group root, if detected, which may affect confidence scores for that group.
	 *
	 * @return List of area groups representing the final grouped areas.
	 */
	@Nonnull
	private List<AreaGroup> buildPublicGroups(@Nonnull List<MergedGroup> orderedGroups,
	                                          @Nonnull Map<String, Integer> rootToGroupId,
	                                          boolean spaghettiDetected,
	                                          @Nullable String dominantRoot) {
		List<AreaGroup> groups = new ArrayList<>();
		for (MergedGroup group : orderedGroups) {
			// Map all classes in the group from internal names to paths.
			List<ClassPathNode> classes = group.memberNames.stream()
					.map(name -> Objects.requireNonNull(group.memberPaths.get(name), "Missing class path for member: " + name))
					.toList();

			// Get basic information about the group and compute its confidence score based on various heuristics.
			AreaFormationKind formationKind = group.initialComponentCount > 1 ? AreaFormationKind.MERGED : AreaFormationKind.SCC;
			String purpose = selectDominantPurpose(group.purposeWeights);
			double confidence = computeConfidence(group, formationKind, spaghettiDetected && group.rootName.equals(dominantRoot));
			groups.add(new AreaGroup(
					rootToGroupId.get(group.rootName),
					classes,
					formationKind,
					purpose,
					confidence,
					group.incoming.size(),
					group.outgoing.size(),
					group.containsEntryPoint
			));
		}

		// The insertion order above is already sorted since the ordered groups are pre-sorted in the calling context.
		return List.copyOf(groups);
	}

	/**
	 * Heuristic confidence computation for how likely a group is a cohesive area of functionality.
	 *
	 * @param group
	 * 		Group to compute confidence for.
	 * @param formationKind
	 * 		How the group was formed.
	 * @param dominantSpaghettiGroup
	 * 		Whether this group is the dominant spaghetti group.
	 *
	 * @return Confidence score between {@code [0, 1.0]}.
	 */
	private double computeConfidence(@Nonnull MergedGroup group,
	                                 @Nonnull AreaFormationKind formationKind,
	                                 boolean dominantSpaghettiGroup) {
		double confidence = formationKind == AreaFormationKind.SCC ? 0.82 : 0.68;

		// Penalize groups that are small,
		// since a single class is less likely to represent a cohesive area than multiple classes working together.
		if (group.memberCount == 1)
			confidence -= 0.12;

		// Penalize groups that are part of a dominant spaghetti group,
		// since they are likely part of a large, tangled area of functionality that is not cohesive.
		if (dominantSpaghettiGroup)
			confidence -= 0.18;

		// Boost confidence if the group contains an entry point,
		// since those are more likely to be cohesive areas of functionality.
		if (group.containsEntryPoint)
			confidence += 0.05;

		// Boost confidence for groups with strong internal connectivity,
		// since they are more likely to represent cohesive areas of functionality.
		if (group.internalWeight >= Math.max(8, group.memberCount * 4))
			confidence += 0.10;

		return Math.clamp(confidence, 0.10, 0.99);
	}

	/**
	 * @param purposeWeights
	 * 		Map of purpose groups to their accumulated weights based on references from classes in a group.
	 *
	 * @return Dominant purpose group based on the accumulated weights,
	 * or the {@link PackagePurpose#DEFAULT_BUCKET default bucket} if no dominant purpose can be determined.
	 *
	 * @see PackagePurpose
	 */
	@Nonnull
	private static String selectDominantPurpose(@Nonnull Map<String, Integer> purposeWeights) {
		return purposeWeights.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
						.thenComparing(Map.Entry.comparingByKey()))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(PackagePurpose.DEFAULT_BUCKET);
	}

	/**
	 * @param groups
	 * 		All groups to analyze for signs of a dominant spaghetti group.
	 * @param analyzedClassCount
	 * 		Total number of analyzed classes across all groups.
	 *
	 * @return {@code true} if a dominant spaghetti group is detected based on the configured threshold, {@code false} otherwise.
	 *
	 * @see AreaAnalysisConfig#getSpaghettiThresholdPercent()
	 */
	private boolean isSpaghettiDetected(@Nonnull List<MergedGroup> groups, int analyzedClassCount) {
		if (groups.isEmpty() || analyzedClassCount == 0)
			return false;

		// If the largest group contains more than the configured percentage of all analyzed classes,
		// we consider it a sign of a dominant spaghetti group.
		int threshold = config.getSpaghettiThresholdPercent().getValue();
		int largestGroupSize = groups.stream()
				.mapToInt(group -> group.memberCount)
				.max()
				.orElse(0);
		return largestGroupSize * 100 >= threshold * analyzedClassCount;
	}

	/**
	 * @param groups
	 * 		All groups to analyze for the dominant group root.
	 *
	 * @return Internal name of the dominant group root,
	 * defined as the group with the largest member count,
	 * or {@code null} if there are no groups.
	 */
	@Nullable
	private static String dominantGroupRoot(@Nonnull List<MergedGroup> groups) {
		return groups.stream()
				.max(Comparator.comparingInt((MergedGroup group) -> group.memberCount)
						.thenComparing(group -> group.representativeName))
				.map(group -> group.rootName)
				.orElse(null);
	}

	/**
	 * Intermediate graph structure for initial analysis and grouping, before merging.
	 */
	private static final class AreaGraph {
		// Internal class name -> Node
		private final Map<String, Node> nodes = new TreeMap<>();
		// Paths to classes that were referenced but not grouped, largely for reporting/debugging purposes.
		private final List<ClassPathNode> ungroupedClasses = new ArrayList<>();
	}

	/**
	 * Element of {@link AreaGraph}.
	 */
	private static final class Node {
		// Workspace class reference.
		private final ClassPathNode path;
		// Reference by internal class name -> accumulated weight and count of edges to that class.
		private final Map<String, EdgeAccumulator> internalOutgoing = new HashMap<>();
		// PackagePurpose bucket -> accumulated weight of references to classes in that bucket, used for purpose hinting.
		private final Map<String, Integer> purposeWeights = new HashMap<>();
		// Outgoing reference weight to classes that are not part of the graph.
		private final EdgeAccumulator externalOutgoing = new EdgeAccumulator();
		// Does this have a 'main(String[])' method or something else viable as an entry point? Used for confidence scoring.
		private boolean containsEntryPoint;

		private Node(@Nonnull ClassPathNode path) {
			this.path = path;
		}
	}

	/**
	 * Edge count + weight for {@link Node}.
	 */
	private static final class EdgeAccumulator {
		private int weight;
		private int edgeCount;

		private EdgeAccumulator() {
			// default
		}

		private EdgeAccumulator(int weight, int edgeCount) {
			this.weight = weight;
			this.edgeCount = edgeCount;
		}

		private void add(int weight) {
			this.weight += weight;
			edgeCount++;
		}

		private void add(@Nonnull EdgeAccumulator other) {
			weight += other.weight;
			edgeCount += other.edgeCount;
		}
	}

	/**
	 * Helper to compute strongly connected components (SCCs) of the graph.
	 * <p>
	 * See: <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's algorithm</a>
	 * <p>
	 * video: <a href="https://www.youtube.com/watch?v=wUgWX0nc4NY">WilliamFiset's Tarjan's Strongly Connected Component (SCC) Algorithm</a>
	 */
	private static final class Tarjan {
		private final AreaGraph graph;
		private final Map<String, Integer> indexByNode = new HashMap<>();
		private final Map<String, Integer> lowByNode = new HashMap<>();
		private final ArrayDeque<String> stack = new ArrayDeque<>();
		private final Set<String> onStack = new HashSet<>();
		private final List<Component> components = new ArrayList<>();
		private int index;

		private Tarjan(@Nonnull AreaGraph graph) {
			this.graph = graph;
		}

		@Nonnull
		private List<Component> compute() {
			for (String nodeName : graph.nodes.keySet())
				if (!indexByNode.containsKey(nodeName))
					strongConnect(nodeName);
			components.sort(Comparator.comparing(Component::rootName));
			return List.copyOf(components);
		}

		private void strongConnect(@Nonnull String nodeName) {
			indexByNode.put(nodeName, index);
			lowByNode.put(nodeName, index);
			index++;
			stack.push(nodeName);
			onStack.add(nodeName);

			// Sort neighbors for deterministic output, even though the order doesn't affect the resulting components.
			List<String> neighbors = new ArrayList<>(graph.nodes.get(nodeName).internalOutgoing.keySet());
			Collections.sort(neighbors);

			// Explore neighbors, tracking low-link values to identify strongly connected components.
			for (String neighbor : neighbors) {
				if (!indexByNode.containsKey(neighbor)) {
					strongConnect(neighbor);
					lowByNode.put(nodeName, Math.min(lowByNode.get(nodeName), lowByNode.get(neighbor)));
				} else if (onStack.contains(neighbor)) {
					lowByNode.put(nodeName, Math.min(lowByNode.get(nodeName), indexByNode.get(neighbor)));
				}
			}

			// If this node is a root of a strongly connected component, pop the stack to build the component.
			if (Objects.equals(lowByNode.get(nodeName), indexByNode.get(nodeName))) {
				List<String> members = new ArrayList<>();
				String member;
				do {
					member = stack.pop();
					onStack.remove(member);
					members.add(member);
				} while (!nodeName.equals(member));
				members.sort(String::compareTo);
				components.add(new Component(members));
			}
		}
	}

	/**
	 * Initial component of classes that are strongly connected to each other, represented by their internal names.
	 *
	 * @param members
	 * 		Internal class names of the members of this component, sorted with the representative first for determinism.
	 *
	 * @see Tarjan#components
	 */
	private record Component(@Nonnull List<String> members) {
		/**
		 * @return First member of the component.
		 */
		@Nonnull
		private String rootName() {
			return members.getFirst();
		}
	}

	/**
	 * @param sourceRoot
	 * 		Root of the source group to merge.
	 * @param targetRoot
	 * 		Root of the target group to merge.
	 *
	 * @see #findMergeCandidate(List, MergeState, Map)
	 */
	private record MergeCandidate(@Nonnull String sourceRoot, @Nonnull String targetRoot) {}

	/**
	 * Intermediate structure for merged groups during the merging process.
	 */
	private static final class MergeState {
		private static final Comparator<MergedGroup> GROUP_ORDER = Comparator
				.comparing((MergedGroup group) -> group.representativeName)
				.thenComparing(group -> group.rootName);

		// Internal name of root group -> MergedGroup
		private final Map<String, MergedGroup> groups;

		private MergeState(@Nonnull Map<String, MergedGroup> groups) {
			this.groups = groups;
		}

		/**
		 * @return Groups sorted by their representative name, then root name, for deterministic processing order in merging and output.
		 */
		@Nonnull
		private List<MergedGroup> orderedGroups() {
			return groups.values().stream()
					.sorted(GROUP_ORDER)
					.toList();
		}

		/**
		 * Computes a topological order of the groups based on their outgoing edges, to prioritize merges towards groups that are closer together in the order.
		 * <p>
		 * Video: <a href="https://www.youtube.com/watch?v=cIBFEhD77b4">WilliamFiset's Topological Sort (Kahn's Algorithm)</a>
		 *
		 * @param orderedGroups
		 * 		Ordered groups pre-sorted. Should always be the result of {@link #orderedGroups()}.
		 *
		 * @return Groups sorted in topological order based on their outgoing edges, to prioritize merges towards groups that are closer together in the order.
		 */
		@Nonnull
		private List<MergedGroup> topologicalOrder(@Nonnull List<MergedGroup> orderedGroups) {
			// Compute in-degrees based on outgoing edges.
			Map<String, Integer> indegree = new HashMap<>();
			for (MergedGroup group : orderedGroups)
				indegree.put(group.rootName, 0);
			for (MergedGroup group : orderedGroups)
				group.outgoing.keySet().forEach(target -> indegree.computeIfPresent(target, (key, value) -> value + 1));

			// Compute topological order using Kahn's algorithm, with ties broken by the given pre-sorted order of groups.
			TreeSet<MergedGroup> ready = new TreeSet<>(GROUP_ORDER);
			for (MergedGroup group : orderedGroups)
				if (indegree.getOrDefault(group.rootName, 0) == 0)
					ready.add(group);

			// Continually take groups with zero in-degree, and reduce the in-degrees of their targets
			// until we've processed all groups, or we find a cycle.
			List<MergedGroup> result = new ArrayList<>(orderedGroups.size());
			while (!ready.isEmpty()) {
				MergedGroup group = ready.pollFirst();
				result.add(group);
				for (String target : new TreeSet<>(group.outgoing.keySet())) {
					int newValue = indegree.computeIfPresent(target, (key, value) -> value - 1);
					if (newValue == 0) {
						MergedGroup targetGroup = groups.get(target);
						if (targetGroup != null)
							ready.add(targetGroup);
					}
				}
			}

			// If we couldn't process all groups, that means there's a cycle.
			// In that case, just return the pre-sorted order to avoid losing any merges,
			if (result.size() != orderedGroups.size())
				return orderedGroups;
			return result;
		}

		/**
		 * Merges the source group into the target group, absorbing all members and edges, and updating the groups map accordingly.
		 *
		 * @param sourceRoot
		 * 		Root of the source group to merge.
		 * @param targetRoot
		 * 		Root of the target group to merge.
		 */
		private void merge(@Nonnull String sourceRoot, @Nonnull String targetRoot) {
			// Skip if either group cannot be found (shouldn't happen)
			// or if they are the same group (should also not happen since we check for that before merging).
			MergedGroup source = groups.get(sourceRoot);
			MergedGroup target = groups.get(targetRoot);
			if (source == null || target == null || source == target)
				return;

			// Disconnect source and target from each other and absorb their internal edge weight if they were connected,
			// since that edge becomes internal to the merged group and no longer contributes to outgoing/incoming weights.
			EdgeAccumulator sourceToTarget = disconnect(source, target);
			if (sourceToTarget != null)
				source.addInternal(sourceToTarget);
			EdgeAccumulator targetToSource = disconnect(target, source);
			if (targetToSource != null)
				source.addInternal(targetToSource);

			// Accumulate all members and internal weights from the target group into the source group,
			// and update the source group's representative name and member count accordingly.
			source.absorb(target);

			// Reconnect all edges from the target group to other groups,
			// updating the source group's outgoing edges and the other groups' incoming edges accordingly.
			List<String> targetOutgoingRoots = new ArrayList<>(target.outgoing.keySet());
			for (String otherRoot : targetOutgoingRoots) {
				MergedGroup other = groups.get(otherRoot);
				if (other == null)
					continue;
				EdgeAccumulator edge = disconnect(target, other);
				if (edge != null)
					connect(source, other, edge);
			}

			// Reconnect all edges from other groups to the target group,
			// updating the source group's incoming edges and the other groups' outgoing edges accordingly.
			List<String> targetIncomingRoots = new ArrayList<>(target.incoming.keySet());
			for (String otherRoot : targetIncomingRoots) {
				MergedGroup other = groups.get(otherRoot);
				if (other == null)
					continue;
				EdgeAccumulator edge = disconnect(other, target);
				if (edge != null)
					connect(other, source, edge);
			}

			// Delete the merged target group from the groups
			// map now that the source absorbed all of its members and edges.
			groups.remove(targetRoot);
		}

		/**
		 * @param source
		 * 		Source group of the edge to connect.
		 * @param target
		 * 		Target group of the edge to connect <i>(and then delete)</i>.
		 * @param delta
		 * 		Weight and count to add to the edge from source to target.
		 */
		private static void connect(@Nonnull MergedGroup source,
		                            @Nonnull MergedGroup target,
		                            @Nonnull EdgeAccumulator delta) {
			// Skip if they are the same group.
			// This implies a self-loop edge.
			if (source == target) {
				source.addInternal(delta);
				return;
			}

			// Add the delta weight to the edge from source to target, creating it if it doesn't exist,
			// and update the outgoing weight of the source and the incoming weight of the target accordingly.
			EdgeAccumulator edge = source.outgoing.get(target.rootName);
			if (edge == null) {
				edge = new EdgeAccumulator(delta.weight, delta.edgeCount);
				source.outgoing.put(target.rootName, edge);
				target.incoming.put(source.rootName, edge);
			} else {
				edge.add(delta);
			}
			source.outgoingWeight += delta.weight;
			target.incomingWeight += delta.weight;
		}

		/**
		 * @param source
		 * 		Source group of the edge to disconnect.
		 * @param target
		 * 		Target group of the edge to disconnect.
		 *
		 * @return The weight and count of the disconnected edge, or {@code null} if there was no edge to disconnect.
		 */
		@Nullable
		private static EdgeAccumulator disconnect(@Nonnull MergedGroup source, @Nonnull MergedGroup target) {
			EdgeAccumulator edge = source.outgoing.remove(target.rootName);
			if (edge == null)
				return null;
			source.outgoingWeight -= edge.weight;
			target.incoming.remove(source.rootName);
			target.incomingWeight -= edge.weight;
			return edge;
		}
	}

	/**
	 * Intermediate structure representing a merged group of classes during the merging process, along with its members and reference weights.
	 *
	 * @see MergeState#groups
	 */
	private static final class MergedGroup {
		private final String rootName;
		private final List<String> memberNames = new ArrayList<>();
		private final Map<String, ClassPathNode> memberPaths = new HashMap<>();
		private final Map<String, EdgeAccumulator> outgoing = new LinkedHashMap<>();
		private final Map<String, EdgeAccumulator> incoming = new LinkedHashMap<>();
		private final Map<String, Integer> purposeWeights = new HashMap<>();
		private String representativeName;
		private int memberCount;
		private int initialComponentCount;
		private int internalWeight;
		private int internalEdgeCount;
		private int externalOutgoingWeight;
		private int incomingWeight;
		private int outgoingWeight;
		private boolean containsEntryPoint;

		private MergedGroup(@Nonnull String rootName) {
			this.rootName = rootName;
		}

		private void addInternal(@Nonnull EdgeAccumulator edge) {
			internalWeight += edge.weight;
			internalEdgeCount += edge.edgeCount;
		}

		private void absorb(@Nonnull MergedGroup other) {
			List<String> mergedMemberNames = mergeSortedNames(memberNames, other.memberNames);
			memberPaths.putAll(other.memberPaths);
			other.purposeWeights.forEach((purpose, weight) -> purposeWeights.merge(purpose, weight, Integer::sum));
			containsEntryPoint |= other.containsEntryPoint;
			externalOutgoingWeight += other.externalOutgoingWeight;
			initialComponentCount += other.initialComponentCount;
			internalWeight += other.internalWeight;
			internalEdgeCount += other.internalEdgeCount;
			memberNames.clear();
			memberNames.addAll(mergedMemberNames);
			representativeName = memberNames.getFirst();
			memberCount = memberNames.size();
		}

		@Nonnull
		private static List<String> mergeSortedNames(@Nonnull List<String> left, @Nonnull List<String> right) {
			List<String> merged = new ArrayList<>(left.size() + right.size());
			int leftIndex = 0;
			int rightIndex = 0;
			while (leftIndex < left.size() && rightIndex < right.size()) {
				String leftValue = left.get(leftIndex);
				String rightValue = right.get(rightIndex);
				if (leftValue.compareTo(rightValue) <= 0) {
					merged.add(leftValue);
					leftIndex++;
				} else {
					merged.add(rightValue);
					rightIndex++;
				}
			}
			while (leftIndex < left.size())
				merged.add(left.get(leftIndex++));
			while (rightIndex < right.size())
				merged.add(right.get(rightIndex++));
			return merged;
		}
	}
}
