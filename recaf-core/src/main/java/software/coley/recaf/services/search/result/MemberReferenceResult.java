package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a class reference match.
 *
 * @author Matt Coley
 */
public class MemberReferenceResult extends Result<MemberReference> {
	private final MemberReference ref;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param owner
	 * 		Name of class declaring the member.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public MemberReferenceResult(@Nonnull PathNode<?> path,
	                             @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		this(path, new MemberReference(owner, name, desc));
	}

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param ref
	 * 		Member reference.
	 */
	public MemberReferenceResult(@Nonnull PathNode<?> path, @Nonnull MemberReference ref) {
		super(path);
		this.ref = ref;
	}

	@Nonnull
	@Override
	protected MemberReference getValue() {
		return ref;
	}
}
