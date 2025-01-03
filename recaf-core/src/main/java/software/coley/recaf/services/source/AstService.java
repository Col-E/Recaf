package software.coley.recaf.services.source;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.sourcesolver.Parser;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.resolve.Resolver;
import software.coley.sourcesolver.resolve.entry.BasicClassEntry;
import software.coley.sourcesolver.resolve.entry.BasicFieldEntry;
import software.coley.sourcesolver.resolve.entry.BasicMethodEntry;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Service for tracking shared data for AST parsing.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AstService implements Service {
	public static final String ID = "ast";
	/**
	 * Max number of referenced classes to build out from.
	 * See {@link WorkspaceBackedEntryPool#computeEntry(ClassInfo, int)}
	 */
	private static final int DEFAULT_TTL = 3;
	/**
	 * It's rare that we'll need more than one parser, so having a shared reference to a single one for re-use is nice.
	 */
	private static final Parser sharedParser;
	private final AstServiceConfig config;
	private final WorkspaceManager workspaceManager;
	private final Cache<Workspace, EntryPool> entryPoolCache = CacheBuilder.newBuilder()
			.weakKeys() // Intended for the side effect of using '==' for key comparisons over '.equals()'
			.maximumSize(1) // Users will only ever operate on one workspace, so this is free pruning when they switch
			.expireAfterWrite(20, TimeUnit.MINUTES)
			.build();
	private EntryPool currentWorkspacePool;

	static {
		// We need to have this reflection patch call before we create a parser because of the
		// tight coupling with the 'jdk.compiler' module internals that the parser has.
		ReflectUtil.patch();
		sharedParser = new Parser();
	}

	@Inject
	public AstService(@Nonnull AstServiceConfig config,
	                  @Nonnull WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
		this.config = config;

		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);
	}

	/**
	 * @return New Java source parser.
	 *
	 * @see #getSharedJavaParser() Shared parser instance.
	 */
	@Nonnull
	public Parser newJavaParser() {
		return new Parser();
	}

	/**
	 * @return Shared parser instance.
	 */
	@Nonnull
	public Parser getSharedJavaParser() {
		return sharedParser;
	}

	/**
	 * @param parser
	 * 		Parser to use.
	 * @param source
	 * 		Java source to parse.
	 *
	 * @return Parsed model of Java source file.
	 */
	@Nonnull
	public CompilationUnitModel parseJava(@Nonnull Parser parser, @Nonnull String source) {
		return parser.parse(source);
	}

	/**
	 * @param unit
	 * 		Unit to resolve references within.
	 *
	 * @return Resolver to link results to {@link PathNode paths in the current workspace}.
	 */
	@Nonnull
	public ResolverAdapter newJavaResolver(@Nonnull CompilationUnitModel unit) {
		Workspace workspace = workspaceManager.getCurrent();
		if (workspace == null)
			workspace = EmptyWorkspace.get();
		return newJavaResolver(workspace, poolFromWorkspace(workspace), unit);
	}

	/**
	 * @param workspace
	 * 		Workspace with classes to link resolved references to.
	 * @param unit
	 * 		Unit to resolve references within.
	 *
	 * @return Resolver to link results to {@link PathNode paths in the provided workspace}.
	 */
	@Nonnull
	public ResolverAdapter newJavaResolver(@Nonnull Workspace workspace, @Nonnull CompilationUnitModel unit) {
		return newJavaResolver(workspace, poolFromWorkspace(workspace), unit);
	}

	/**
	 * @param workspace
	 * 		Workspace with classes to link resolved references to.
	 * @param pool
	 * 		Pool containing class models used by the resolver.
	 * @param unit
	 * 		Unit to resolve references within.
	 *
	 * @return Resolver to link results to {@link PathNode paths in the provided workspace}.
	 */
	@Nonnull
	private ResolverAdapter newJavaResolver(@Nonnull Workspace workspace, @Nonnull EntryPool pool, @Nonnull CompilationUnitModel unit) {
		prefillReferencedClasses(workspace, pool, unit);
		return new ResolverAdapter(workspace, unit, pool);
	}

	/**
	 * @param unit
	 * 		Unit to map.
	 * @param resolver
	 * 		Resolver to analyze the unit with.
	 * @param mappings
	 * 		Mappings to apply.
	 *
	 * @return Modified source code based on the provided mappings.
	 */
	@Nonnull
	public String applyMappings(@Nonnull CompilationUnitModel unit, @Nonnull Resolver resolver, @Nonnull Mappings mappings) {
		return new AstMapper(unit, resolver, mappings).apply();
	}

	/**
	 * Takes classes from the same package that the given unit is from and populates them in the provided entry pool.
	 * <br>
	 * This is a very important step we must take before using the pool for content resolving.
	 *
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param pool
	 * 		Pool to dump class models into.
	 * @param unit
	 * 		Unit to prefill classes for.
	 */
	private void prefillReferencedClasses(@Nonnull Workspace workspace, @Nonnull EntryPool pool, @Nonnull CompilationUnitModel unit) {
		if (pool instanceof WorkspaceBackedEntryPool workspacePool) {
			String unitPackage = unit.getPackage().getName().replace('.', '/');
			SortedSet<ClassPathNode> classesInPackage = unitPackage.isEmpty() ?
					workspace.findClasses(c -> c.getPackageName() == null) :
					workspace.findClasses(c -> unitPackage.equals(c.getPackageName()));
			for (ClassPathNode classPath : classesInPackage)
				workspacePool.computeEntry(classPath.getValue(), DEFAULT_TTL);
		}
	}

	/**
	 * Maps a workspace to an entry pool instance.
	 * <br>
	 * It is very important that we re-use pools so that we do not waste time
	 * repetitively filling new pools with the same data.
	 *
	 * @param workspace
	 * 		Workspace to pull class information from.
	 *
	 * @return Entry pool to store class information for context resolution purposes.
	 */
	@Nonnull
	private EntryPool poolFromWorkspace(@Nonnull Workspace workspace) {
		EntryPool pool = entryPoolCache.getIfPresent(workspace);
		if (pool == null) {
			pool = new WorkspaceBackedEntryPool(workspace);
			entryPoolCache.put(workspace, pool);
		}
		return pool;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public AstServiceConfig getServiceConfig() {
		return config;
	}

	/**
	 * Empty pool that yields nothing.
	 */
	private static class EmptyEntryPool implements EntryPool {
		private static final EmptyEntryPool INSTANCE = new EmptyEntryPool();

		@Override
		public void register(@Nonnull ClassEntry entry) {
			// no-op
		}

		@Nullable
		@Override
		public ClassEntry getClass(@Nonnull String name) {
			return null;
		}

		@Nonnull
		@Override
		public List<ClassEntry> getClassesInPackage(@Nullable String packageName) {
			return Collections.emptyList();
		}
	}

	/**
	 * Pool that pulls classes from a {@link Workspace}.
	 */
	private static class WorkspaceBackedEntryPool implements EntryPool, ResourceJvmClassListener, ResourceAndroidClassListener {
		private final Map<String, ClassEntry> cache = new ConcurrentHashMap<>();
		private final Workspace workspace;

		private WorkspaceBackedEntryPool(@Nonnull Workspace workspace) {
			this.workspace = workspace;

			// TODO: When we have classes update, we will want to invalidate their child classes
			//  in the cache as well.
			workspace.getPrimaryResource().addListener(this);
		}

		@Override
		public void register(@Nonnull ClassEntry entry) {
			cache.put(entry.getName(), entry);
		}

		@Nullable
		@Override
		public ClassEntry getClass(@Nonnull String name) {
			return getClass(name, DEFAULT_TTL);
		}

		@Nonnull
		@Override
		public List<ClassEntry> getClassesInPackage(@Nullable String packageName) {
			Stream<ClassEntry> workspaceEntries = workspace.findClasses(c -> Objects.equals(packageName, c.getPackageName())).stream()
					.map(p -> computeEntry(p.getValue(), DEFAULT_TTL));
			Stream<ClassEntry> cacheEntries = cache.values().stream()
					.filter(e -> Objects.equals(packageName, e.getPackageName()));
			return Stream.concat(workspaceEntries, cacheEntries).toList();
		}

		/**
		 * Lookup a class entry by name, creating it if possible and not already cached.
		 *
		 * @param name
		 * 		Name of class.
		 * @param ttl
		 * 		Time-to-live, delegated to {@link #computeEntry(ClassInfo, int)}.
		 *
		 * @return Class entry by the given name, if cached or discoverable in the workspace.
		 */
		@Nullable
		private ClassEntry getClass(@Nonnull String name, int ttl) {
			ClassEntry entry = cache.get(name);
			if (entry != null)
				return entry;

			ClassPathNode path = workspace.findClass(name);
			if (path == null)
				return null;

			ClassInfo info = path.getValue();
			return computeEntry(info, ttl);
		}

		/**
		 * @param info
		 * 		Class model in the workspace to map to a form for context resolution.
		 * @param ttl
		 * 		Time-to-live, which will prevent construction of new entries when it reaches 0.
		 *
		 * @return Newly created entry modeling the given class.
		 */
		@Nullable
		private ClassEntry computeEntry(@Nonnull ClassInfo info, int ttl) {
			ClassEntry entry = cache.get(info.getName());
			if (entry != null)
				return entry;

			// Decrement TTL and if it reaches 0 we abort.
			if (--ttl <= 0)
				return null;

			// Construct the class entry model.
			//   NOTE: Parent types are fully computed regardless of TTL. The TTL reduction is used further below.
			ClassEntry superClass = info.getSuperName() == null ? null : getClass(info.getSuperName());
			List<FieldEntry> fields = info.getFields().stream()
					.map(f -> (FieldEntry) new BasicFieldEntry(f.getName(), f.getDescriptor(), f.getAccess()))
					.toList();
			List<MethodEntry> methods = info.getMethods().stream()
					.map(m -> (MethodEntry) new BasicMethodEntry(m.getName(), m.getDescriptor(), m.getAccess()))
					.toList();
			List<ClassEntry> innerClasses = new ArrayList<>();
			List<ClassEntry> interfaces = new ArrayList<>();
			entry = new BasicClassEntry(info.getName(), info.getAccess(), superClass, interfaces, innerClasses, fields, methods);
			register(entry);

			// Lists of other classes are populated after we put the entry in the pool to prevent entry building cycles.
			for (InnerClassInfo innerClass : info.getInnerClasses()) {
				if (innerClass.isExternalReference())
					continue;
				ClassEntry innerClassEntry = getClass(innerClass.getInnerClassName());
				if (innerClassEntry != null)
					innerClasses.add(innerClassEntry);
			}
			for (String implemented : info.getInterfaces()) {
				ClassEntry interfaceEntry = getClass(implemented);
				if (interfaceEntry != null)
					interfaces.add(interfaceEntry);
			}

			// Ensure all referenced classes are populated in the pool.
			// Because we only branch out based off a decrementing TTL counter, we should only end up mapping a few levels outwards.
			// This ensures that when we do any resolving logic with this pool, associated classes are readily available in the pool.
			//
			// There is a concern that the edges which fall on TTL==1 won't have their contents "readily available"
			// but in practice when those missing items are loaded it kicks off another round of pre-emptive loading.
			// This should result in a UX that is largely smoother overall, especially if the user is interacting with
			// classes that are "nearby" each other in terms of inheritance or external references.
			if (info instanceof JvmClassInfo jvmClassInfo)
				for (String referencedClass : jvmClassInfo.getReferencedClasses())
					getClass(referencedClass, ttl);

			return entry;
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
		                       @Nonnull AndroidClassInfo cls) {
			cache.remove(cls.getName());
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
		                          @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
			cache.remove(newCls.getName());
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
		                          @Nonnull AndroidClassInfo cls) {
			cache.remove(cls.getName());
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                       @Nonnull JvmClassInfo cls) {
			cache.remove(cls.getName());
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                          @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
			cache.remove(newCls.getName());
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                          @Nonnull JvmClassInfo cls) {
			cache.remove(cls.getName());
		}
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			currentWorkspacePool = poolFromWorkspace(workspace);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			currentWorkspacePool = EmptyEntryPool.INSTANCE;
		}
	}
}
