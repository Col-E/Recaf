package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

/**
 * Outlines base transformation information such as the name and list of any dependencies.
 *
 * @author Matt Coley
 */
public interface ClassTransformer {
	/**
	 * @return Name of the transformer.
	 */
	@Nonnull
	String name();

	/**
	 * @return Set of transformer classes that must run before this one.
	 */
	@Nonnull
	default Set<Class<? extends ClassTransformer>> dependencies() {
		return Collections.emptySet();
	}
}
