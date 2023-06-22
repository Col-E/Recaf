package software.coley.recaf.services.callgraph;

import dev.xdark.jlinker.MemberInfo;
import dev.xdark.jlinker.Resolution;
import dev.xdark.jlinker.ResolutionError;
import dev.xdark.jlinker.Result;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.MultiMap;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents method calls as a navigable graph.
 *
 * @author Amejonah
 * @author Matt Coley
 * @see MethodVertex
 */
@WorkspaceScoped
public class CallGraph implements Service, WorkspaceModificationListener, ResourceJvmClassListener {
	public static final String SERVICE_ID = "graph-calls";
	private static final DebuggingLogger logger = Logging.get(CallGraph.class);
	private final CachedLinkResolver resolver = new CachedLinkResolver();
	private final Map<JvmClassInfo, LinkedClass> classToLinkerType = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Map<JvmClassInfo, ClassMethodsContainer> classToMethodsContainer = Collections.synchronizedMap(new IdentityHashMap<>());
	private final MultiMap<String, MethodRef, Set<MethodRef>> unresolvedCalls = MultiMap.from(
			Collections.synchronizedMap(new HashMap<>()),
			() -> Collections.synchronizedSet(new HashSet<>()));
	private final CallGraphConfig config;
	private final ClassLookup lookup;

	/**
	 * @param config
	 * 		Graphing config options.
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	@Inject
	public CallGraph(@Nonnull CallGraphConfig config, @Nonnull Workspace workspace) {
		this.config = config;
		lookup = new ClassLookup(workspace);

		// Only initialize & register listeners if active
		if (config.getActive().getValue()) {
			workspace.addWorkspaceModificationListener(this);
			workspace.getPrimaryResource().addResourceJvmClassListener(this);
			initialize(workspace);
		}
	}

	/**
	 * @param classInfo
	 * 		Class to wrap.
	 *
	 * @return Wrapper for easy {@link MethodVertex} management for the class.
	 */
	@Nonnull
	public ClassMethodsContainer getClassMethodsContainer(@Nonnull JvmClassInfo classInfo) {
		return classToMethodsContainer.computeIfAbsent(classInfo, c -> new ClassMethodsContainer(classInfo));
	}

	/**
	 * @param classInfo
	 * 		Class to wrap.
	 *
	 * @return JLinker wrapper for class.
	 */
	@Nonnull
	private LinkedClass linked(@Nonnull JvmClassInfo classInfo) {
		return classToLinkerType.computeIfAbsent(classInfo, c -> new LinkedClass(lookup, c));
	}

	/**
	 * @param workspace
	 * 		Workspace to {@link #visit(JvmClassInfo)} all classes of.
	 */
	private void initialize(@Nonnull Workspace workspace) {
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			Stream.concat(resource.jvmClassBundleStream(),
					resource.getVersionedJvmClassBundles().values().stream()).forEach(bundle -> {
				for (JvmClassInfo jvmClass : bundle.values()) {
					visit(jvmClass);
				}
			});
		}
	}

	/**
	 * Populate {@link MethodVertex} for all methods in {@link JvmClassInfo#getMethods()}.
	 *
	 * @param jvmClass
	 * 		Class to visit.
	 */
	private void visit(@Nonnull JvmClassInfo jvmClass) {
		ClassMethodsContainer classMethodsContainer = getClassMethodsContainer(jvmClass);
		jvmClass.getClassReader().accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MutableMethodVertex methodVertex = (MutableMethodVertex) classMethodsContainer.getVertex(name, descriptor);
				if (methodVertex == null) {
					logger.error("Method {}{} was visited, but not present in info for declaring class {}",
							name, descriptor, jvmClass.getName());
					return null;
				}

				return new MethodVisitor(RecafConstants.getAsmVersion()) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						onMethodCalled(methodVertex, opcode, owner, name, descriptor, isInterface);
					}

					@Override
					public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						if (!"java/lang/invoke/LambdaMetafactory".equals(bootstrapMethodHandle.getOwner())
								|| !"metafactory".equals(bootstrapMethodHandle.getName())
								|| !"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(bootstrapMethodHandle.getDesc())) {
							super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
							return;
						}
						Object handleObj = bootstrapMethodArguments.length == 3 ? bootstrapMethodArguments[1] : null;
						if (handleObj instanceof Handle handle) {
							switch (handle.getTag()) {
								case Opcodes.H_INVOKESPECIAL:
								case Opcodes.H_INVOKEVIRTUAL:
								case Opcodes.H_INVOKESTATIC:
								case Opcodes.H_INVOKEINTERFACE:
									visitMethodInsn(handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
							}
							return;
						}
						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
					}
				};
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	/**
	 * Called from the {@link ClassReader} in {@link #visit(JvmClassInfo)}.
	 * Links the given vertex to the remote {@link MethodVertex} of the resolved method call,
	 * if resolution is a success. When not successful, the call is recorded as an
	 * {@link #unresolvedCalls unresolved reference}.
	 *
	 * @param methodVertex
	 * 		The method that is doing the call.
	 * @param opcode
	 * 		Call opcode.
	 * @param owner
	 * 		Call owner.
	 * @param name
	 * 		Method call name.
	 * @param descriptor
	 * 		Method call descriptor.
	 * @param isInterface
	 * 		Method interface flag.
	 */
	private void onMethodCalled(MutableMethodVertex methodVertex, int opcode, String owner, String name,
								String descriptor, boolean isInterface) {
		MethodRef ref = new MethodRef(owner, name, descriptor);

		// Resolve the method
		Result<Resolution<JvmClassInfo, MethodMember>> resolutionResult = resolve(opcode, owner, name, descriptor, isInterface);

		// Handle result
		if (resolutionResult != null && resolutionResult.isSuccess()) {
			// Extract vertex from resolution
			Resolution<JvmClassInfo, MethodMember> resolution = resolutionResult.value();
			ClassMethodsContainer resolvedClass = getClassMethodsContainer(resolution.owner().innerValue());
			MutableMethodVertex resolvedMethodCallVertex = (MutableMethodVertex) resolvedClass.getVertex(resolution.member().innerValue());

			// Link the vertices
			methodVertex.getCalls().add(resolvedMethodCallVertex);
			resolvedMethodCallVertex.getCallers().add(methodVertex);

			// Remove tracked unresolved call if any exist
			Set<MethodRef> unresolvedWithinOwner = unresolvedCalls.get(owner);
			if (unresolvedWithinOwner.remove(ref)) {
				logger.debugging(l -> l.info("Satisfy unresolved call {}", ref));
			}
		} else {
			unresolvedCalls.put(owner, ref);

			// The result is null when the class cannot be found.
			if (resolutionResult == null)
				logger.debugging(l -> l.warn("Defining class '{}' not found, cannot resolve method {}", owner, ref));
			else
				logger.debugging(l -> l.warn("Cannot resolve method: {} - {}", ref, resolutionResult.error()));
		}
	}

	/**
	 * @param opcode
	 * 		Method invoke opcode.
	 * @param owner
	 * 		Declaring class of method.
	 * @param name
	 * 		Name of method invoked.
	 * @param descriptor
	 * 		Descriptor of method invoked.
	 * @param isInterface
	 * 		Invoke interface flag.
	 *
	 * @return Resolution result of the method within the owner.
	 * {@code null} when the {@code owner} could not be found.
	 */
	@Nullable
	public Result<Resolution<JvmClassInfo, MethodMember>> resolve(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		JvmClassInfo ownerClass = lookup.apply(owner);

		// Skip if we cannot resolve owner
		if (ownerClass == null) {
			unresolvedCalls.put(owner, new MethodRef(owner, name, descriptor));
			return null;
		}

		Result<Resolution<JvmClassInfo, MethodMember>> resolutionResult;
		LinkedClass linkedOwnerClass = linked(ownerClass);
		switch (opcode) {
			case Opcodes.H_INVOKESPECIAL:
			case Opcodes.INVOKESPECIAL:
				// Invoke-Special is a direct call, so we do need to do resolving
				MemberInfo<MethodMember> method = linkedOwnerClass.getMethod(name, descriptor);
				if (method != null)
					resolutionResult = Result.ok(new Resolution<>(linkedOwnerClass, method, false));
				else
					resolutionResult = Result.error(ResolutionError.NO_SUCH_METHOD);
				break;
			case Opcodes.H_INVOKEVIRTUAL:
			case Opcodes.INVOKEVIRTUAL:
				resolutionResult = resolver.resolveVirtualMethod(linkedOwnerClass, name, descriptor);
				break;
			case Opcodes.H_INVOKEINTERFACE:
			case Opcodes.INVOKEINTERFACE:
				resolutionResult = resolver.resolveInterfaceMethod(linkedOwnerClass, name, descriptor);
				break;
			case Opcodes.H_INVOKESTATIC:
			case Opcodes.INVOKESTATIC:
				resolutionResult = resolver.resolveStaticMethod(linkedOwnerClass, name, descriptor);
				break;
			default:
				throw new IllegalArgumentException("Invalid method opcode: " + opcode);
		}
		return resolutionResult;
	}

	@Override
	public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		// Visit all library classes
		Stream.concat(library.jvmClassBundleStream(),
				library.getVersionedJvmClassBundles().values().stream()).forEach(bundle -> {
			for (JvmClassInfo jvmClass : bundle.values()) {
				onNewClass(library, bundle, jvmClass);
			}
		});
	}

	@Override
	public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		// Remove all vertices from library
		Stream.concat(library.jvmClassBundleStream(),
				library.getVersionedJvmClassBundles().values().stream()).forEach(bundle -> {
			for (JvmClassInfo jvmClass : bundle.values()) {
				onRemoveClass(library, bundle, jvmClass);
			}
		});
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		visit(cls);
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
		onRemoveClass(resource, bundle, oldCls);
		onNewClass(resource, bundle, newCls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		// Prune vertex connections of all methods within the class
		ClassMethodsContainer container = getClassMethodsContainer(cls);
		Set<MethodRef> unresolvedWithinOwner = unresolvedCalls.get(cls.getName());
		for (MethodVertex vertex : container.getVertices()) {
			MethodRef ref = vertex.getMethod();
			if (vertex instanceof MutableMethodVertex) {
				((MutableMethodVertex) vertex).prune();
				unresolvedWithinOwner.add(ref);
			} else {
				logger.warn("Could not prune reference: {}", ref);
			}
		}

		// Remove from maps
		classToLinkerType.remove(cls);
		classToMethodsContainer.remove(cls);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CallGraphConfig getServiceConfig() {
		return config;
	}

	/**
	 * Mutable impl of {@link MethodVertex}.
	 */
	static class MutableMethodVertex implements MethodVertex {
		private final Set<MethodVertex> callers = Collections.synchronizedSet(new HashSet<>());
		private final Set<MethodVertex> calls = Collections.synchronizedSet(new HashSet<>());
		private final MethodRef method;
		private final MethodMember resolvedMethod;

		MutableMethodVertex(MethodRef method, MethodMember resolvedMethod) {
			this.method = method;
			this.resolvedMethod = resolvedMethod;
		}

		/**
		 * Removes this method vertex from connected vertices.
		 */
		private void prune() {
			// Remove this vertex as a caller from the methods we call
			for (MethodVertex out : getCalls()) {
				if (out instanceof MutableMethodVertex) {
					out.getCallers().remove(this);
				}
			}

			// Remove this vertex as a destination from methods that call us
			for (MethodVertex in : getCallers()) {
				if (in instanceof MutableMethodVertex) {
					in.getCalls().remove(this);
				}
			}
		}

		@Nonnull
		@Override
		public MethodRef getMethod() {
			return method;
		}

		@Nullable
		@Override
		public MethodMember getResolvedMethod() {
			return resolvedMethod;
		}

		@Nonnull
		@Override
		public Collection<MethodVertex> getCallers() {
			return callers;
		}

		@Nonnull
		@Override
		public Collection<MethodVertex> getCalls() {
			return calls;
		}

		@Override
		public String toString() {
			return method.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MutableMethodVertex vertex = (MutableMethodVertex) o;
			return method.equals(vertex.method);
		}

		@Override
		public int hashCode() {
			return method.hashCode();
		}
	}
}
