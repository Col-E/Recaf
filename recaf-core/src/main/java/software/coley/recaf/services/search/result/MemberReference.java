package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;

/**
 * Member reference outline.
 *
 * @param owner
 * 		Name of class declaring the member.
 * @param name
 * 		Member name.
 * @param desc
 * 		Member descriptor.
 *
 * @author Matt Coley
 */
public record MemberReference(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
	/**
	 * @return {@code true} when this is a reference to a field member.
	 */
	public boolean isFieldReference() {
		return !isMethodReference();
	}

	/**
	 * @return {@code true} when this is a reference to a method member.
	 */
	public boolean isMethodReference() {
		return desc.charAt(0) == '(';
	}
}