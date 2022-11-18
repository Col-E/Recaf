package me.coley.recaf.graph;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.util.Multimap;
import me.coley.recaf.util.MultimapBuilder;
import me.coley.recaf.util.Types;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.Resources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class inheritance graph utility.
 *
 * @author Matt Coley
 */
public class InheritanceGraph implements ResourceClassListener, ResourceDexClassListener {
	private static final InheritanceVertex STUB = new InheritanceVertex(null, null, null, false);
	private static final String OBJECT = "java/lang/Object";
	private final Multimap<String, String, Set<String>> parentToChild = MultimapBuilder
			.<String, String>hashKeys()
			.hashValues()
			.build();
	private final Map<String, InheritanceVertex> vertices = new ConcurrentHashMap<>();
	private final Function<String, InheritanceVertex> vertexProvider = createVertexProvider();
	private final Workspace workspace;

	/**
	 * Create an inheritance graph.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public InheritanceGraph(Workspace workspace) {
		this.workspace = workspace;
		for (Resource resource : workspace.getResources()) {
			resource.addClassListener(this);
			resource.addDexListener(this);
		}
		refreshChildLookup();
	}

	/**
	 * Refresh parent-to-child lookup.
	 */
	public void refreshChildLookup() {
		// Clear
		parentToChild.clear();
		// Repopulate
		workspace.getResources().getClasses()
				.forEach(this::populateParentToChildLookup);
	}

	/**
	 * Populate a references from the given child class to the parent class.
	 *
	 * @param name
	 * 		Child class name.
	 * @param parentName
	 * 		Parent class name.
	 */
	public void populateParentToChildLookup(String name, String parentName) {
		parentToChild.put(parentName, name);
	}

	/**
	 * Populate all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	public void populateParentToChildLookup(CommonClassInfo info) {
		populateParentToChildLookup(info.getName(), info.getSuperName());
		for (String itf : info.getInterfaces())
			populateParentToChildLookup(info.getName(), itf);
	}

	/**
	 * Remove all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	public void removeParentToChildLookup(CommonClassInfo info) {
		removeParentToChildLookup(info.getName(), info.getSuperName());
		for (String itf : info.getInterfaces())
			removeParentToChildLookup(info.getName(), itf);
	}

	/**
	 * Remove a references from the given child class to the parent class.
	 *
	 * @param name
	 * 		Child class name.
	 * @param parentName
	 * 		Parent class name.
	 */
	public void removeParentToChildLookup(String name, String parentName) {
		parentToChild.remove(parentName, name);
	}

	/**
	 * @param parent
	 * 		Parent to find children of.
	 *
	 * @return Direct extensions/implementations of the given parent.
	 */
	private Collection<String> getDirectChildren(String parent) {
		return parentToChild.getIfPresent(parent);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Vertex in graph of class. {@code null} if no such class was found in the inputs.
	 */
	public InheritanceVertex getVertex(String name) {
		InheritanceVertex vertex = vertices.computeIfAbsent(name, vertexProvider);
		return vertex == STUB ? null : vertex;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Complete inheritance family of the class.
	 */
	public Set<InheritanceVertex> getVertexFamily(String name) {
		InheritanceVertex vertex = getVertex(name);
		if (vertex == null)
			return Collections.emptySet();
		return vertex.getFamily();
	}

	/**
	 * @param first
	 * 		First class name.
	 * @param second
	 * 		Second class name.
	 *
	 * @return Common parent of the classes.
	 */
	public String getCommon(String first, String second) {
		// Full upwards hierarchy for the first
		InheritanceVertex vertex = getVertex(first);
		if (vertex == null || OBJECT.equals(first) || OBJECT.equals(second))
			return OBJECT;
		Set<String> firstParents = getVertex(first).allParents()
				.map(InheritanceVertex::getName)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		firstParents.add(first);
		// Ensure 'Object' is last
		firstParents.remove(Types.OBJECT_TYPE.getInternalName());
		firstParents.add(Types.OBJECT_TYPE.getInternalName());
		// Base case
		if (firstParents.contains(second))
			return second;
		// Iterate over second's parents via breadth-first-search
		Queue<String> queue = new LinkedList<>();
		queue.add(second);
		do {
			// Item to fetch parents of
			String next = queue.poll();
			if (next == null || next.equals(OBJECT))
				break;
			InheritanceVertex nextVertex = getVertex(next);
			if (nextVertex == null)
				break;
			for (String parent : nextVertex.getParents().stream()
					.map(InheritanceVertex::getName).collect(Collectors.toList())) {
				// Parent in the set of visited classes? Then its valid.
				if (firstParents.contains(parent))
					return parent;
				// Queue up the parent
				if (!parent.equals(OBJECT))
					queue.add(parent);
			}
		} while (!queue.isEmpty());
		// Fallback option
		return OBJECT;
	}

	private Function<String, InheritanceVertex> createVertexProvider() {
		return name -> {
			Resources resources = workspace.getResources();
			CommonClassInfo info = resources.getClass(name);
			if (info == null)
				info = resources.getDexClass(name);
			if (info == null)
				return STUB;
			boolean isPrimary = resources.getPrimary().getClasses().containsKey(name) ||
					resources.getPrimary().getDexClasses().containsKey(name);
			return new InheritanceVertex(info, this::getVertex, this::getDirectChildren, isPrimary);
		};
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		populateParentToChildLookup(newValue);
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		populateParentToChildLookup(newValue);
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		removeParentToChildLookup(oldValue);
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		removeParentToChildLookup(oldValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		onUpdateClassImpl(oldValue, newValue);
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		onUpdateClassImpl(oldValue, newValue);
	}

	private void onUpdateClassImpl(CommonClassInfo oldValue, CommonClassInfo newValue) {
		String name = oldValue.getName();
		if (!newValue.getName().equals(name))
			throw new IllegalStateException("onUpdateClass should not permit a class name change");
		// Update hierarchy now that super-name changed
		if(oldValue.getSuperName() != null && newValue.getSuperName() != null) {
			if (!oldValue.getSuperName().equals(newValue.getSuperName())) {
				removeParentToChildLookup(name, oldValue.getSuperName());
				populateParentToChildLookup(name, newValue.getSuperName());
			}
		}
		// Same deal, but for interfaces
		Set<String> interfaces = new HashSet<>(oldValue.getInterfaces());
		interfaces.addAll(newValue.getInterfaces());
		for (String itf : interfaces) {
			boolean oldHas = oldValue.getInterfaces().contains(itf);
			boolean newHas = newValue.getInterfaces().contains(itf);
			if (oldHas && !newHas) {
				removeParentToChildLookup(name, itf);
			} else if (!oldHas && newHas) {
				populateParentToChildLookup(name, itf);
			}
		}
		// Update vertex wrapped class-info
		InheritanceVertex vertex = getVertex(name);
		if (vertex != null)
			vertex.setValue(newValue);
	}
}