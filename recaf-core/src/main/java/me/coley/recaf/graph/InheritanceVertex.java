package me.coley.recaf.graph;

import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.MemberInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Graph element for a class inheritance hierarchy.
 *
 * @author Matt Coley
 */
public class InheritanceVertex {
	private final Function<String, InheritanceVertex> lookup;
	private final Function<String, Collection<String>> childrenLookup;
	private final ClassInfo value;
	private final boolean isPrimary;
	private Set<InheritanceVertex> parents;
	private Set<InheritanceVertex> children;

	/**
	 * @param value
	 * 		The wrapped value.
	 * @param lookup
	 * 		Class vertex lookup.
	 * @param childrenLookup
	 * 		Class child lookup.
	 * @param isPrimary
	 *  Flag for if the class belongs to a workspaces primary resource.
	 */
	public InheritanceVertex(ClassInfo value, Function<String, InheritanceVertex> lookup,
							 Function<String, Collection<String>> childrenLookup, boolean isPrimary) {
		this.value = value;
		this.lookup = lookup;
		this.childrenLookup = childrenLookup;
		this.isPrimary = isPrimary;
	}

	/**
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return If the field exists in the current vertex.
	 */
	public boolean hasField(String name, String desc) {
		for (MemberInfo fn : value.getFields())
			if (fn.getName().equals(name) && fn.getDescriptor().equals(desc))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return If the method exists in the current vertex.
	 */
	public boolean hasMethod(String name, String desc) {
		for (MemberInfo mn : value.getMethods())
			if (mn.getName().equals(name) && mn.getDescriptor().equals(desc))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return {@code true} if method is an extension of an outside class's methods and thus should not be renamed.
	 * {@code false} if the method is safe to rename.
	 */
	public boolean isLibraryMethod(String name, String desc) {
		// Check against this definition
		if (!isPrimary && hasMethod(name, desc))
			return true;
		// Check parents.
		// If we extend a class with a library definition then it should be considered a library method.
		for (InheritanceVertex parent : getParents())
			if (parent.isLibraryMethod(name, desc))
				return true;
		// No library definition found, so its safe to rename.
		return false;
	}

	/**
	 * @return The entire class hierarchy.
	 */
	public Set<InheritanceVertex> getFamily() {
		Set<InheritanceVertex> family = new HashSet<>();
		getAllParents().forEach(p -> {
			if (!p.getName().equals("java/lang/Object"))
				family.addAll(p.getAllChildren());
		});
		return family;
	}

	/**
	 * @return All classes this extends or implements.
	 */
	public Set<InheritanceVertex> getAllParents() {
		return parents().collect(Collectors.toSet());
	}

	private Stream<InheritanceVertex> parents() {
		return Stream.concat(
				Stream.of(this),
				getParents().stream().flatMap(InheritanceVertex::parents));
	}

	/**
	 * @return Classes this directly extends or implements.
	 */
	public Set<InheritanceVertex> getParents() {
		if (parents == null) {
			parents = new HashSet<>();
			if (value.getSuperName() != null) {
				InheritanceVertex parentVertex = lookup.apply(value.getSuperName());
				if (parentVertex != null)
					parents.add(parentVertex);
			}
			for (String itf : value.getInterfaces()) {
				InheritanceVertex itfVertex = lookup.apply(itf);
				if (itfVertex != null)
					parents.add(itfVertex);
			}
		}
		return parents;
	}

	/**
	 * @return All classes this extends or implements.
	 */
	public Set<InheritanceVertex> getAllChildren() {
		return children().collect(Collectors.toSet());
	}

	private Stream<InheritanceVertex> children() {
		return Stream.concat(
				Stream.of(this),
				getChildren().stream().flatMap(InheritanceVertex::children));
	}

	/**
	 * @return Classes that extend or implement this class.
	 */
	public Set<InheritanceVertex> getChildren() {
		if (children == null) {
			children = new HashSet<>();
			childrenLookup.apply(value.getName())
					.forEach(name -> children.add(lookup.apply(name)));
		}
		return children;
	}

	/**
	 * @return {@link #getValue() wrapped class's} name
	 */
	public String getName() {
		return value.getName();
	}

	/**
	 * @return Wrapped class info.
	 */
	public ClassInfo getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InheritanceVertex vertex = (InheritanceVertex) o;
		return Objects.equals(getName(), vertex.getName());
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}
