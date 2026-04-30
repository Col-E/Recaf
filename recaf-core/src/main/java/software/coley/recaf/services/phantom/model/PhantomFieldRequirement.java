package software.coley.recaf.services.phantom.model;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Required field shape for a generated phantom class.
 *
 * @author Matt Coley
 */
public class PhantomFieldRequirement {
	private final String name;
	private final String descriptor;
	private boolean isStatic;

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param isStatic
	 * 		Whether the field must be generated as static.
	 */
	public PhantomFieldRequirement(@Nonnull String name, @Nonnull String descriptor, boolean isStatic) {
		this.name = Objects.requireNonNull(name);
		this.descriptor = Objects.requireNonNull(descriptor);
		this.isStatic = isStatic;
	}

	/**
	 * @return Field name.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Field descriptor.
	 */
	@Nonnull
	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * @return {@code true} when the generated field must be static.
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Marks the field as static when any use site requires it.
	 */
	public void markStatic() {
		isStatic = true;
	}
}
