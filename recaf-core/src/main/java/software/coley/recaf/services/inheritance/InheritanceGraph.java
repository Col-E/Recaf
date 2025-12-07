package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.mapping.MappingApplicationListener;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents class inheritance as a navigable graph.
 *
 * @author Matt Coley
 */
public class InheritanceGraph implements WorkspaceModificationListener, WorkspaceCloseListener,
		ResourceJvmClassListener, ResourceAndroidClassListener, MappingApplicationListener {
	/** Vertex used for classes that are not found in the workspace. */
	private static final InheritanceVertex STUB = new InheritanceStubVertex();
	private static final String OBJECT = "java/lang/Object";
	private final Map<String, Set<String>> parentToChild;
	private final Map<String, InheritanceVertex> vertices;
	private final Set<String> stubs = ConcurrentHashMap.newKeySet();
	private final Workspace workspace;

	/**
	 * Create an inheritance graph.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public InheritanceGraph(@Nonnull Workspace workspace) {
		this.workspace = workspace;

		// Populate map lookups with the initial capacity of the number of classes in the workspace plus a buffer.
		int classesInWorkspace = workspace.allResourcesStream(false /* dont count internal resource classes */)
				.mapToInt(res -> res.classBundleStreamRecursive().mapToInt(Map::size).sum())
				.sum() + 1;
		parentToChild = new ConcurrentHashMap<>(classesInWorkspace);
		vertices = new ConcurrentHashMap<>(classesInWorkspace);

		// Add listeners to primary resource so when classes update we keep our graph up to date.
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		primaryResource.addResourceJvmClassListener(this);
		primaryResource.addResourceAndroidClassListener(this);
		workspace.addWorkspaceModificationListener(this);

		// Populate downwards (parent --> child) lookup
		refreshChildLookup();
	}

	/**
	 * Refresh parent-to-child lookup.
	 */
	private void refreshChildLookup() {
		// Clear
		parentToChild.clear();

		// Repopulate
		workspace.findClasses(false, cls -> {
			populateParentToChildLookup(cls);
			return false;
		});
	}

	/**
	 * Populate a references from the given child class to the parent class.
	 *
	 * @param name
	 * 		Child class name.
	 * @param parentName
	 * 		Parent class name.
	 */
	private void populateParentToChildLookup(@Nonnull String name, @Nonnull String parentName) {
		parentToChild.computeIfAbsent(parentName, k -> ConcurrentHashMap.newKeySet()).add(name);

		// Clear any cached relationships in the vertex and the parent vertex.
		InheritanceVertex parentVertex = getVertex(parentName);
		InheritanceVertex childVertex = getVertex(name);
		if (parentVertex != null) parentVertex.clearCachedVertices();
		if (childVertex != null) childVertex.clearCachedVertices();
	}

	/**
	 * Populate all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	private void populateParentToChildLookup(@Nonnull ClassInfo info) {
		populateParentToChildLookup(info, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	/**
	 * Populate all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 * @param visited
	 * 		Classes already visited in population.
	 */
	private void populateParentToChildLookup(@Nonnull ClassInfo info, @Nonnull Set<ClassInfo> visited) {
		// Since we have observed this class to exist, we will remove the "stub" placeholder for this name.
		stubs.remove(info.getName());

		// Skip if already visited
		if (!visited.add(info))
			return;

		// Skip module classes
		if (info.hasModuleModifier())
			return;

		// Add direct parent
		String name = info.getName();
		InheritanceVertex vertex = getVertex(name);
		if (vertex != null)
			vertex.clearCachedVertices();

		String superName = info.getSuperName();
		if (superName != null) {
			populateParentToChildLookup(name, superName);

			// Visit parent
			InheritanceVertex superVertex = getVertex(superName);
			if (superVertex != null && !superVertex.isJavaLangObject() && !superVertex.isLoop())
				populateParentToChildLookup(superVertex.getValue(), visited);
		}

		// Add direct interfaces
		for (String itf : info.getInterfaces()) {
			populateParentToChildLookup(name, itf);

			// Visit interfaces
			InheritanceVertex interfaceVertex = getVertex(itf);
			if (interfaceVertex != null)
				populateParentToChildLookup(interfaceVertex.getValue(), visited);
		}
	}

	/**
	 * Remove all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	private void removeParentToChildLookup(@Nonnull ClassInfo info) {
		String superName = info.getSuperName();
		if (superName != null)
			removeParentToChildLookup(info.getName(), superName);
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
	private void removeParentToChildLookup(@Nonnull String name, @Nonnull String parentName) {
		Set<String> children = parentToChild.get(parentName);
		if (children != null)
			children.remove(name);

		// Clear any cached relationships in the vertex and the parent vertex.
		InheritanceVertex parentVertex = getVertex(parentName);
		InheritanceVertex childVertex = getVertex(name);
		if (parentVertex != null) parentVertex.clearCachedVertices();
		if (childVertex != null) childVertex.clearCachedVertices();
	}

	/**
	 * Removes the given class from the graph.
	 *
	 * @param cls
	 * 		Class that was removed.
	 */
	private void removeClass(@Nonnull ClassInfo cls) {
		removeParentToChildLookup(cls);

		String name = cls.getName();
		vertices.remove(name);
	}

	/**
	 * @param parent
	 * 		Parent to find children of.
	 *
	 * @return Direct extensions/implementations of the given parent.
	 */
	@Nonnull
	private Set<String> getDirectChildren(@Nonnull String parent) {
		return parentToChild.getOrDefault(parent, Collections.emptySet());
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Vertex in graph of class. {@code null} if no such class was found in the inputs.
	 */
	@Nullable
	public InheritanceVertex getVertex(@Nonnull String name) {
		InheritanceVertex vertex = vertices.get(name);
		if (vertex == null && !stubs.contains(name)) {
			// Vertex does not exist and was not marked as a stub.
			// We want to look up the vertex for the given class and figure out if its valid or needs to be stubbed.
			InheritanceVertex provided = createVertex(name);
			if (provided == STUB || provided == null) {
				// Provider yielded either a stub OR no result. Discard it.
				stubs.add(name);
			} else {
				// Provider yielded a valid vertex. Update the return value and record it in the map.
				vertices.put(name, provided);
				vertex = provided;
			}
		}
		return vertex;
	}

	/**
	 * @param name
	 * 		Class name.
	 * @param includeObject
	 *        {@code true} to include {@link Object} as a vertex.
	 *
	 * @return Complete inheritance family of the class.
	 */
	@Nonnull
	public Set<InheritanceVertex> getVertexFamily(@Nonnull String name, boolean includeObject) {
		InheritanceVertex vertex = getVertex(name);
		if (vertex == null)
			return Collections.emptySet();
		if (vertex.isModule())
			return Collections.singleton(vertex);
		return vertex.getFamily(includeObject);
	}

	/**
	 * Given {@code List.class.isAssignableFrom(ArrayList.class)} the {@code first} parameter would be
	 * {@code java/util/List} and the {@code second} parameter would be {@code java/util/ArrayList}.
	 *
	 * @param first
	 * 		Assumed super-class or interface type.
	 * @param second
	 * 		Assumed child class which extends the super-class or implements the interface type.
	 *
	 * @return {@code true} when {@code first.isAssignableFrom(second)}.
	 */
	public boolean isAssignableFrom(@Nonnull String first, @Nonnull String second) {
		// Any Object can be assigned from T.
		if (OBJECT.equals(first))
			return true;

		// Any T can be assigned from T.
		if (first.equals(second))
			return true;

		// Any non-Object T cannot be assigned from Object.
		if (second.equals(OBJECT))
			return false;

		// Lookup vertex for the child type, and see if any parent contains the supposed super/interface type.
		InheritanceVertex secondVertex = getVertex(second);
		if (secondVertex != null && secondVertex.hasParent(second))
			return true;

		// Lookup vertex for the parent type, and see if any child contains the supposed type.
		InheritanceVertex firstVertex = getVertex(first);
		return firstVertex != null && firstVertex.hasChild(second);
	}

	/**
	 * @param first
	 * 		First class name.
	 * @param second
	 * 		Second class name.
	 *
	 * @return Common parent of the classes.
	 */
	@Nonnull
	public String getCommon(@Nonnull String first, @Nonnull String second) {
		// Easy base cases
		if (OBJECT.equals(first) || OBJECT.equals(second))
			return OBJECT;
		if (first.equals(second))
			return first;

		// Try with the first name
		InheritanceVertex vertex = getVertex(first);
		if (vertex != null)
			return getCommon(vertex, first, second);

		// Try again but with the other name
		vertex = getVertex(second);
		if (vertex != null)
			return getCommon(vertex, second, first);

		// Neither is resolvable
		return OBJECT;
	}

	/**
	 * @param firstVertex
	 * 		Vertex of the {@code first} name.
	 * @param first
	 * 		First class name.
	 * @param second
	 * 		Second class name.
	 *
	 * @return Common parent of the classes.
	 */
	@Nonnull
	private String getCommon(@Nonnull InheritanceVertex firstVertex, @Nonnull String first, @Nonnull String second) {
		// Full upwards hierarchy for the first
		SequencedSet<String> firstParents = firstVertex.allParents()
				.map(InheritanceVertex::getParentAndCurrentNames)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		firstParents.add(first);

		// Ensure 'Object' is last
		firstParents.remove(OBJECT);
		firstParents.add(OBJECT);

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
				continue;

			InheritanceVertex nextVertex = getVertex(next);
			if (nextVertex == null)
				continue;

			for (String parent : nextVertex.getParents().stream()
					.map(InheritanceVertex::getParentAndCurrentNames)
					.flatMap(Collection::stream)
					.toList()) {
				if (!parent.equals(OBJECT)) {
					// Parent in the set of visited classes? Then its valid.
					if (firstParents.contains(parent))
						return parent;
					// Queue up the parent
					queue.add(parent);
				}
			}
		} while (!queue.isEmpty());

		// Fallback option
		return OBJECT;
	}

	/**
	 * When {@link #STUB} is the return of this method, the class was not found.
	 * <br>
	 * When {@code null} is the return of this method, the class name is illegal.
	 *
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Vertex of class.
	 */
	@Nullable
	private InheritanceVertex createVertex(@Nullable String name) {
		// Edge case handling for 'java/lang/Object' doing a parent lookup.
		// There is no parent, do not use STUB.
		if (name == null)
			return null;

		// Edge case handling for arrays. There is no object typing of arrays.
		if (name.isEmpty() || name.charAt(0) == '[')
			return null;

		// Find class in workspace, if not found yield stub.
		ClassPathNode result = workspace.findClass(name);
		if (result == null)
			return STUB;

		// Map class to vertex.
		ResourcePathNode resourcePath = result.getPathOfType(WorkspaceResource.class);
		boolean isPrimary = resourcePath != null && resourcePath.isPrimaryOrEmbeddedInPrimary();
		ClassInfo info = result.getValue();
		return new InheritanceVertex(info, this::getVertex, this::getDirectChildren, isPrimary);
	}

	private void onUpdateClassImpl(@Nonnull ClassInfo oldValue, @Nonnull ClassInfo newValue) {
		String name = oldValue.getName();
		if (!newValue.getName().equals(name))
			throw new IllegalStateException("onUpdateClass should not permit a class name change");

		// Update hierarchy now that super-name changed
		if (oldValue.getSuperName() != null && newValue.getSuperName() != null) {
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


	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		populateParentToChildLookup(cls);
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		populateParentToChildLookup(cls);
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
		onUpdateClassImpl(oldCls, newCls);
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
		onUpdateClassImpl(oldCls, newCls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		removeClass(cls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		removeClass(cls);
	}

	@Override
	public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		Set<ClassInfo> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		library.jvmClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.forEach(c -> populateParentToChildLookup(c, visited));
		library.androidClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.forEach(c -> populateParentToChildLookup(c, visited));
		refreshChildLookup();
	}

	@Override
	public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		library.jvmClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.forEach(this::removeClass);
		library.androidClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.forEach(this::removeClass);
		refreshChildLookup();
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		parentToChild.clear();
		vertices.clear();
		stubs.clear();
	}

	@Override
	public void onPreApply(@Nonnull Workspace workspace, @Nonnull MappingResults mappingResults) {
		// no-op
	}

	@Override
	public void onPostApply(@Nonnull Workspace workspace, @Nonnull MappingResults mappingResults) {
		// Must apply to the graph's associated workspace.
		if (this.workspace != workspace)
			return;

		// Remove vertices and lookups of items that no longer exist.
		mappingResults.getPreMappingPaths().forEach((name, path) -> {
			// If we see a 'stub' from the vertex creator, we know it is no longer
			// in the workspace and should be removed from our cache.
			InheritanceVertex vertex = createVertex(name);
			if (vertex == STUB) {
				vertices.remove(name);
				parentToChild.remove(name);
			}
		});

		// While applying mappings, the graph does not perfectly refresh, so we need to clear out some state
		// so that when the graph is used again the correct information will be fetched.
		Set<ClassInfo> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		mappingResults.getPostMappingPaths().forEach((name, path) -> {
			// Stub information for classes we know exist in the workspace should be removed.
			stubs.remove(name);

			// Refresh the parent-->children mapping.
			parentToChild.remove(name);
			ClassInfo postClass = path.getValue();
			populateParentToChildLookup(postClass, visited);
		});
	}

	private static class InheritanceStubVertex extends InheritanceVertex {
		private InheritanceStubVertex() {
			super(new StubClassInfo("java/lang/Object").asJvmClass(), in -> null, in -> null, false);
		}

		@Override
		public boolean hasField(@Nonnull String name, @Nonnull String desc) {
			return false;
		}

		@Override
		public boolean hasMethod(@Nonnull String name, @Nonnull String desc) {
			return false;
		}

		@Override
		public boolean isJavaLangObject() {
			return false;
		}

		@Override
		public boolean isParentOf(@Nonnull InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isChildOf(@Nonnull InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isIndirectFamilyMember(@Nonnull InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isIndirectFamilyMember(@Nonnull Set<InheritanceVertex> family, @Nonnull InheritanceVertex vertex) {
			return false;
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getFamily(boolean includeObject) {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getAllParents() {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public Stream<InheritanceVertex> allParents() {
			return Stream.empty();
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getParents() {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getAllChildren() {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getChildren() {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public Set<InheritanceVertex> getAllDirectVertices() {
			return Collections.emptySet();
		}

		@Nonnull
		@Override
		public String getName() {
			return "$$STUB$$";
		}
	}
}