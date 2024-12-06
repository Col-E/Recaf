package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.visitors.WorkspaceClassWriter;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for holding a number of JVM class transformers and shared state for transformation.
 *
 * @author Matt Coley
 */
public class JvmTransformerContext {
	private final Map<Class<? extends JvmClassTransformer>, JvmClassTransformer> transformerMap;
	private final Map<String, JvmClassData> classData = new ConcurrentHashMap<>();
	private final Workspace workspace;
	private final WorkspaceResource resource;

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
	}

	/**
	 * Builds the map of initial transformed class paths to their final transformed states.
	 *
	 * @param inheritanceGraph
	 * 		Inheritance graph tied to the workspace the transformed classes belong to.
	 */
	@Nonnull
	protected Map<ClassPathNode, JvmClassInfo> buildChangeMap(@Nonnull InheritanceGraph inheritanceGraph) {
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		Map<ClassPathNode, JvmClassInfo> map = new HashMap<>();
		for (JvmClassData data : classData.values()) {
			if (data.isDirty()) {
				if (data.node != null) {
					// Emit bytecode from the current node
					ClassWriter writer = new WorkspaceClassWriter(inheritanceGraph, data.initialClass.getClassReader(), 0);
					data.node.accept(writer);
					byte[] modifiedBytes = writer.toByteArray();

					// Update output map
					JvmClassInfo modifiedClass = data.initialClass.toJvmClassBuilder()
							.adaptFrom(modifiedBytes)
							.build();
					ClassPathNode classPath = resourcePath.child(data.bundle)
							.child(modifiedClass.getPackageName())
							.child(modifiedClass);
					map.put(classPath, modifiedClass);
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
	 * Gets the current ASM node representation of the given class.
	 * Transformers can update the <i>"current"</i> state of the node via
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
	 * Transformers can update the <i>"current"</i> state of the bytecode via
	 * {@link #setBytecode(JvmClassBundle, JvmClassInfo, byte[])}.
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
	 * Once the transformation application is completed, the latest value recorded here
	 * <i>(or in {@link #setBytecode(JvmClassBundle, JvmClassInfo, byte[])}</i> will be dumped into the workspace.
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
		getJvmClassData(bundle, info).setNode(node);
	}

	/**
	 * Updates the transformed state of a class by recording new bytecode of the class.
	 * Once the transformation application is completed, the latest value recorded here
	 * <i>(or in {@link #setNode(JvmClassBundle, JvmClassInfo, ClassNode)})</i> will be dumped into the workspace.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		The class's model in the workspace.
	 * @param bytecode
	 * 		Bytecode of the class to store.
	 *
	 * @see #getBytecode(JvmClassBundle, JvmClassInfo)
	 */
	public void setBytecode(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info, @Nonnull byte[] bytecode) {
		getJvmClassData(bundle, info).setBytecode(bytecode);
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
	@SuppressWarnings("unchecked")
	public <T extends JvmClassTransformer> T getJvmTransformer(Class<T> key) throws TransformationException {
		JvmClassTransformer transformer = transformerMap.get(key);
		if (transformer == null)
			throw new TransformationException("Transformation context attempted lookup of class '"
					+ key.getSimpleName() + "' but did not have an associated entry");
		return (T) transformer;
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
		private byte[] bytecode;
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
			return node;
		}

		/**
		 * The current bytecode of the class as set by {@link JvmTransformerContext#setBytecode(JvmClassBundle, JvmClassInfo, byte[])}.
		 * This value does not update when using {@link JvmTransformerContext#setNode(JvmClassBundle, JvmClassInfo, ClassNode)}.
		 *
		 * @return Current bytecode of the class.
		 */
		@Nonnull
		public byte[] getBytecode() {
			return bytecode;
		}

		/**
		 * @param node
		 * 		Current node representation to set for this class.
		 */
		public void setNode(@Nonnull ClassNode node) {
			this.node = node;
			dirty = true;
		}

		/**
		 * @param bytecode
		 * 		Current bytecode to set for this class.
		 */
		public void setBytecode(@Nonnull byte[] bytecode) {
			this.bytecode = bytecode;
			node = null; // Invalidate node state
			dirty = true;
		}

		/**
		 * @return {@code true} when changes have been applied to this class.
		 */
		public boolean isDirty() {
			return dirty;
		}
	}
}
