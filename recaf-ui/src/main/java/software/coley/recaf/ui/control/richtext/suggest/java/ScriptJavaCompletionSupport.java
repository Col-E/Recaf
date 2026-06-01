package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.fxmisc.richtext.model.PlainTextChange;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.member.BasicFieldMember;
import software.coley.recaf.info.member.BasicMethodMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.script.AugmentedSource;
import software.coley.recaf.services.script.ScriptSourceAugmentation;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.Parser;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;
import software.coley.sourcesolver.model.VariableModel;
import software.coley.sourcesolver.resolve.entry.BasicClassEntry;
import software.coley.sourcesolver.resolve.entry.BasicFieldEntry;
import software.coley.sourcesolver.resolve.entry.BasicMethodEntry;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;
import software.coley.sourcesolver.resolve.result.Resolution;
import software.coley.sourcesolver.util.Range;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Completion context for the script inputs.
 *
 * @author Matt Coley
 */
@Dependent
public class ScriptJavaCompletionSupport implements EditorComponent, Closing, JavaCompletionContext {
	private static final long REPARSE_ELAPSED_TIME = 2_000L;
	private final ExecutorService parseThreadPool = ThreadPoolFactory.newSingleThreadExecutor("script-java-parse");
	private final NavigableMap<Integer, Integer> offsetMap = new TreeMap<>();
	private final AstService astService;
	private final WorkspaceManager workspaceManager;
	private final Parser parser;
	private Future<?> lastFuture;
	private int lastSourceHash;
	private CompilationUnitModel unit;
	private ResolverAdapter resolver;
	private DeclaredClassInfo declaredClassInfo;
	private AugmentedSource augmentedSource;
	private Editor editor;

	@Inject
	public ScriptJavaCompletionSupport(@Nonnull AstService astService,
	                                   @Nonnull WorkspaceManager workspaceManager) {
		this.astService = astService;
		this.workspaceManager = workspaceManager;
		parser = astService.getSharedJavaParser();
	}

	@PreDestroy
	private void cleanup() {
		if (lastFuture != null)
			lastFuture.cancel(true);
		parseThreadPool.close();
	}

	@Nonnull
	@Override
	public Workspace getWorkspace() {
		return workspaceManager.getCurrent();
	}

	@Nullable
	@Override
	public CompilationUnitModel getUnit() {
		return unit;
	}

	@Nullable
	@Override
	public ResolverAdapter getResolver() {
		return resolver;
	}

	@Override
	public int mapCurrentPositionToAst(int pos) {
		AugmentedSource localAugmentedSource = augmentedSource;
		int adjusted = offset(pos);
		if (localAugmentedSource == null)
			return adjusted;
		return localAugmentedSource.mapOriginalToAugmented(adjusted);
	}

	@Nullable
	@Override
	public Resolution resolveRawPositionSilently(int pos) {
		ResolverAdapter localResolver = resolver;
		if (localResolver == null)
			return null;
		return localResolver.resolveAt(mapCurrentPositionToAst(pos), null);
	}

	@Nullable
	@Override
	public ClassPathNode getPath() {
		return null;
	}

	@Nullable
	@Override
	public DeclaredClassInfo getDeclaredClassInfo() {
		return declaredClassInfo;
	}

	@Override
	public boolean isCompletionAvailable() {
		return unit != null && resolver != null && declaredClassInfo != null;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;

		// Register text change listeners to trigger reparsing of the script source.
		editor.getTextChangeEventStream().addObserver(this::handleShortDurationChange);
		editor.getTextChangeEventStream().successionEnds(Duration.ofMillis(REPARSE_ELAPSED_TIME))
				.addObserver(e -> handleLongDurationChange());
		if (!editor.getText().isBlank())
			handleLongDurationChange();
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
	}

	@Override
	public void close() {
		if (!parseThreadPool.isShutdown())
			parseThreadPool.shutdownNow();
	}

	/**
	 * Handle updating the offset-map so that we do not need to do a full reparse of the source.
	 *
	 * @param change
	 * 		Text changed.
	 */
	private void handleShortDurationChange(@Nonnull PlainTextChange change) {
		offsetMap.merge(change.getPosition(), change.getNetLength(), Integer::sum);
	}

	/**
	 * Handle a full reparse of the source, updating the {@link #unit}.
	 */
	private void handleLongDurationChange() {
		// Skip if the editor or parser are not available.
		if (editor == null || parser == null || parseThreadPool.isShutdown())
			return;

		// Cancel any pending parse tasks, and submit a new one to reparse the source.
		if (lastFuture != null && !lastFuture.isDone())
			lastFuture.cancel(true);
		lastFuture = parseThreadPool.submit(ThreadUtil.wrap(() -> {
			Editor localEditor = editor;
			if (localEditor == null)
				return;

			// Skip if the source hasn't changed since the last time.
			// This may occur when the user inserts some text, then removes it, resulting in the original text again.
			String text = localEditor.getText();
			int textHash = text.hashCode();
			if (lastSourceHash == textHash)
				return;
			lastSourceHash = textHash;

			// Skip if the source is not a class script, as we only support that for now.
			// In the future we may want to either:
			// - Support shorthand scripts as well
			// - Deprecate shorthand scripts and only support class scripts, as they are more powerful and flexible.
			if (!ScriptSourceAugmentation.isClassScript(text)) {
				unit = null;
				resolver = null;
				declaredClassInfo = null;
				augmentedSource = null;
				offsetMap.clear();
				return;
			}

			// Perform the augmentation and parsing of the source.
			// This will give us a stable AST to work with for completion lookups.
			AugmentedSource newAugmentedSource = ScriptSourceAugmentation.augmentClassScript(text);
			CompilationUnitModel resultingUnit;
			synchronized (parser) {
				resultingUnit = parser.parse(newAugmentedSource.augmentedSource());
			}

			// If there are no declared classes, then the source is not valid, and we cannot provide completion.
			if (resultingUnit.getDeclaredClasses().isEmpty()) {
				unit = null;
				resolver = null;
				declaredClassInfo = null;
				augmentedSource = null;
				offsetMap.clear();
				return;
			}

			// Create a new resolver for the resulting AST, and extract the declared class info for the single declared class.
			Workspace workspace = workspaceManager.getCurrent();
			ResolverAdapter newResolver = astService.newJavaResolver(workspace, resultingUnit);
			String className = extractInternalClassName(newAugmentedSource);
			DeclaredClassInfo newDeclaredClassInfo = null;
			if (className != null) {
				// Build our source-solver class context for the script class.
				// This is just done to extract fields/methods from the source to build the following model.
				registerDeclaredClassContext(newResolver, className, null);

				// Build our declared class info model by using the resolver to find declared fields/methods in the source.
				newDeclaredClassInfo = buildDeclaredClassInfo(resultingUnit, newResolver, className, newAugmentedSource.augmentedSource());

				// Rebuild the source-solver context with information it gave us from the first pass.
				registerDeclaredClassContext(newResolver, className, newDeclaredClassInfo);
			}

			// Update the completion context with new values.
			unit = resultingUnit;
			resolver = newResolver;
			declaredClassInfo = newDeclaredClassInfo;
			augmentedSource = newAugmentedSource;
			offsetMap.clear();
		}));
	}

	@Nullable
	private static String extractInternalClassName(@Nonnull AugmentedSource augmentedSource) {
		String simpleName = ScriptSourceAugmentation.extractClassName(augmentedSource.augmentedSource());
		if (simpleName == null)
			return null;
		String packageName = augmentedSource.packageInternalName();
		return packageName.isEmpty() ? simpleName : packageName + "/" + simpleName;
	}

	/**
	 * Registers a class context for the declared class in the resolver, so that it can be found when resolving the source.
	 *
	 * @param resolver
	 * 		Resolver to register the class context in.
	 * @param internalClassName
	 * 		Internal name of the class to register
	 * @param declaredClassInfo
	 * 		Class model containing fields/methods to register in the context,
	 * 		or {@code null} to register an empty context with just the class name.
	 */
	private static void registerDeclaredClassContext(@Nonnull ResolverAdapter resolver,
	                                                 @Nonnull String internalClassName,
	                                                 @Nullable DeclaredClassInfo declaredClassInfo) {
		// Scripts generally do not have a superclass.
		// So for now we can just use 'Object'.
		EntryPool pool = resolver.getEntryPool();
		ClassEntry objectEntry = pool.getClass("java/lang/Object");

		// Extract fields/methods.
		List<FieldEntry> fields = declaredClassInfo == null ? List.of() : declaredClassInfo.fields().stream()
				.map(field -> (FieldEntry) new BasicFieldEntry(field.getName(), field.getDescriptor(), field.getAccess()))
				.toList();
		List<MethodEntry> methods = declaredClassInfo == null ? List.of() : declaredClassInfo.methods().stream()
				.map(method -> (MethodEntry) new BasicMethodEntry(method.getName(), method.getDescriptor(), method.getAccess()))
				.toList();

		// Build the entry and register it in the pool, so that the resolver can find it when resolving the source.
		BasicClassEntry entry = new BasicClassEntry(internalClassName,
				declaredClassInfo == null ? 0 : declaredClassInfo.access(),
				objectEntry,
				new ArrayList<>(),
				new ArrayList<>(),
				null,
				fields,
				methods);
		pool.register(entry);
		resolver.setClassContext(new StubClassInfo(internalClassName).asJvmClass());
	}

	/**
	 * Builds the declared class info model for the single declared class in the source.
	 *
	 * @param unit
	 * 		Compilation unit containing the declared class.
	 * @param resolver
	 * 		Resolver to use for resolving types in the source.
	 * @param internalClassName
	 * 		Internal name of the declared class.
	 * @param augmentedText
	 * 		Augmented source text.
	 *
	 * @return Model containing metadata about the declared class,
	 * such as its fields and methods, to be used for completion suggestions.
	 */
	@Nonnull
	private static DeclaredClassInfo buildDeclaredClassInfo(@Nonnull CompilationUnitModel unit,
	                                                        @Nonnull ResolverAdapter resolver,
	                                                        @Nonnull String internalClassName,
	                                                        @Nonnull String augmentedText) {
		Model declaredClass = unit.getDeclaredClasses().getFirst();
		List<FieldMember> fields = new ArrayList<>();
		List<MethodMember> methods = new ArrayList<>();
		String simpleClassName = simpleName(internalClassName);

		// Find all variables in the class that are defined at the top-level script class.
		for (VariableModel field : declaredClass.getRecursiveChildrenOfType(VariableModel.class)) {
			// Skip local variables in methods.
			if (field.getParentOfType(MethodModel.class) != null)
				continue;

			// Skip anything not in the top-level script class.
			if (!(field.getParent() == declaredClass))
				continue;

			String descriptor = resolver.descriptorOf(field);
			if (descriptor == null)
				descriptor = Types.OBJECT_TYPE.getDescriptor();
			fields.add(new BasicFieldMember(field.getName(), descriptor, null, 0, null));
		}

		// Find all methods in the class that are defined at the top-level script class/
		for (MethodModel method : declaredClass.getRecursiveChildrenOfType(MethodModel.class)) {
			// Skip anything not in the top-level script class.
			if (!(method.getParent() == declaredClass))
				continue;

			// Skip the constructor.
			String name = method.getName();
			if (simpleClassName.equals(name))
				continue;

			String descriptor = resolver.descriptorOf(method);
			if (descriptor == null)
				descriptor = "()V";
			methods.add(new BasicMethodMember(name, descriptor, null,
					accessFromRange(augmentedText, method.getModifiers().getRange()), List.of()));
		}

		return new DeclaredClassInfo(internalClassName, 0, List.copyOf(fields), List.copyOf(methods), List.of());
	}

	private static int accessFromRange(@Nonnull String text, @Nonnull Range range) {
		int begin = Math.max(0, Math.min(text.length(), range.begin()));
		int end = Math.max(begin, Math.min(text.length(), range.end()));
		String slice = text.substring(begin, end);
		int access = 0;
		if (slice.contains("public")) access |= Opcodes.ACC_PUBLIC;
		if (slice.contains("protected")) access |= Opcodes.ACC_PROTECTED;
		if (slice.contains("private")) access |= Opcodes.ACC_PRIVATE;
		if (slice.contains("static")) access |= Opcodes.ACC_STATIC;
		if (slice.contains("final")) access |= Opcodes.ACC_FINAL;
		if (slice.contains("abstract")) access |= Opcodes.ACC_ABSTRACT;
		return access;
	}

	private int offset(int index) {
		if (offsetMap.isEmpty())
			return index;
		NavigableMap<Integer, Integer> subOffsetMap = offsetMap.subMap(0, true, index, false);
		int offset = -subOffsetMap.values().stream().mapToInt(i -> i).sum();
		return index + offset;
	}

	@Nonnull
	private static String simpleName(@Nonnull String internalClassName) {
		int slash = internalClassName.lastIndexOf('/');
		int dollar = internalClassName.lastIndexOf('$');
		int index = Math.max(slash, dollar);
		return index >= 0 ? internalClassName.substring(index + 1) : internalClassName;
	}
}
