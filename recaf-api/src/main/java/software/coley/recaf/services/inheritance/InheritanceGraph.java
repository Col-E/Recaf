package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import software.coley.collections.Lists;
import software.coley.recaf.cdi.AutoRegisterWorkspaceListeners;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.BasicJvmClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.MultiMap;
import software.coley.recaf.util.MultiMapBuilder;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class inheritance graph utility.
 *
 * @author Matt Coley
 */
@WorkspaceScoped
@AutoRegisterWorkspaceListeners
public class InheritanceGraph implements Service, WorkspaceModificationListener, WorkspaceCloseListener,
		ResourceJvmClassListener, ResourceAndroidClassListener {
	public static final String SERVICE_ID = "graph-inheritance";
	private static final InheritanceVertex STUB = new InheritanceStubVertex();
	private static final String OBJECT = "java/lang/Object";
	private final MultiMap<String, String, Set<String>> parentToChild = MultiMapBuilder
			.<String, String>hashKeys()
			.hashValues()
			.build();
	private final Map<String, InheritanceVertex> vertices = new ConcurrentHashMap<>();
	private final Function<String, InheritanceVertex> vertexProvider = createVertexProvider();
	private final InheritanceGraphConfig config;
	private final Workspace workspace;

	/**
	 * Create an inheritance graph.
	 *
	 * @param config
	 * 		Config instance.
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	@Inject
	public InheritanceGraph(@Nonnull InheritanceGraphConfig config, @Nonnull Workspace workspace) {
		this.config = config;
		this.workspace = workspace;

		// Add listeners to primary resource so when classes update we keep our graph up to date.
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		primaryResource.addResourceJvmClassListener(this);
		primaryResource.addResourceAndroidClassListener(this);

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
		for (WorkspaceResource resource : Lists.add(workspace.getSupportingResources(), workspace.getPrimaryResource())) {
			resource.getJvmClassBundle().values()
					.forEach(this::populateParentToChildLookup);
			resource.androidClassBundleStream()
					.flatMap(bundle -> bundle.values().stream())
					.forEach(this::populateParentToChildLookup);
		}
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
		parentToChild.put(parentName, name);
	}

	/**
	 * Populate all references from the given child class to its parents.
	 *
	 * @param info
	 * 		Child class.
	 */
	private void populateParentToChildLookup(@Nonnull ClassInfo info) {
		// Skip module classes
		if (info.hasModuleModifier())
			return;

		// Add direct parent
		String name = info.getName();
		String superName = info.getSuperName();
		if (superName != null)
			populateParentToChildLookup(name, superName);

		// Visit parent
		InheritanceVertex superVertex = vertexProvider.apply(superName);
		if (superVertex != null && !superVertex.isJavaLangObject() && superVertex.getValue() != null && !superVertex.isLoop())
			populateParentToChildLookup(superVertex.getValue());

		// Add direct interfaces
		for (String itf : info.getInterfaces()) {
			populateParentToChildLookup(name, itf);

			// Visit interfaces
			InheritanceVertex interfaceVertex = vertexProvider.apply(itf);
			if (interfaceVertex != null && interfaceVertex.getValue() != null)
				populateParentToChildLookup(interfaceVertex.getValue());
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
		parentToChild.remove(parentName, name);
		InheritanceVertex parentVertex = getVertex(parentName);
		InheritanceVertex childVertex = getVertex(name);
		if (parentVertex != null) parentVertex.clearCachedVertices();
		if (childVertex != null) childVertex.clearCachedVertices();
	}

	/**
	 * @param parent
	 * 		Parent to find children of.
	 *
	 * @return Direct extensions/implementations of the given parent.
	 */
	@Nonnull
	private Collection<String> getDirectChildren(String parent) {
		return parentToChild.getIfPresent(parent);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Vertex in graph of class. {@code null} if no such class was found in the inputs.
	 */
	@Nullable
	public InheritanceVertex getVertex(String name) {
		InheritanceVertex vertex = vertices.computeIfAbsent(name, vertexProvider);
		return vertex == STUB ? null : vertex;
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
	public Set<InheritanceVertex> getVertexFamily(String name, boolean includeObject) {
		InheritanceVertex vertex = getVertex(name);
		if (vertex == null)
			return Collections.emptySet();
		return vertex.getFamily(includeObject);
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
		// Full upwards hierarchy for the first
		InheritanceVertex vertex = getVertex(first);
		if (vertex == null || OBJECT.equals(first) || OBJECT.equals(second))
			return OBJECT;

		Set<String> firstParents = vertex.allParents()
				.map(InheritanceVertex::getName)
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
				break;

			InheritanceVertex nextVertex = getVertex(next);
			if (nextVertex == null)
				break;

			for (String parent : nextVertex.getParents().stream()
					.map(InheritanceVertex::getName).toList()) {
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

	@Nonnull
	private Function<String, InheritanceVertex> createVertexProvider() {
		return name -> {
			// Edge case handling for 'java/lang/Object' doing a parent lookup.
			// There is no parent, do not use STUB.
			if (name == null)
				return null;

			// Find class in workspace, if not found yield stub.
			ClassPathNode result = workspace.findClass(name);
			if (result == null)
				return STUB;

			// Map class to vertex.
			ResourcePathNode resourceParent = result.getParentOfType(WorkspaceResource.class);
			boolean isPrimary = resourceParent != null && resourceParent.isPrimary();
			ClassInfo info = result.getValue();
			return new InheritanceVertex(info, this::getVertex, this::getDirectChildren, isPrimary);
		};
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
		removeParentToChildLookup(cls);
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		removeParentToChildLookup(cls);
	}

	@Override
	public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		refreshChildLookup();
	}

	@Override
	public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		// no-op
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		parentToChild.clear();
		vertices.clear();
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public InheritanceGraphConfig getServiceConfig() {
		return config;
	}

	private static class InheritanceStubVertex extends InheritanceVertex {
		private InheritanceStubVertex() {
			super(new StubClass(), in -> null, in -> null, false);
		}

		@Override
		public boolean hasField(String name, String desc) {
			return false;
		}

		@Override
		public boolean hasMethod(String name, String desc) {
			return false;
		}

		@Override
		public boolean isJavaLangObject() {
			return false;
		}

		@Override
		public boolean isParentOf(InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isChildOf(InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isIndirectFamilyMember(InheritanceVertex vertex) {
			return false;
		}

		@Override
		public boolean isIndirectFamilyMember(Set<InheritanceVertex> family, InheritanceVertex vertex) {
			return false;
		}

		@Override
		public Set<InheritanceVertex> getFamily(boolean includeObject) {
			return Collections.emptySet();
		}

		@Override
		public Set<InheritanceVertex> getAllParents() {
			return Collections.emptySet();
		}

		@Override
		public Stream<InheritanceVertex> allParents() {
			return Stream.empty();
		}

		@Override
		public Set<InheritanceVertex> getParents() {
			return Collections.emptySet();
		}

		@Override
		public Set<InheritanceVertex> getAllChildren() {
			return Collections.emptySet();
		}

		@Override
		public Set<InheritanceVertex> getChildren() {
			return Collections.emptySet();
		}

		@Override
		public Set<InheritanceVertex> getAllDirectVertices() {
			return Collections.emptySet();
		}

		@Override
		public String getName() {
			return "$$STUB$$";
		}
	}

	private static class StubClass extends BasicJvmClassInfo {
		public StubClass() {
			super(new JvmClassInfoBuilder()
					.adaptFrom(RuntimeWorkspaceResource.getInstance().getJvmClassBundle()
							.get("java/lang/Object").getClassReader()));
		}
	}
}