package me.coley.recaf.graph;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.Recurse;

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
	private final CommonClassInfo value;
	private final boolean isPrimary;
	private volatile Set<InheritanceVertex> parents;
	private volatile Set<InheritanceVertex> children;

	/**
	 * @param value
	 * 		The wrapped value.
	 * @param lookup
	 * 		Class vertex lookup.
	 * @param childrenLookup
	 * 		Class child lookup.
	 * @param isPrimary
	 * 		Flag for if the class belongs to a workspaces primary resource.
	 */
	public InheritanceVertex(CommonClassInfo value, Function<String, InheritanceVertex> lookup,
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
		for (FieldInfo fn : value.getFields())
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
		for (MethodInfo mn : value.getMethods())
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
		return parents()
				.filter(x -> !"java/lang/Object".equals(x.getName()))
				.flatMap(InheritanceVertex::children)
				.collect(Collectors.toSet());
	}

	/**
	 * @return All classes this extends or implements.
	 */
	public Set<InheritanceVertex> getAllParents() {
		return parents().collect(Collectors.toSet());
	}

	/**
	 * @return All classes this extends or implements.
	 */
	public Stream<InheritanceVertex> parents() {
		return Recurse.recurse(this, x -> x.getParents().stream());
	}

	/**
	 * @return Classes this directly extends or implements.
	 */
	public Set<InheritanceVertex> getParents() {
		Set<InheritanceVertex> parents = this.parents;
		if (parents == null) {
			synchronized (this) {
				parents = this.parents;
				if (parents == null) {
					String name = getName();
					parents = new HashSet<>();
					String superName = value.getSuperName();
					if (superName != null && !name.equals(superName)) {
						InheritanceVertex parentVertex = lookup.apply(superName);
						if (parentVertex != null)
							parents.add(parentVertex);
					}
					for (String itf : value.getInterfaces()) {
						InheritanceVertex itfVertex = lookup.apply(itf);
						if (itfVertex != null && !name.equals(itf))
							parents.add(itfVertex);
					}
					this.parents = parents;
				}
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
		return Recurse.recurse(this, x -> x.getChildren().stream());
	}

	/**
	 * @return Classes that extend or implement this class.
	 */
	public Set<InheritanceVertex> getChildren() {
		Set<InheritanceVertex> children = this.children;
		if (children == null) {
			synchronized (this) {
				children = this.children;
				if (children == null) {
					String name = getName();
					children = childrenLookup.apply(value.getName())
							.stream()
							.filter(childName -> !name.equals(childName))
							.map(lookup)
							.collect(Collectors.toSet());
					this.children = children;
				}
			}
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
	public CommonClassInfo getValue() {
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
