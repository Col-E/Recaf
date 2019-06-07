package me.coley.recaf.bytecode.analysis;

import me.coley.recaf.Input;
import me.coley.recaf.graph.Graph;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Graph model to represent the class inheritance of a loaded input. <br>
 * The graph is generative, meaning the graph's vertices and edges are dynamically generated when
 * requested. The relations are not stored due to their modifiable nature.
 *
 * @author Matt
 */
public class Hierarchy implements Graph<ClassNode, ClassVertex> {
	/**
	 * Map of parent to children names.
	 */
	private final Map<String, Set<String>> descendents = new HashMap<>();
	/**
	 * Input to use for generating vertices from.
	 */
	private final Input input;

	public Hierarchy(Input input) {
		this.input = input;
		setupChildLookup();
	}

	@Override
	public Set<ClassVertex> roots() {
		return getInput().getClasses().values().stream()
				.map(cn -> new ClassVertex(this, cn))
				.collect(Collectors.toSet());
	}

	public ClassVertex getRoot(String name) {
		if (getInput().classes.contains(name)) {
			ClassNode key = input.getClass(name);
			return getRoot(key);
		}
		return null;
	}

	/**
	 * @return Input associated with the current hierarchy map.
	 */
	public Input getInput() {
		return input;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Direct descendants of the class.
	 */
	public Stream<String> getDescendants(String name) {
		if (descendents.containsKey(name))
			return descendents.get(name).stream();
		// Empty stream
		return Stream.of();
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return All descendants of the class.
	 */
	public Stream<String> getAllDescendants(String name) {
		return (getDescendants(name).map(desc -> getAllDescendants(desc))
				.reduce(getDescendants(name), (master, children) -> Stream.concat(master, children)));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Direct parents of the class.
	 */
	public Stream<String> getParents(String name) {
		ClassVertex vert = getRoot(name);
		if (vert != null)
			return getParents(vert);
		// Empty stream
		return Stream.of();
	}

	/**
	 * @param vertex
	 * 		Class vertex.
	 *
	 * @return Direct parents of the class.
	 */
	public Stream<String> getParents(ClassVertex vertex) {
		return Stream.concat(
				Stream.of(vertex.getData().superName),
				Stream.of(vertex.getData().interfaces.toArray(new String[0])));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return All parents of the class.
	 */
	public Stream<String> getAllParents(String name) {
		return (getParents(name).map(desc -> getAllParents(desc))
				.reduce(getParents(name), (master, parents) -> Stream.concat(master, parents)));
	}



	// ============================== UTILITY =================================== //

	/**
	 * Populate {@link #descendents} map.
	 */
	private void setupChildLookup() {
		// TODO: Call this if somebody changes a supername/interface
		descendents.clear();
		for (ClassNode clazz : getInput().getClasses().values()) {
			descendents.computeIfAbsent(clazz.superName, k -> new HashSet<>()).add(clazz.name);
			for (String inter : clazz.interfaces) {
				descendents.computeIfAbsent(inter, k -> new HashSet<>()).add(clazz.name);
			}
		}
	}
}
