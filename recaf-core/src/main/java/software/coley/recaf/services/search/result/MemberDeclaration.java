package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Accessed;

/**
 * Member declaration outline.
 *
 * @param owner
 * 		Name of class declaring the member.
 * @param name
 * 		Member name.
 * @param desc
 * 		Member descriptor.
 * @param access
 * 		Member access flags.
 *
 * @author Matt Coley
 */
public record MemberDeclaration(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, int access) implements Accessed {
	/**
	 * @return {@code true} when this is a declaration of a field member.
	 */
	public boolean isFieldDeclaration() {
		return !isMethodDeclaration();
	}

	/**
	 * @return {@code true} when this is a declaration of a method member.
	 */
	public boolean isMethodDeclaration() {
		return desc.charAt(0) == '(';
	}

	@Override
	public int getAccess() {
		return access;
	}
}
