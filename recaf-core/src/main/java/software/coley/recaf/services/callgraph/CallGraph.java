package software.coley.recaf.services.callgraph;

import com.google.common.annotations.VisibleForTesting;
import dev.xdark.jlinker.MemberInfo;
import dev.xdark.jlinker.Resolution;
import dev.xdark.jlinker.ResolutionError;
import dev.xdark.jlinker.Result;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.MultiMap;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Represents method calls as a navigable graph.
 *
 * @author Amejonah
 * @author Matt Coley
 * @see MethodVertex
 */
public class CallGraph implements WorkspaceModificationListener, ResourceJvmClassListener {
	private static final DebuggingLogger logger = Logging.get(CallGraph.class);
	private final ExecutorService threadPool = ThreadPoolFactory.newFixedThreadPool("call-graph", 1, true);
	private final CachedLinkResolver resolver = new CachedLinkResolver();
	private final Map<JvmClassInfo, LinkedClass> classToLinkerType = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Map<JvmClassInfo, ClassMethodsContainer> classToMethodsContainer = Collections.synchronizedMap(new IdentityHashMap<>());
	private final MultiMap<String, MethodRef, Set<MethodRef>> unresolvedDeclarations = MultiMap.from(
			new ConcurrentHashMap<>(),
			() -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
	private final MultiMap<String, CallingContext, Set<CallingContext>> unresolvedReferences = MultiMap.from(
			new ConcurrentHashMap<>(),
			() -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
	private final ObservableBoolean isReady = new ObservableBoolean(false);
	private final Workspace workspace;
	private final ClassLookup lookup;
	private boolean initialized;

	/**
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	public CallGraph(@Nonnull Workspace workspace) {
		this.workspace = workspace;

		lookup = new ClassLookup(workspace);
	}

	/**
	 * @return {@code true} when {@link #initialize()} has been called.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Observable boolean tracking the state of the call-graph's parsing of the current workspace.
	 */
	@Nonnull
	public ObservableBoolean isReady() {
		return isReady;
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
	 * @param method
	 * 		Method to get vertex of.
	 *
	 * @return Vertex of method. Can be {@code null} if the method member does not
	 * define its {@link MethodMember#getDeclaringClass() declaring class}.
	 */
	@Nullable
	public MethodVertex getVertex(@Nonnull MethodMember method) {
		ClassInfo declaringClass = method.getDeclaringClass();
		if (declaringClass == null) return null;
		return getClassMethodsContainer(declaringClass.asJvmClass()).getVertex(method);
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
	 * Initialize the graph.
	 */
	public void initialize() {
		// Only allow calls to initialize the graph once
		if (initialized) return;
		initialized = true;

		// Register modification listeners so that we can update the graph when class state changes.
		workspace.addWorkspaceModificationListener(this);
		workspace.getPrimaryResource().addResourceJvmClassListener(this);

		// Initialize asynchronously, and mark 'isReady' if completed successfully
		CompletableFuture.runAsync(() -> {
			for (WorkspaceResource resource : workspace.getAllResources(false)) {
				Stream.concat(resource.jvmClassBundleStream(),
						resource.getVersionedJvmClassBundles().values().stream()).forEach(bundle -> {
					for (JvmClassInfo jvmClass : bundle.values()) {
						visit(jvmClass);
					}
				});
			}
		}, threadPool).whenComplete((unused, t) -> {
			if (t == null) {
				isReady.setValue(true);
			} else {
				logger.error("Call graph initialization failed", t);
				isReady.setValue(false);
			}
		});
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
					public void visitEnd() {
						super.visitEnd();

						linkedResolvedCalls(jvmClass, name, descriptor, methodVertex);
					}

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						onMethodCalled(jvmClass, methodVertex, opcode, owner, name, descriptor, isInterface);
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
	 * Called from the {@link MethodVisitor} in {@link #visit(JvmClassInfo)} when a method is visited.
	 * <p>
	 * This method ensures that {@link #unresolvedReferences unresolved references} are marked as resolved
	 * and the {@link MethodVertex} model is updated when the given method details match a previously
	 * unresolved declaration.
	 *
	 * @param owner
	 * 		Owner type of the previously unresolved method call.
	 * @param name
	 * 		Previously unresolved method name.
	 * @param descriptor
	 * 		Previously unresolved method descriptor.
	 * @param methodVertex
	 * 		Vertex for the previously unresolved method.
	 */
	private void linkedResolvedCalls(@Nonnull JvmClassInfo owner,
	                                 @Nonnull String name,
	                                 @Nonnull String descriptor,
	                                 @Nonnull MutableMethodVertex methodVertex) {
		// Skip if the unresolved references map does not contain the given class
		// or if the set of unresolved calls to this class is empty.
		String ownerName = owner.getName();
		if (!unresolvedReferences.containsKey(ownerName))
			return; // The 'get' does a compute-if-absent, so the contains-key saves us allocations.
		Set<CallingContext> unresolvedCallsToThisClass = unresolvedReferences.get(ownerName);
		if (unresolvedCallsToThisClass.isEmpty())
			return;

		// For each calling context that refers to this class, resolve the reference to this method (method params).
		for (CallingContext context : unresolvedCallsToThisClass) {
			MethodRef callingMethod = context.callingMethod();
			ClassMethodsContainer callingContainer = getClassMethodsContainer(context.callingClass());
			MutableMethodVertex callingVertex = (MutableMethodVertex) callingContainer.getVertex(callingMethod.name(), callingMethod.desc());
			if (callingVertex == null)
				continue;
			onMethodCalled(context.callingClass, callingVertex, context.opcode, ownerName, name, descriptor, context.itf);
		}
	}


	/**
	 * Called from the {@link ClassReader} in {@link #visit(JvmClassInfo)}.
	 * Links the given vertex to the remote {@link MethodVertex} of the resolved method call,
	 * if resolution is a success.
	 * <p>
	 * When not successful, the call is recorded as an unresolved reference in two directions.
	 * <ul>
	 *     <li>{@link #unresolvedDeclarations Class names --> Missing method declarations}</li>
	 *     <li>{@link #unresolvedReferences  Class names --> Methods that have calls to missing method declarations</li>
	 * </ul>
	 *
	 * @param callingClass
	 * 		The class that defines the calling method.
	 * @param callingVertex
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
	private void onMethodCalled(@Nonnull JvmClassInfo callingClass, @Nonnull MutableMethodVertex callingVertex,
	                            int opcode, @Nonnull String owner, @Nonnull String name,
	                            @Nonnull String descriptor, boolean isInterface) {
		MethodRef ref = new MethodRef(owner, name, descriptor);

		// Resolve the method
		Result<Resolution<JvmClassInfo, MethodMember>> resolutionResult = resolve(opcode, owner, name, descriptor, isInterface);

		// Handle result
		CallingContext callContext = new CallingContext(callingClass, callingVertex.getMethod(), opcode, isInterface);
		if (resolutionResult.isSuccess()) {
			// Extract vertex from resolution
			Resolution<JvmClassInfo, MethodMember> resolution = resolutionResult.value();
			ClassMethodsContainer resolvedClass = getClassMethodsContainer(resolution.owner().innerValue());
			MutableMethodVertex resolvedMethodCallVertex = (MutableMethodVertex) resolvedClass.getVertex(resolution.member().innerValue());

			// Link the vertices
			callingVertex.getCalls().add(resolvedMethodCallVertex);
			resolvedMethodCallVertex.getCallers().add(callingVertex);

			// Remove tracked unresolved call if any exist
			Set<MethodRef> unresolvedWithinOwner = unresolvedDeclarations.get(owner);
			if (unresolvedWithinOwner.remove(ref)) {
				if (unresolvedWithinOwner.isEmpty()) unresolvedDeclarations.remove(owner);
				logger.debugging(l -> l.info("Satisfy unresolved call {}", ref));
			}

			// Remove tracking of unresolved declarations/references
			boolean resolvedPriorUnknown = false;
			resolvedPriorUnknown |= unresolvedDeclarations.remove(owner, ref);
			resolvedPriorUnknown |= unresolvedReferences.remove(owner, callContext);
			if (resolvedPriorUnknown) logger.debugging(l -> l.warn("Found previous unresolved reference: {}", ref));
		} else {
			unresolvedDeclarations.put(owner, ref);
			unresolvedReferences.put(owner, callContext);
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
	 * The result {@link Result#isError()} will be {@code true} when the {@code owner} could not be found.
	 */
	@Nonnull
	public Result<Resolution<JvmClassInfo, MethodMember>> resolve(int opcode, @Nonnull String owner, @Nonnull String name,
	                                                              @Nonnull String descriptor, boolean isInterface) {
		JvmClassInfo ownerClass = lookup.apply(owner);

		// Skip if we cannot resolve owner
		if (ownerClass == null) {
			unresolvedDeclarations.put(owner, new MethodRef(owner, name, descriptor));
			return Result.error(ResolutionError.NO_SUCH_METHOD);
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
		Set<MethodRef> unresolvedWithinOwner = unresolvedDeclarations.get(cls.getName());
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

	/**
	 * @return Map of classes that could not be resolved, to method declarations observed being made to them.
	 */
	@Nonnull
	@VisibleForTesting
	MultiMap<String, MethodRef, Set<MethodRef>> getUnresolvedDeclarations() {
		return unresolvedDeclarations;
	}

	/**
	 * Models the calling context to some method.
	 *
	 * @param callingClass
	 * 		Class that defines the calling method.
	 * @param callingMethod
	 * 		The calling method information.
	 * @param opcode
	 * 		The outgoing call opcode.
	 * @param itf
	 * 		The outgoing call {@code isInterface} flag.
	 */
	private record CallingContext(@Nonnull JvmClassInfo callingClass, @Nonnull MethodRef callingMethod,
	                              int opcode, boolean itf) {
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			// We only care about the reference equality
			CallingContext other = (CallingContext) o;
			return callingMethod.equals(other.callingMethod);
		}

		@Override
		public int hashCode() {
			// We only care about the reference hash
			return callingMethod.hashCode();
		}
	}

	/**
	 * Mutable impl of {@link MethodVertex}.
	 */
	static class MutableMethodVertex implements MethodVertex {
		private final Set<MethodVertex> callers = Collections.synchronizedSet(new HashSet<>());
		private final Set<MethodVertex> calls = Collections.synchronizedSet(new HashSet<>());
		private final MethodRef method;
		private final MethodMember resolvedMethod;

		MutableMethodVertex(@Nonnull MethodRef method, @Nonnull MethodMember resolvedMethod) {
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
