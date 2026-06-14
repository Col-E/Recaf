package software.coley.recaf.services.source;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.util.ReflectUtil;
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
import software.coley.sourcesolver.resolve.entry.DescribableEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;
import software.coley.sourcesolver.resolve.entry.PrimitiveEntry;
import software.coley.sourcesolver.resolve.generic.GenericType;
import software.coley.sourcesolver.resolve.generic.GenericTypeParameter;
import software.coley.sourcesolver.resolve.generic.GenericTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
	 * Stub class for cases where we cannot find information about a type in the workspace.
	 */
	private static class StubClassEntry extends BasicClassEntry {
		public StubClassEntry(@Nonnull String className, @Nonnull ClassEntry superEntry) {
			super(className, Opcodes.ACC_PUBLIC, superEntry, List.of(), List.of(), null, List.of(), null, List.of(), List.of(), List.of());
		}
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
		private final ClassEntry OBJECT_ENTRY;
		private final Set<String> hydratedRuntimePackages = ConcurrentHashMap.newKeySet();
		private final Workspace workspace;

		private WorkspaceBackedEntryPool(@Nonnull Workspace workspace) {
			this.workspace = workspace;

			OBJECT_ENTRY = Objects.requireNonNull(getClass("java/lang/Object"), "Failed to find java/lang/Object in the workspace, which is required for type resolution");

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
			Map<String, ClassEntry> entries = new LinkedHashMap<>();
			workspace.findClasses(c -> Objects.equals(packageName, c.getPackageName())).forEach(path -> {
				ClassEntry entry = computeEntry(path.getValue(), DEFAULT_TTL);
				if (entry != null)
					entries.put(entry.getName(), entry);
			});

			// For classes available in the runtime classpath (IE, for script writing support) we want
			// to fill in the dozens of 'service.*' imports and such.
			hydrateRuntimePackage(packageName);

			cache.values().stream()
					.filter(e -> Objects.equals(packageName, e.getPackageName()))
					.forEach(entry -> entries.putIfAbsent(entry.getName(), entry));
			return List.copyOf(entries.values());
		}

		/**
		 * Add classes from the runtime classpath to the pool, if we can access them.
		 *
		 * @param packageName
		 * 		Name of package to pull classes from, or {@code null} for the default package.
		 */
		private void hydrateRuntimePackage(@Nullable String packageName) {
			String packageKey = packageName == null ? "" : packageName;
			if (!hydratedRuntimePackages.add(packageKey))
				return;

			// Add system classes, then our own classpath classes.
			hydrateRuntimePackage(packageName, ClasspathUtil.getSystemClassSet());
			hydrateRuntimePackage(packageName, ClasspathUtil.getClasspathClassSet());
		}

		/**
		 * Add classes from the runtime classpath to the pool, if we can access them.
		 *
		 * @param packageName
		 * 		Name of package to pull classes from, or {@code null} for the default package.
		 * @param classNames
		 * 		Set of class names to pull from.
		 */
		private void hydrateRuntimePackage(@Nullable String packageName, @Nonnull NavigableSet<String> classNames) {
			// There isn't anything we need to support in the default package.
			if (packageName == null || packageName.isEmpty())
				return;

			// Because the class names are stored in a sorted set,
			// we can do a quick range query to pull out all classes in the package.
			String prefix = packageName + "/";
			for (String className : classNames.tailSet(prefix, true)) {
				if (!className.startsWith(prefix))
					break;
				if (className.indexOf('/', prefix.length()) >= 0)
					continue;
				hydrateRuntimeClass(className);
			}
		}

		/**
		 * Add a class from the runtime classpath to the pool, if we can access it.
		 *
		 * @param className
		 * 		Name of class to hydrate.
		 */
		private void hydrateRuntimeClass(@Nonnull String className) {
			// Workspace will always allow us to pull classes from the runtime classpath,
			// so we can just ask it for the class and if it's there we'll add it to the pool.
			ClassPathNode path = workspace.findClass(className);
			if (path != null)
				computeEntry(path.getValue(), DEFAULT_TTL);
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
			if (ttl <= 1)
				return null;

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
			String className = info.getName();
			ClassEntry entry = cache.get(className);
			if (entry != null)
				return entry;

			// Decrement TTL and if it reaches 0 we abort.
			if (--ttl <= 0)
				return null;

			// Construct the class entry model.
			//   NOTE: Parent types are fully computed regardless of TTL. The TTL reduction is used further below.
			String superName = info.getSuperName();
			ClassEntry superClass = superName == null ? null : getClass(superName);
			if (superName != null && superClass == null)
				superClass = new StubClassEntry(superName, OBJECT_ENTRY);
			List<ClassEntry> innerClasses = new ArrayList<>();
			List<ClassEntry> interfaces = new ArrayList<>();
			String outerClassName = info.getOuterClassName();
			ClassEntry outerClass = outerClassName != null && outerClassName.startsWith(className + '$') ? cache.get(outerClassName) : null;
			List<FieldEntry> fields = new ArrayList<>();
			List<MethodEntry> methods = new ArrayList<>();

			// Register a placeholder before parsing generic signatures so self-referential
			// bounds like Enum<E extends Enum<E>> can resolve the current class entry.
			entry = new BasicClassEntry(className, info.getAccess(), superClass, interfaces, innerClasses, outerClass,
					List.of(), null, List.of(), fields, methods);
			register(entry);
			ClassEntry objectEntry = className.equals("java/lang/Object") ? entry : lookupClassEntry("java/lang/Object", 1);
			int currentTtl = ttl;
			Function<String, ClassEntry> classProvider = name -> lookupClassEntry(name, currentTtl);
			GenericSignatureBridge.ClassSignatureData classSignature = GenericSignatureBridge.parseClassSignature(
					info, superClass, interfaces, objectEntry, classProvider);
			GenericSignatureBridge.TypeVariableScope classScope =
					GenericSignatureBridge.TypeVariableScope.of(className, objectEntry, null, classSignature.typeParameters());

			for (FieldMember field : info.getFields()) {
				GenericType fieldType = GenericSignatureBridge.parseFieldType(field, classScope, objectEntry, classProvider);
				fields.add(new BasicFieldEntry(field.getName(), field.getDescriptor(), field.getAccess(), fieldType));
			}
			for (MethodMember method : info.getMethods()) {
				GenericSignatureBridge.MethodSignatureData methodSignature = GenericSignatureBridge.parseMethodSignature(
						className, method, classScope, objectEntry, classProvider);
				methods.add(new BasicMethodEntry(method.getName(), method.getDescriptor(), method.getAccess(),
						methodSignature.returnType(), methodSignature.parameterTypes()));
			}

			entry = new BasicClassEntry(className, info.getAccess(), superClass, interfaces, innerClasses, outerClass,
					classSignature.typeParameters(), classSignature.genericSuperType(),
					classSignature.genericInterfaceTypes(), fields, methods);
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

		@Nonnull
		private ClassEntry lookupClassEntry(@Nonnull String name, int ttl) {
			ClassEntry entry = getClass(name, ttl);
			if (entry != null)
				return entry;
			return fallbackClassEntry(name);
		}

		@Nonnull
		private ClassEntry fallbackClassEntry(@Nonnull String name) {
			ClassEntry entry = cache.get(name);
			if (entry != null)
				return entry;
			if (name.equals("java/lang/Object"))
				return OBJECT_ENTRY;
			return new StubClassEntry(name, OBJECT_ENTRY);
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

	/**
	 * Translated from reflective logic in {@link GenericTypes}
	 * but adapted to use ASM's signature parser for our binary class models.
	 */
	private static final class GenericSignatureBridge {
		private GenericSignatureBridge() {}

		@Nonnull
		static ClassSignatureData parseClassSignature(@Nonnull ClassInfo info,
		                                              @Nullable ClassEntry rawSuperType,
		                                              @Nonnull List<ClassEntry> rawInterfaceTypes,
		                                              @Nonnull ClassEntry objectEntry,
		                                              @Nonnull Function<String, ClassEntry> classProvider) {
			GenericType.ClassType fallbackSuperType = rawSuperType == null ? null : GenericTypes.ofClass(rawSuperType);
			List<GenericType.ClassType> fallbackInterfaceTypes = rawInterfaceTypes.stream()
					.map(GenericTypes::ofClass)
					.toList();
			String signature = info.getSignature();
			if (signature == null || signature.isBlank())
				return new ClassSignatureData(List.of(), fallbackSuperType, fallbackInterfaceTypes);

			try {
				ClassSignatureParser parser = new ClassSignatureParser(info.getName(), objectEntry, classProvider,
						fallbackSuperType, fallbackInterfaceTypes);
				new SignatureReader(signature).accept(parser);
				return parser.finish();
			} catch (Throwable ignored) {
				return new ClassSignatureData(List.of(), fallbackSuperType, fallbackInterfaceTypes);
			}
		}

		@Nonnull
		static GenericType parseFieldType(@Nonnull FieldMember field,
		                                  @Nonnull TypeVariableScope classScope,
		                                  @Nonnull ClassEntry objectEntry,
		                                  @Nonnull Function<String, ClassEntry> classProvider) {
			String signature = field.getSignature();
			if (signature != null && !signature.isBlank()) {
				try {
					return parseTypeSignature(signature, classScope, objectEntry, classProvider);
				} catch (Throwable ignored) {
					// Fall through to the erased descriptor.
				}
			}
			return typeFromDescriptor(field.getDescriptor(), classProvider);
		}

		@Nonnull
		static MethodSignatureData parseMethodSignature(@Nonnull String ownerName,
		                                                @Nonnull MethodMember method,
		                                                @Nonnull TypeVariableScope classScope,
		                                                @Nonnull ClassEntry objectEntry,
		                                                @Nonnull Function<String, ClassEntry> classProvider) {
			String signature = method.getSignature();
			if (signature != null && !signature.isBlank()) {
				try {
					MethodSignatureParser parser = new MethodSignatureParser(ownerId(ownerName, method),
							classScope, objectEntry, classProvider);
					new SignatureReader(signature).accept(parser);
					return parser.finish();
				} catch (Throwable ignored) {
					// Fall through to the erased descriptor.
				}
			}
			return methodSignatureFromDescriptor(method.getDescriptor(), classProvider);
		}

		@Nonnull
		private static GenericType parseTypeSignature(@Nonnull String signature,
		                                              @Nonnull TypeVariableScope scope,
		                                              @Nonnull ClassEntry objectEntry,
		                                              @Nonnull Function<String, ClassEntry> classProvider) {
			TypeCapture capture = new TypeCapture();
			new SignatureReader(signature).acceptType(new TypeSignatureParser(scope, objectEntry, classProvider, capture::setType));
			return capture.requireType();
		}

		@Nonnull
		private static MethodSignatureData methodSignatureFromDescriptor(@Nonnull String descriptor,
		                                                                 @Nonnull Function<String, ClassEntry> classProvider) {
			Type methodType = Type.getMethodType(descriptor);
			List<GenericType> parameterTypes = new ArrayList<>(methodType.getArgumentTypes().length);
			for (Type argumentType : methodType.getArgumentTypes())
				parameterTypes.add(typeFromAsmType(argumentType, classProvider));
			return new MethodSignatureData(typeFromAsmType(methodType.getReturnType(), classProvider), parameterTypes);
		}

		@Nonnull
		private static GenericType typeFromDescriptor(@Nonnull String descriptor,
		                                              @Nonnull Function<String, ClassEntry> classProvider) {
			return typeFromAsmType(Type.getType(descriptor), classProvider);
		}

		@Nonnull
		private static GenericType typeFromAsmType(@Nonnull Type type,
		                                           @Nonnull Function<String, ClassEntry> classProvider) {
			return switch (type.getSort()) {
				case Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT,
				     Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE ->
						GenericTypes.ofPrimitive(PrimitiveEntry.getPrimitive(type.getDescriptor()));
				case Type.ARRAY -> {
					Type elementType = type;
					int dimensions = 0;
					while (elementType.getSort() == Type.ARRAY) {
						elementType = elementType.getElementType();
						dimensions++;
					}
					yield new GenericType.ArrayType(typeFromAsmType(elementType, classProvider), dimensions);
				}
				case Type.OBJECT -> GenericTypes.ofClass(classProvider.apply(type.getInternalName()));
				default -> throw new IllegalStateException("Unsupported descriptor sort: " + type.getSort());
			};
		}

		@Nonnull
		private static String ownerId(@Nonnull String ownerName, @Nonnull MethodMember method) {
			return ownerName + "#" + method.getName() + method.getDescriptor();
		}

		@Nullable
		private static GenericType.ClassType asClassType(@Nullable GenericType type) {
			return type instanceof GenericType.ClassType classType ? classType : null;
		}

		private record ClassSignatureData(@Nonnull List<GenericTypeParameter> typeParameters,
		                                  @Nullable GenericType.ClassType genericSuperType,
		                                  @Nonnull List<GenericType.ClassType> genericInterfaceTypes) {}

		private record MethodSignatureData(@Nonnull GenericType returnType,
		                                   @Nonnull List<GenericType> parameterTypes) {
			private MethodSignatureData {
				parameterTypes = List.copyOf(parameterTypes);
			}
		}

		private static final class TypeVariableScope {
			private final String fallbackOwnerId;
			private final ClassEntry objectEntry;
			private final TypeVariableScope parent;
			private final Map<String, GenericTypeParameter> variables = new LinkedHashMap<>();

			private TypeVariableScope(@Nonnull String fallbackOwnerId,
			                          @Nonnull ClassEntry objectEntry,
			                          @Nullable TypeVariableScope parent) {
				this.fallbackOwnerId = fallbackOwnerId;
				this.objectEntry = objectEntry;
				this.parent = parent;
			}

			@Nonnull
			static TypeVariableScope of(@Nonnull String fallbackOwnerId,
			                            @Nonnull ClassEntry objectEntry,
			                            @Nullable TypeVariableScope parent,
			                            @Nonnull List<GenericTypeParameter> variables) {
				TypeVariableScope scope = new TypeVariableScope(fallbackOwnerId, objectEntry, parent);
				for (GenericTypeParameter variable : variables)
					scope.put(variable);
				return scope;
			}

			void put(@Nonnull GenericTypeParameter parameter) {
				variables.put(parameter.name(), parameter);
			}

			@Nonnull
			GenericTypeParameter resolve(@Nonnull String name) {
				GenericTypeParameter parameter = variables.get(name);
				if (parameter != null)
					return parameter;
				if (parent != null)
					return parent.resolve(name);
				return new GenericTypeParameter(fallbackOwnerId, name, objectEntry);
			}

			@Nonnull
			ClassEntry objectEntry() {
				return objectEntry;
			}
		}

		private static final class ClassSignatureParser extends SignatureVisitor {
			private final String ownerName;
			private final ClassEntry objectEntry;
			private final Function<String, ClassEntry> classProvider;
			private final List<GenericTypeParameter> typeParameters = new ArrayList<>();
			private final Map<String, GenericTypeParameter> typeParameterMap = new LinkedHashMap<>();
			private final GenericType.ClassType fallbackSuperType;
			private final List<GenericType.ClassType> fallbackInterfaceTypes;
			private final List<GenericType.ClassType> interfaceTypes = new ArrayList<>();
			private String pendingTypeParameterName;
			private DescribableEntry pendingTypeParameterBound;
			private GenericType.ClassType superType;
			private boolean parsedSuperType;

			private ClassSignatureParser(@Nonnull String ownerName,
			                             @Nonnull ClassEntry objectEntry,
			                             @Nonnull Function<String, ClassEntry> classProvider,
			                             @Nullable GenericType.ClassType fallbackSuperType,
			                             @Nonnull List<GenericType.ClassType> fallbackInterfaceTypes) {
				super(RecafConstants.getAsmVersion());
				this.ownerName = ownerName;
				this.objectEntry = objectEntry;
				this.classProvider = classProvider;
				this.fallbackSuperType = fallbackSuperType;
				this.fallbackInterfaceTypes = fallbackInterfaceTypes;
			}

			@Override
			public void visitFormalTypeParameter(String name) {
				finishPendingTypeParameter();
				pendingTypeParameterName = name;
				pendingTypeParameterBound = objectEntry;
			}

			@Override
			public SignatureVisitor visitClassBound() {
				return new BoundCaptureVisitor(objectEntry, classProvider, typeParameterMap, this::acceptBound);
			}

			@Override
			public SignatureVisitor visitInterfaceBound() {
				return new BoundCaptureVisitor(objectEntry, classProvider, typeParameterMap, this::acceptBound);
			}

			@Override
			public SignatureVisitor visitSuperclass() {
				finishPendingTypeParameter();
				return new TypeSignatureParser(scope(), objectEntry, classProvider, type -> {
					parsedSuperType = true;
					superType = asClassType(type);
				});
			}

			@Override
			public SignatureVisitor visitInterface() {
				finishPendingTypeParameter();
				return new TypeSignatureParser(scope(), objectEntry, classProvider, type -> {
					GenericType.ClassType classType = asClassType(type);
					if (classType != null)
						interfaceTypes.add(classType);
				});
			}

			@Nonnull
			private ClassSignatureData finish() {
				finishPendingTypeParameter();
				GenericType.ClassType resolvedSuperType = parsedSuperType ? superType : fallbackSuperType;
				List<GenericType.ClassType> resolvedInterfaceTypes =
						interfaceTypes.isEmpty() ? fallbackInterfaceTypes : List.copyOf(interfaceTypes);
				return new ClassSignatureData(List.copyOf(typeParameters), resolvedSuperType, resolvedInterfaceTypes);
			}

			private void acceptBound(@Nonnull DescribableEntry bound) {
				if (pendingTypeParameterBound == objectEntry)
					pendingTypeParameterBound = bound;
			}

			private void finishPendingTypeParameter() {
				if (pendingTypeParameterName == null)
					return;
				GenericTypeParameter parameter = new GenericTypeParameter(ownerName, pendingTypeParameterName,
						pendingTypeParameterBound == null ? objectEntry : pendingTypeParameterBound);
				typeParameterMap.put(parameter.name(), parameter);
				typeParameters.add(parameter);
				pendingTypeParameterName = null;
				pendingTypeParameterBound = null;
			}

			@Nonnull
			private TypeVariableScope scope() {
				return TypeVariableScope.of(ownerName, objectEntry, null, List.copyOf(typeParameterMap.values()));
			}
		}

		private static final class MethodSignatureParser extends SignatureVisitor {
			private final String ownerId;
			private final TypeVariableScope classScope;
			private final ClassEntry objectEntry;
			private final Function<String, ClassEntry> classProvider;
			private final List<GenericType> parameterTypes = new ArrayList<>();
			private final Map<String, GenericTypeParameter> methodTypeParameterMap = new LinkedHashMap<>();
			private String pendingTypeParameterName;
			private DescribableEntry pendingTypeParameterBound;
			private GenericType returnType;

			private MethodSignatureParser(@Nonnull String ownerId,
			                              @Nonnull TypeVariableScope classScope,
			                              @Nonnull ClassEntry objectEntry,
			                              @Nonnull Function<String, ClassEntry> classProvider) {
				super(RecafConstants.getAsmVersion());
				this.ownerId = ownerId;
				this.classScope = classScope;
				this.objectEntry = objectEntry;
				this.classProvider = classProvider;
			}

			@Override
			public void visitFormalTypeParameter(String name) {
				finishPendingTypeParameter();
				pendingTypeParameterName = name;
				pendingTypeParameterBound = objectEntry;
			}

			@Override
			public SignatureVisitor visitClassBound() {
				return new BoundCaptureVisitor(objectEntry, classProvider, methodTypeParameterMap, this::acceptBound);
			}

			@Override
			public SignatureVisitor visitInterfaceBound() {
				return new BoundCaptureVisitor(objectEntry, classProvider, methodTypeParameterMap, this::acceptBound);
			}

			@Override
			public SignatureVisitor visitParameterType() {
				finishPendingTypeParameter();
				return new TypeSignatureParser(scope(), objectEntry, classProvider, parameterTypes::add);
			}

			@Override
			public SignatureVisitor visitReturnType() {
				finishPendingTypeParameter();
				return new TypeSignatureParser(scope(), objectEntry, classProvider, type -> returnType = type);
			}

			@Nonnull
			private MethodSignatureData finish() {
				finishPendingTypeParameter();
				return new MethodSignatureData(
						returnType == null ? GenericTypes.ofPrimitive(PrimitiveEntry.getPrimitive("V")) : returnType,
						parameterTypes);
			}

			private void acceptBound(@Nonnull DescribableEntry bound) {
				if (pendingTypeParameterBound == objectEntry)
					pendingTypeParameterBound = bound;
			}

			private void finishPendingTypeParameter() {
				if (pendingTypeParameterName == null)
					return;
				methodTypeParameterMap.put(pendingTypeParameterName, new GenericTypeParameter(ownerId,
						pendingTypeParameterName, pendingTypeParameterBound == null ? objectEntry : pendingTypeParameterBound));
				pendingTypeParameterName = null;
				pendingTypeParameterBound = null;
			}

			@Nonnull
			private TypeVariableScope scope() {
				return TypeVariableScope.of(ownerId, objectEntry, classScope, List.copyOf(methodTypeParameterMap.values()));
			}
		}

		private static final class BoundCaptureVisitor extends SignatureVisitor {
			private final ClassEntry objectEntry;
			private final Function<String, ClassEntry> classProvider;
			private final Map<String, GenericTypeParameter> typeParameters;
			private final java.util.function.Consumer<DescribableEntry> boundConsumer;
			private DescribableEntry resolvedBound;

			private BoundCaptureVisitor(@Nonnull ClassEntry objectEntry,
			                            @Nonnull Function<String, ClassEntry> classProvider,
			                            @Nonnull Map<String, GenericTypeParameter> typeParameters,
			                            @Nonnull java.util.function.Consumer<DescribableEntry> boundConsumer) {
				super(RecafConstants.getAsmVersion());
				this.objectEntry = objectEntry;
				this.classProvider = classProvider;
				this.typeParameters = typeParameters;
				this.boundConsumer = boundConsumer;
			}

			@Override
			public void visitBaseType(char descriptor) {
				resolvedBound = PrimitiveEntry.getPrimitive(String.valueOf(descriptor));
			}

			@Override
			public SignatureVisitor visitArrayType() {
				return new SignatureVisitor(api) {
					private int dimensions = 1;

					@Override
					public void visitBaseType(char descriptor) {
						resolvedBound = PrimitiveEntry.getPrimitive(String.valueOf(descriptor)).toArrayEntry(dimensions);
					}

					@Override
					public SignatureVisitor visitArrayType() {
						dimensions++;
						return this;
					}

					@Override
					public void visitClassType(String name) {
						resolvedBound = classProvider.apply(name).toArrayEntry(dimensions);
					}

					@Override
					public void visitTypeVariable(String name) {
						GenericTypeParameter parameter = typeParameters.get(name);
						resolvedBound = (parameter == null ? objectEntry : parameter.upperBound()).toArrayEntry(dimensions);
					}
				};
			}

			@Override
			public void visitClassType(String name) {
				resolvedBound = classProvider.apply(name);
			}

			@Override
			public void visitInnerClassType(String name) {
				if (resolvedBound instanceof ClassEntry classEntry)
					resolvedBound = classProvider.apply(classEntry.getName() + '$' + name);
			}

			@Override
			public void visitTypeVariable(String name) {
				GenericTypeParameter parameter = typeParameters.get(name);
				resolvedBound = parameter == null ? objectEntry : parameter.upperBound();
			}

			@Override
			public void visitEnd() {
				if (resolvedBound != null)
					boundConsumer.accept(resolvedBound);
			}
		}

		private static final class TypeSignatureParser extends SignatureVisitor {
			private final TypeVariableScope scope;
			private final ClassEntry objectEntry;
			private final Function<String, ClassEntry> classProvider;
			private final java.util.function.Consumer<GenericType> typeConsumer;
			private String className;
			private List<GenericType> typeArguments;

			private TypeSignatureParser(@Nonnull TypeVariableScope scope,
			                            @Nonnull ClassEntry objectEntry,
			                            @Nonnull Function<String, ClassEntry> classProvider,
			                            @Nonnull java.util.function.Consumer<GenericType> typeConsumer) {
				super(RecafConstants.getAsmVersion());
				this.scope = scope;
				this.objectEntry = objectEntry;
				this.classProvider = classProvider;
				this.typeConsumer = typeConsumer;
			}

			@Override
			public void visitBaseType(char descriptor) {
				typeConsumer.accept(GenericTypes.ofPrimitive(PrimitiveEntry.getPrimitive(String.valueOf(descriptor))));
			}

			@Override
			public void visitTypeVariable(String name) {
				typeConsumer.accept(new GenericType.TypeVariableType(scope.resolve(name)));
			}

			@Override
			public SignatureVisitor visitArrayType() {
				return new TypeSignatureParser(scope, objectEntry, classProvider, type -> {
					if (type instanceof GenericType.ArrayType arrayType) {
						typeConsumer.accept(new GenericType.ArrayType(arrayType.elementType(), arrayType.dimensions() + 1));
					} else {
						typeConsumer.accept(new GenericType.ArrayType(type, 1));
					}
				});
			}

			@Override
			public void visitClassType(String name) {
				className = name;
				typeArguments = new ArrayList<>();
			}

			@Override
			public void visitInnerClassType(String name) {
				className = className + '$' + name;
				typeArguments = new ArrayList<>();
			}

			@Override
			public void visitTypeArgument() {
				typeArguments.add(new GenericType.WildcardType(null, null, objectEntry));
			}

			@Override
			public SignatureVisitor visitTypeArgument(char wildcard) {
				return new TypeSignatureParser(scope, objectEntry, classProvider, type -> {
					GenericType typeArgument = switch (wildcard) {
						case SignatureVisitor.EXTENDS -> new GenericType.WildcardType(type, null, type.asDescribable());
						case SignatureVisitor.SUPER -> new GenericType.WildcardType(null, type, objectEntry);
						default -> type;
					};
					typeArguments.add(typeArgument);
				});
			}

			@Override
			public void visitEnd() {
				ClassEntry classEntry = classProvider.apply(className);
				typeConsumer.accept(new GenericType.ClassType(classEntry,
						typeArguments == null ? List.of() : List.copyOf(typeArguments)));
			}
		}

		private static final class TypeCapture {
			private GenericType type;

			private void setType(@Nonnull GenericType type) {
				this.type = type;
			}

			@Nonnull
			private GenericType requireType() {
				if (type == null)
					throw new IllegalStateException("Missing parsed type");
				return type;
			}
		}
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			currentWorkspacePool = poolFromWorkspace(workspace);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			entryPoolCache.invalidate(workspace);
			currentWorkspacePool = EmptyEntryPool.INSTANCE;
		}
	}
}
