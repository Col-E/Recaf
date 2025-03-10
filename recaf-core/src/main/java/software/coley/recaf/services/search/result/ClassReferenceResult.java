package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a class reference match.
 *
 * @author Matt Coley
 */
public class ClassReferenceResult extends Result<ClassReference> {
	private final ClassReference ref;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param name
	 * 		Class name.
	 */
	public ClassReferenceResult(@Nonnull PathNode<?> path, @Nonnull String name) {
		this(path, new ClassReference(name));
	}

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param ref
	 * 		Class reference.
	 */
	public ClassReferenceResult(@Nonnull PathNode<?> path, @Nonnull ClassReference ref) {
		super(path);
		this.ref = ref;
	}

	@Nonnull
	@Override
	protected ClassReference getValue() {
		return ref;
	}
}
