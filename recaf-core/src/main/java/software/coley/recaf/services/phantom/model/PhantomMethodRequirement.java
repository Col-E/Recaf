package software.coley.recaf.services.phantom.model;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Required method shape for a generated phantom class.
 *
 * @author Matt Coley
 */
public class PhantomMethodRequirement {
	private final String name;
	private final String descriptor;
	private boolean isStatic;
	private final Set<String> exceptions = new TreeSet<>();

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param isStatic
	 * 		Whether the method must be generated as static.
	 */
	public PhantomMethodRequirement(@Nonnull String name, @Nonnull String descriptor, boolean isStatic) {
		this.name = name;
		this.descriptor = descriptor;
		this.isStatic = isStatic;
	}

	/**
	 * @return Method name.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Method descriptor.
	 */
	@Nonnull
	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * @param exception
	 * 		Internal name of a checked or unchecked exception the method declares.
	 */
	public void addException(@Nonnull String exception) {
		exceptions.add(exception);
	}

	/**
	 * @return Internal names of the exceptions the method declares, sorted.
	 */
	@Nonnull
	public Set<String> getExceptions() {
		return Collections.unmodifiableSet(exceptions);
	}

	/**
	 * Marks the method as static when any use site requires it.
	 */
	public void markStatic() {
		isStatic = true;
	}

	/**
	 * @return {@code true} when the generated method must be static.
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * @return {@code true} when the requirement targets a constructor.
	 */
	public boolean isConstructor() {
		return "<init>".equals(name);
	}

	/**
	 * @return Stable key used when deduplicating methods by signature.
	 */
	@Nonnull
	public String key() {
		return name + descriptor;
	}
}
