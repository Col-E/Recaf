package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Accessed;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Named;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.properties.PropertyContainer;

/**
 * Component of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public interface ClassMember extends PropertyContainer, Annotated, Accessed, Named {
	/**
	 * @return Member name.
	 */
	@Nonnull
	String getName();

	/**
	 * @return Member descriptor.
	 */
	@Nonnull
	String getDescriptor();

	/**
	 * @return Signature containing generic information. May be {@code null}.
	 */
	@Nullable
	String getSignature();

	/**
	 * @return The class declaring this member.
	 * May be {@code null} if this information is not known.
	 */
	@Nullable
	default ClassInfo getDeclaringClass() {
		// Not required to be implemented.
		return null;
	}

	/**
	 * @return {@code true} when the member is aware of its declaring {@link ClassInfo}
	 */
	default boolean isDeclarationAware() {
		return getDeclaringClass() != null;
	}

	/**
	 * @return {@code true} when the member is a field.
	 */
	boolean isField();

	/**
	 * @return {@code true} when the member is a method.
	 */
	boolean isMethod();
}
