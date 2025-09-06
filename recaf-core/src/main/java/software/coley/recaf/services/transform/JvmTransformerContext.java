package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.util.analysis.ReAnalyzer;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.lookup.BasicGetStaticLookup;
import software.coley.recaf.util.analysis.lookup.BasicInvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.BasicInvokeVirtualLookup;
import software.coley.recaf.util.analysis.lookup.GetFieldLookup;
import software.coley.recaf.util.analysis.lookup.GetStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.visitors.FrameSkippingVisitor;
import software.coley.recaf.util.visitors.WorkspaceClassWriter;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Context for holding a number of JVM class transformers and shared state for transformation.
 *
 * @author Matt Coley
 */
public class JvmTransformerContext {
	private final Map<Class<? extends JvmClassTransformer>, JvmClassTransformer> transformerMap;
	private final AggregatedMappings mappings;
	private final Set<String> classesToRemove = new HashSet<>();
	private final Map<String, JvmClassData> classData = new ConcurrentHashMap<>();
	private final Set<String> recomputeFrameClasses = new HashSet<>();
	private final Workspace workspace;
	private final WorkspaceResource resource;
	private Supplier<GetFieldLookup> getFieldLookupSupplier = () -> null;
	private Supplier<GetStaticLookup> getStaticLookupSupplier = BasicGetStaticLookup::new;
	private Supplier<InvokeVirtualLookup> invokeVirtualLookupSupplier = BasicInvokeVirtualLookup::new;
	private Supplier<InvokeStaticLookup> invokeStaticLookupSupplier = BasicInvokeStaticLookup::new;
	private boolean transformerDidWork;

	/**
	 * Constructs a new context from an array of transformers.
	 *
	 * @param workspace
	 * 		Workspace containing the classes to transform.
	 * @param resource
	 * 		Resource in the workspace containing classes to transform. Should always be the {@link Workspace#getPrimaryResource()}.
	 * @param transformers
	 * 		Transformers to associate with this context.
	 */
	public JvmTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull JvmClassTransformer... transformers) {
		this(workspace, resource, Arrays.asList(transformers));
	}

	/**
	 * Constructs a new context from a collection of transformers.
	 *
	 * @param workspace
	 * 		Workspace containing the classes to transform.
	 * @param resource
	 * 		Resource in the workspace containing classes to transform. Should always be the {@link Workspace#getPrimaryResource()}.
	 * @param transformers
	 * 		Transformers to associate with this context.
	 */
	public JvmTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull Collection<? extends JvmClassTransformer> transformers) {
		this.transformerMap = buildMap(transformers);
		this.workspace = workspace;
		this.resource = resource;

		// We will use aggregated mappings for the reverse-mapping utility it offers.
		// Some transformers that aim to provide mappings will find this very handy.
		mappings = new AggregatedMappings(workspace);
	}

	/**
	 * Builds the map of initial transformed class paths to their final transformed states.
	 * <br>
	 * The map keys are existing workspace paths the respective classes.
	 * <br>
	 * The map values are classes post-transformation, without any mappings applied.
	 *
	 * @param inheritanceGraph
	 * 		Inheritance graph tied to the workspace the transformed classes belong to.
	 *
	 * @throws TransformationException
	 * 		When the classes cannot be written back to {@code byte[]} likely due to frame computation problems.
	 */
	@Nonnull
	protected Map<ClassPathNode, JvmClassInfo> buildChangeMap(@Nonnull InheritanceGraph inheritanceGraph) throws TransformationException {
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		Map<ClassPathNode, JvmClassInfo> map = new IdentityHashMap<>();
		for (JvmClassData data : classData.values()) {
			if (data.isDirty()) {
				if (data.node != null) {
					// Emit bytecode from the current node
					boolean recompute = recomputeFrameClasses.contains(data.node.name);
					int flags = recompute ? ClassWriter.COMPUTE_FRAMES : 0;
					ClassReader reader = data.initialClass.getClassReader();
					ClassWriter writer = new WorkspaceClassWriter(inheritanceGraph, reader, flags);
					try {
						if (recompute)
							data.node.accept(new FrameSkippingVisitor(writer));
						else
							data.node.accept(writer);

						// Update output map
						byte[] modifiedBytes = writer.toByteArray();
						JvmClassInfo modifiedClass = data.initialClass.toJvmClassBuilder()
								.adaptFrom(modifiedBytes)
								.build();
						ClassPathNode classPath = resourcePath.child(data.bundle)
								.child(modifiedClass.getPackageName())
								.child(modifiedClass);
						map.put(classPath, modifiedClass);
					} catch (Throwable t) {
						throw new TransformationException("ClassNode --> byte[] failed for class '" + data.node.name + "'", t);
					}
				} else {
					// Update output map if the bytecode is not the same as the initial state
					byte[] bytecode = data.getBytecode();
					if (!Arrays.equals(bytecode, data.initialClass.getBytecode())) {
						JvmClassInfo modifiedClass = data.initialClass.toJvmClassBuilder()
								.adaptFrom(bytecode)
								.build();
						ClassPathNode classPath = resourcePath.child(data.bundle)
								.child(modifiedClass.getPackageName())
								.child(modifiedClass);
						map.put(classPath, modifiedClass);
					}
				}
			}
		}
		return map;
	}

	/**
	 * @param inheritanceGraph
	 * 		Inheritance graph of workspace.
	 * @param cls
	 * 		Name of class defining a method to analyze.
	 * @param method
	 * 		Method to analyze.
	 *
	 * @return Analyzed frames of the given method.
	 *
	 * @throws TransformationException
	 * 		When the analyzer throws an exception when computing the frames of the given method.
	 */
	@Nonnull
	public Frame<ReValue>[] analyze(@Nonnull InheritanceGraph inheritanceGraph,
	                                @Nonnull ClassNode cls,
	                                @Nonnull MethodNode method) throws TransformationException {
		try {
			ReAnalyzer analyzer = newAnalyzer(inheritanceGraph, cls, method);
			return analyzer.analyze(cls.name, method);
		} catch (Throwable t) {
			throw new TransformationException("Error encountered when computing method frames", t);
		}
	}

	/**
	 * @param inheritanceGraph
	 * 		Inheritance graph of workspace.
	 * @param cls
	 * 		Name of class defining a method to analyze.
	 * @param method
	 * 		Method to analyze.
	 *
	 * @return An analyzer for the given method.
	 */
	@Nonnull
	public ReAnalyzer newAnalyzer(@Nonnull InheritanceGraph inheritanceGraph,
	                              @Nonnull ClassNode cls,
	                              @Nonnull MethodNode method) {
		ReInterpreter interpreter = newInterpreter(inheritanceGraph);
		return new ReAnalyzer(interpreter);
	}

	/**
	 * @param inheritanceGraph
	 * 		Inheritance graph of workspace.
	 *
	 * @return An interpreter for handling instruction execution.
	 */
	@Nonnull
	public ReInterpreter newInterpreter(@Nonnull InheritanceGraph inheritanceGraph) {
		ReInterpreter interpreter = new ReInterpreter(inheritanceGraph);
		interpreter.setGetFieldLookup(getFieldLookupSupplier.get());
		interpreter.setGetStaticLookup(getStaticLookupSupplier.get());
		interpreter.setInvokeVirtualLookup(invokeVirtualLookupSupplier.get());
		interpreter.setInvokeStaticLookup(invokeStaticLookupSupplier.get());
		return interpreter;
	}

	/**
	 * Utility to invoke {@link DeadCodeRemovingTransformer} for a given method.
	 * Requires the transformer to be provided to this context.
	 *
	 * @param declaringClass
	 * 		Class declaring the method to clean up.
	 * @param method
	 * 		Method with dead code to remove.
	 *
	 * @return {@code true} when there were changes as a result of dead code removal.
	 * {@code false} for no changes being made to the passed method.
	 *
	 * @throws TransformationException
	 * 		When the {@link DeadCodeRemovingTransformer} was not provided to this context,
	 * 		or if dead code removal encountered an error.
	 */
	public boolean pruneDeadCode(@Nonnull ClassNode declaringClass, @Nonnull MethodNode method) throws TransformationException {
		return getJvmTransformer(DeadCodeRemovingTransformer.class).prune(declaringClass, method);
	}

	/**
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 *
	 * @return {@code true} when the context currently has the class represented as a node <i>(vs raw {@code byte[]})</i.>
	 */
	public boolean isNode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
		JvmClassData data = getJvmClassData(bundle, info);
		return data.node != null;
	}

	/**
	 * Gets the current ASM node representation of the given class.
	 * <p/>
	 * Transformers can update the <i>"current"</i> state of the node via
	 * {@link #setBytecode(JvmClassBundle, JvmClassInfo, byte[])} or
	 * {@link #setNode(JvmClassBundle, JvmClassInfo, ClassNode)}.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 *
	 * @return The current tracked/transformed {@link ClassNode} for the associated class.
	 *
	 * @see #setNode(JvmClassBundle, JvmClassInfo, ClassNode)
	 */
	@Nonnull
	public ClassNode getNode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
		return getJvmClassData(bundle, info).getOrCreateNode();
	}

	/**
	 * Gets the current bytecode of the given class.
	 * <p/>
	 * Transformers can update the <i>"current"</i> state of the bytecode via
	 * {@link #setBytecode(JvmClassBundle, JvmClassInfo, byte[])} or
	 * {@link #setNode(JvmClassBundle, JvmClassInfo, ClassNode)}.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 *
	 * @return The current tracked/transformed bytecode for the associated class.
	 *
	 * @see #setBytecode(JvmClassBundle, JvmClassInfo, byte[])
	 */
	@Nonnull
	public byte[] getBytecode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
		return getJvmClassData(bundle, info).getBytecode();
	}

	/**
	 * Updates the transformed state of a class by recording an ASM node representation of the class.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 * @param node
	 * 		ASM node representation of the class to store.
	 *
	 * @see #getNode(JvmClassBundle, JvmClassInfo)
	 */
	public void setNode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info, @Nonnull ClassNode node) {
		transformerDidWork = true;
		getJvmClassData(bundle, info).setNode(node);
	}

	/**
	 * Updates the transformed state of a class by recording new bytecode of the class.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 * 		This does not need to reflect the updated state of the bytecode, it is strictly used for keying/lookups.
	 * @param bytecode
	 * 		Bytecode of the class to store.
	 *
	 * @see #getBytecode(JvmClassBundle, JvmClassInfo)
	 */
	public void setBytecode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info, @Nonnull byte[] bytecode) {
		transformerDidWork = true;
		getJvmClassData(bundle, info).setBytecode(bytecode);
	}

	/**
	 * Marks a class for removal in the workspace.
	 *
	 * @param info
	 * 		The class model in the workspace.
	 *
	 * @see #getClassesToRemove()
	 */
	public void markClassForRemoval(@Nonnull JvmClassInfo info) {
		markClassForRemoval(info.getName());
	}

	/**
	 * Marks a class for removal in the workspace.
	 *
	 * @param name
	 * 		Internal class name.
	 *
	 * @see #getClassesToRemove()
	 */
	public void markClassForRemoval(@Nonnull String name) {
		classesToRemove.add(name);
	}

	/**
	 * @return Names of classes marked for removal.
	 *
	 * @see #markClassForRemoval(JvmClassInfo)
	 */
	@Nonnull
	public Set<String> getClassesToRemove() {
		return Collections.unmodifiableSet(classesToRemove);
	}

	/**
	 * Clears any transformations applied to the given class.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 */
	public void clear(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
		JvmClassData data = getJvmClassData(bundle, info);
		data.setBytecode(data.initialClass.getBytecode());
		data.dirty = false;
	}

	/**
	 * Called by transformers that have more thorough changes applied to classes that likely violate existing frames.
	 *
	 * @param className
	 * 		Name of class to recompute frames for when building the change map.
	 */
	public void setRecomputeFrames(@Nonnull String className) {
		recomputeFrameClasses.add(className);
	}

	/**
	 * Transformers that aim to rename classes, fields, and methods should register the desired mappings
	 * here, and they will be applied after all other transformations are applied.
	 *
	 * @return Mappings to apply upon transformation completion.
	 */
	@Nonnull
	public AggregatedMappings getMappings() {
		return mappings;
	}

	/**
	 * Get the {@link JvmClassTransformer} instance associated with this context, or throw an exception if no such
	 * transformer is registered. If you are looking for an optional lookup use: {@link #getOptionalJvmTransformer(Class)}.
	 *
	 * @param key
	 * 		Transformer class.
	 * @param <T>
	 * 		Transformer type.
	 *
	 * @return Shared instance of the transformer within this context.
	 *
	 * @throws TransformationException
	 * 		When the transformer was not found within this context.
	 */
	@Nonnull
	public <T extends JvmClassTransformer> T getJvmTransformer(Class<T> key) throws TransformationException {
		T transformer = getOptionalJvmTransformer(key);
		if (transformer == null)
			throw new TransformationException("Transformation context attempted lookup of class '"
					+ key.getSimpleName() + "' but did not have an associated entry");
		return transformer;
	}

	/**
	 * Get the {@link JvmClassTransformer} instance associated with this context, if it is registered.
	 *
	 * @param key
	 * 		Transformer class.
	 * @param <T>
	 * 		Transformer type.
	 *
	 * @return Shared instance of the transformer within this context,
	 * or {@code null} if no such transformer is registered to this context.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T extends JvmClassTransformer> T getOptionalJvmTransformer(Class<T> key) {
		// NOTE: Any Recaf-defined transformer must be @Dependent so that CDI doesn't give you proxy wrappers
		// of the class. Our map is identity based, and if you do 'get(MyClass.class)' and we end up storing the
		// proxy wrapper, then the lookup will fail even though the transformer is seemingly registered.
		JvmClassTransformer transformer = transformerMap.get(key);
		if (transformer == null)
			return null;
		return (T) transformer;
	}

	/**
	 * @param supplier
	 * 		Supplier for {@link GetFieldLookup} to be used with {@link #newAnalyzer(InheritanceGraph, ClassNode, MethodNode)}.
	 */
	public void setGetFieldLookupSupplier(@Nullable Supplier<GetFieldLookup> supplier) {
		if (supplier == null)
			supplier = () -> null;
		getFieldLookupSupplier = supplier;
	}

	/**
	 * @param supplier
	 * 		Supplier for {@link GetStaticLookup} to be used with {@link #newAnalyzer(InheritanceGraph, ClassNode, MethodNode)}.
	 */
	public void setGetStaticLookupSupplier(@Nullable Supplier<GetStaticLookup> supplier) {
		if (supplier == null)
			supplier = () -> null;
		getStaticLookupSupplier = supplier;
	}

	/**
	 * @param supplier
	 * 		Supplier for {@link InvokeVirtualLookup} to be used with {@link #newAnalyzer(InheritanceGraph, ClassNode, MethodNode)}.
	 */
	public void setInvokeVirtualLookupSupplier(@Nullable Supplier<InvokeVirtualLookup> supplier) {
		if (supplier == null)
			supplier = () -> null;
		invokeVirtualLookupSupplier = supplier;
	}

	/**
	 * @param supplier
	 * 		Supplier for {@link InvokeStaticLookup} to be used with {@link #newAnalyzer(InheritanceGraph, ClassNode, MethodNode)}.
	 */
	public void setInvokeStaticLookupSupplier(@Nullable Supplier<InvokeStaticLookup> supplier) {
		if (supplier == null)
			supplier = () -> null;
		invokeStaticLookupSupplier = supplier;
	}

	/**
	 * Called before any transformer operates with this context.
	 * <br>
	 * Clears any state associated with the operation of transformers.
	 *
	 * @see #didTransformerDoWork()
	 */
	protected void resetTransformerTracking() {
		// Any transformation application should call this before the transformer methods operate on data.
		transformerDidWork = false;
	}

	/**
	 * Used to check if a {@link ClassTransformer} did work after its {@code transform} has been executed with
	 * this context being used as a parameter.
	 *
	 * @return {@code true} if the last transformer ran did work with this context.
	 */
	protected boolean didTransformerDoWork() {
		return transformerDidWork;
	}

	@Nonnull
	private JvmClassData getJvmClassData(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info) {
		return classData.computeIfAbsent(info.getName(), ignored -> new JvmClassData(bundle, info));
	}

	@Nonnull
	private static Map<Class<? extends JvmClassTransformer>, JvmClassTransformer> buildMap(@Nonnull Collection<? extends JvmClassTransformer> transformers) {
		Map<Class<? extends JvmClassTransformer>, JvmClassTransformer> map = new IdentityHashMap<>();
		for (JvmClassTransformer transformer : transformers)
			map.put(transformer.getClass(), transformer);
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Container of per-class transformation state.
	 */
	private static class JvmClassData {
		private final JvmClassBundle bundle;
		private final JvmClassInfo initialClass;
		private volatile byte[] bytecode;
		private volatile ClassNode node;
		private boolean dirty;

		/**
		 * @param bundle
		 * 		Bundle containing the class.
		 * @param initialClass
		 * 		Initial state of the class before transformation.
		 */
		public JvmClassData(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo initialClass) {
			this.initialClass = initialClass;
			this.bundle = bundle;
			bytecode = initialClass.getBytecode();
		}

		/**
		 * @return Node representation of the {@link #getBytecode() current bytecode}.
		 */
		@Nonnull
		public ClassNode getOrCreateNode() {
			if (node == null) {
				synchronized (this) {
					if (node == null) {
						node = new ClassNode();
						new ClassReader(bytecode).accept(node, 0);
					}
				}
			}

			// We always give back a copy so actions taken on this node are not affecting the cached instance
			// unless a transformer explicitly commits the change.
			ClassNode nodeCopy = new ClassNode();
			node.accept(nodeCopy);
			return nodeCopy;
		}

		/**
		 * @return Current bytecode of the class.
		 */
		@Nonnull
		public byte[] getBytecode() {
			if (bytecode == null) {
				synchronized (this) {
					if (bytecode == null) {
						ClassWriter writer = new ClassWriter(0);
						node.accept(writer);
						bytecode = writer.toByteArray();
					}
				}
			}
			return bytecode;
		}

		/**
		 * @param node
		 * 		Current node representation to set for this class.
		 */
		public void setNode(@Nonnull ClassNode node) {
			synchronized (this) {
				this.node = node;
				bytecode = null; // Invalidate bytecode state
				dirty = true;
			}
		}

		/**
		 * @param bytecode
		 * 		Current bytecode to set for this class.
		 */
		public void setBytecode(@Nonnull byte[] bytecode) {
			synchronized (this) {
				this.bytecode = bytecode;
				node = null; // Invalidate node state
				dirty = true;
			}
		}

		/**
		 * @return {@code true} when changes have been applied to this class.
		 */
		public boolean isDirty() {
			return dirty;
		}
	}
}
