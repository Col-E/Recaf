package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for holding a number of class transformers and shared state for transformation.
 *
 * @author Matt Coley
 */
public abstract class AbstractTransformerContext<T extends ClassTransformer> {
	private final Set<String> classesToRemove = ConcurrentHashMap.newKeySet();
	private final ThreadLocal<Boolean> transformerDidWork = ThreadLocal.withInitial(() -> false);
	private final Map<Class<? extends T>, T> transformerMap;
	protected final Workspace workspace;
	protected final WorkspaceResource resource;

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
	@SafeVarargs
	public AbstractTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull T... transformers) {
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
	public AbstractTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull Collection<? extends T> transformers) {
		this.transformerMap = buildMap(transformers);
		this.workspace = workspace;
		this.resource = resource;
	}

	/**
	 * @return Workspace containing the classes to transform.
	 */
	@Nonnull
	public Workspace getWorkspace() {
		return workspace;
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
	 * Marks that the current transformer being run with this context did work.
	 */
	protected void recordDidWork() {
		transformerDidWork.set(true);
	}

	/**
	 * Used to check if a {@link ClassTransformer} did work after its {@code transform} has been executed with
	 * this context being used as a parameter.
	 *
	 * @return {@code true} if the last transformer ran did work with this context.
	 */
	protected boolean didTransformerDoWork() {
		return transformerDidWork.get();
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
		transformerDidWork.set(false);
	}

	/**
	 * Get the {@link JvmClassTransformer} instance associated with this context, or throw an exception if no such
	 * transformer is registered. If you are looking for an optional lookup use: {@link #getOptionalTransformer(Class)}.
	 *
	 * @param key
	 * 		Transformer class.
	 * @param <CT>
	 * 		Transformer type.
	 *
	 * @return Shared instance of the transformer within this context.
	 *
	 * @throws TransformationException
	 * 		When the transformer was not found within this context.
	 */
	@Nonnull
	public <CT extends ClassTransformer> CT getTransformer(Class<CT> key) throws TransformationException {
		CT transformer = getOptionalTransformer(key);
		if (transformer == null)
			throw new TransformationException("Transformation context attempted lookup of class '"
					+ key.getSimpleName() + "' but did not have an associated entry");
		return transformer;
	}

	/**
	 * Get the {@link ClassTransformer} instance associated with this context, if it is registered.
	 *
	 * @param key
	 * 		Transformer class.
	 * @param <CT>
	 * 		Transformer type.
	 *
	 * @return Shared instance of the transformer within this context,
	 * or {@code null} if no such transformer is registered to this context.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <CT extends ClassTransformer> CT getOptionalTransformer(@Nonnull Class<CT> key) {
		// NOTE: Any Recaf-defined transformer must be @Dependent so that CDI doesn't give you proxy wrappers
		// of the class. Our map is identity based, and if you do 'get(MyClass.class)' and we end up storing the
		// proxy wrapper, then the lookup will fail even though the transformer is seemingly registered.
		ClassTransformer transformer = transformerMap.get(key);
		if (transformer == null)
			return null;
		return (CT) transformer;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private static <T extends ClassTransformer> Map<Class< ? extends T>, T> buildMap(@Nonnull Collection<? extends T> transformers) {
		Map<Class<? extends T>, T> map = new IdentityHashMap<>();
		for (T transformer : transformers)
			map.put((Class<? extends T>) transformer.getClass(), transformer);
		return Collections.unmodifiableMap(map);
	}
}
