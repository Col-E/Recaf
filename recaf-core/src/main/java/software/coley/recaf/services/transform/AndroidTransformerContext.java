package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.ClassDefinition;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Android-specific transformer-context implementation.
 *
 * @author Matt Coley
 */
public class AndroidTransformerContext extends AbstractTransformerContext<AndroidClassTransformer> {
	private final Map<String, ClassDefinition> classDefinitions = new ConcurrentHashMap<>();

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
	public AndroidTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull AndroidClassTransformer... transformers) {
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
	public AndroidTransformerContext(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull Collection<? extends AndroidClassTransformer> transformers) {
		super(workspace, resource, transformers);
	}

	/**
	 * Gets the current definition representation of the given class.
	 * <p>
	 * Transformers can update the <i>"current"</i> state of the definition
	 * via {@link #setDefinition(AndroidClassInfo, ClassDefinition)}.
	 *
	 * @param classInfo
	 * 		The class's model in the workspace.
	 *
	 * @return The current tracked/transformed {@link ClassDefinition} for the associated class.
	 *
	 * @see #setDefinition(AndroidClassInfo, ClassDefinition)
	 */
	@Nonnull
	public ClassDefinition getDefinition(@Nonnull AndroidClassInfo classInfo) {
		ClassDefinition definition = classDefinitions.get(classInfo.getName());
		return Objects.requireNonNullElse(definition, classInfo.getBackingDefinition());
	}

	/**
	 * Updates the transformed state of a class by recording a new definition for the class.
	 *
	 * @param classInfo
	 * 		The class's model in the workspace.
	 * @param definition
	 * 		Class definition to store.
	 *
	 * @see #getDefinition(AndroidClassInfo)
	 */
	public void setDefinition(@Nonnull AndroidClassInfo classInfo,
	                          @Nonnull ClassDefinition definition) {
		recordDidWork();
		classDefinitions.put(classInfo.getName(), definition);
	}
}
