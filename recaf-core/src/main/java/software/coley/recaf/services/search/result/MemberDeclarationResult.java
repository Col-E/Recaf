package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a member declaration match.
 *
 * @author Matt Coley
 */
public class MemberDeclarationResult extends Result<MemberDeclaration> {
	private final MemberDeclaration declaration;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param declaration
	 * 		Matched member declaration.
	 */
	public MemberDeclarationResult(@Nonnull PathNode<?> path, @Nonnull MemberDeclaration declaration) {
		super(path);
		this.declaration = declaration;
	}

	@Nonnull
	@Override
	public MemberDeclaration getValue() {
		return declaration;
	}
}
