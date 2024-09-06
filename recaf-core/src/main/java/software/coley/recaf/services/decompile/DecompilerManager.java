package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.weld.util.LazyValueHolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.visitors.*;
import software.coley.recaf.workspace.model.Workspace;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Manager of multiple {@link Decompiler} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DecompilerManager implements Service {
	public static final String SERVICE_ID = "decompilers";
	private static final DebuggingLogger logger = Logging.get(DecompilerManager.class);
	private static final NoopJvmDecompiler NO_OP_JVM = NoopJvmDecompiler.getInstance();
	private static final NoopAndroidDecompiler NO_OP_ANDROID = NoopAndroidDecompiler.getInstance();
	private final ExecutorService decompileThreadPool = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID);
	private final List<JvmBytecodeFilter> bytecodeFilters = new CopyOnWriteArrayList<>();
	private final List<OutputTextFilter> outputTextFilters = new CopyOnWriteArrayList<>();
	private final Map<String, JvmDecompiler> jvmDecompilers = new TreeMap<>();
	private final Map<String, AndroidDecompiler> androidDecompilers = new TreeMap<>();
	private final DecompilerManagerConfig config;
	private final ObservableObject<JvmDecompiler> targetJvmDecompiler;
	private final ObservableObject<AndroidDecompiler> targetAndroidDecompiler;

	/**
	 * @param config
	 * 		Config to pull values from.
	 * @param implementations
	 * 		CDI provider of decompiler implementations.
	 */
	@Inject
	public DecompilerManager(@Nonnull DecompilerManagerConfig config,
	                         @Nonnull Instance<Decompiler> implementations) {
		this.config = config;

		// Register implementations
		for (Decompiler implementation : implementations) {
			if (implementation instanceof JvmDecompiler jvmDecompiler) {
				register(jvmDecompiler);
			} else if (implementation instanceof AndroidDecompiler androidDecompiler) {
				register(androidDecompiler);
			}
		}

		ObservableString preferredJvmDecompiler = config.getPreferredJvmDecompiler();
		ObservableString preferredAndroidDecompiler = config.getPreferredAndroidDecompiler();

		// Mirror properties from config, mapped to instances
		targetJvmDecompiler = preferredJvmDecompiler
				.mapObject(key -> jvmDecompilers.getOrDefault(key == null ? "" : key, NO_OP_JVM));
		targetAndroidDecompiler = preferredAndroidDecompiler
				.mapObject(key -> androidDecompilers.getOrDefault(key == null ? "" : key, NO_OP_ANDROID));

		// Select first item if no value is present
		if (preferredJvmDecompiler.getValue() == null) {
			JvmDecompiler decompiler = jvmDecompilers.isEmpty() ?
					NO_OP_JVM : jvmDecompilers.values().iterator().next();
			preferredJvmDecompiler.setValue(decompiler.getName());
		}
		if (preferredAndroidDecompiler.getValue() == null) {
			AndroidDecompiler decompiler = androidDecompilers.isEmpty() ?
					NO_OP_ANDROID : androidDecompilers.values().iterator().next();
			preferredAndroidDecompiler.setValue(decompiler.getName());
		}
	}

	/**
	 * Uses the built-in thread-pool to schedule the decompilation with the {@link #getTargetJvmDecompiler()}.
	 *
	 * @param workspace
	 * 		Workspace to pull additional information from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Future of decompilation result.
	 */
	@Nonnull
	public CompletableFuture<DecompileResult> decompile(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo) {
		return decompile(getTargetJvmDecompiler(), workspace, classInfo);
	}

	/**
	 * Uses the built-in thread-pool to schedule the decompilation.
	 *
	 * @param decompiler
	 * 		Decompiler implementation to use.
	 * @param workspace
	 * 		Workspace to pull additional information from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Future of decompilation result.
	 */
	@Nonnull
	public CompletableFuture<DecompileResult> decompile(@Nonnull JvmDecompiler decompiler, @Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo) {
		return CompletableFuture.supplyAsync(() -> {
			boolean doCache = config.getCacheDecompilations().getValue();
			if (doCache) {
				// Check for cached result, returning the cached result if found
				// and only if the current config matches the one that yielded the cached result.
				DecompileResult cachedResult = CachedDecompileProperty.get(classInfo, decompiler);
				if (cachedResult != null) {
					if (cachedResult.getConfigHash() == decompiler.getConfig().getHash())
						return cachedResult;

					// Config changed, void the cache.
					CachedDecompileProperty.remove(classInfo);
				}
			}

			// We will use the layered filter manually here so any user requested cleanup is done before we pass the class to the decompiler.
			// The decompiler base implementation skips some work if there are no registered filters so doing it externally like this is
			// better for performance. If the user has no filtering enabled then no re-reads and re-writes are necessary.
			JvmClassInfo filteredClass = JvmBytecodeFilter.applyFilters(workspace, classInfo, Collections.singletonList(getLayeredJvmBytecodeFilter()));

			// Decompile and cache the results.
			DecompileResult result = decompiler.decompile(workspace, filteredClass);
			String decompilation = result.getText();
			if (decompilation != null && !outputTextFilters.isEmpty()) {
				// Apply output filters and re-wrap the result with the new output text.
				for (OutputTextFilter textFilter : outputTextFilters)
					decompilation = textFilter.filter(workspace, classInfo, decompilation);
				result = new DecompileResult(decompilation, result.getConfigHash());
			}
			if (doCache)
				CachedDecompileProperty.set(classInfo, decompiler, result);
			return result;
		}, decompileThreadPool);
	}

	/**
	 * Uses the built-in thread-pool to schedule the decompilation with the {@link #getTargetAndroidDecompiler()}.
	 *
	 * @param workspace
	 * 		Workspace to pull additional information from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Future of decompilation result.
	 */
	@Nonnull
	public CompletableFuture<DecompileResult> decompile(@Nonnull Workspace workspace, @Nonnull AndroidClassInfo classInfo) {
		return decompile(getTargetAndroidDecompiler(), workspace, classInfo);
	}

	/**
	 * Uses the built-in thread-pool to schedule the decompilation.
	 *
	 * @param decompiler
	 * 		Decompiler implementation to use.
	 * @param workspace
	 * 		Workspace to pull additional information from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Future of decompilation result.
	 */
	@Nonnull
	public CompletableFuture<DecompileResult> decompile(@Nonnull AndroidDecompiler decompiler, @Nonnull Workspace workspace, @Nonnull AndroidClassInfo classInfo) {
		return CompletableFuture.supplyAsync(() -> decompiler.decompile(workspace, classInfo), decompileThreadPool);
	}

	/**
	 * Adds an input bytecode filter to all {@link JvmDecompiler} instances.
	 *
	 * @param filter
	 * 		Filter to add.
	 */
	public void addJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter) {
		bytecodeFilters.add(filter);
	}

	/**
	 * Removes an input bytecode filter from all {@link JvmDecompiler} instances.
	 *
	 * @param filter
	 * 		Filter to remove.
	 */
	public void removeJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter) {
		bytecodeFilters.remove(filter);
	}

	/**
	 * Adds an output text filter to all {@link Decompiler} instances.
	 *
	 * @param filter
	 * 		Filter to add.
	 */
	public void addOutputTextFilter(@Nonnull OutputTextFilter filter) {
		outputTextFilters.add(filter);
	}

	/**
	 * Removes an output text filter from all {@link Decompiler} instances.
	 *
	 * @param filter
	 * 		Filter to remove.
	 */
	public void removeOutputTextFilter(@Nonnull OutputTextFilter filter) {
		outputTextFilters.remove(filter);
	}

	/**
	 * @return Preferred JVM decompiler.
	 */
	@Nonnull
	public JvmDecompiler getTargetJvmDecompiler() {
		return targetJvmDecompiler.getValue();
	}

	/**
	 * @return Preferred Android decompiler.
	 */
	@Nonnull
	public AndroidDecompiler getTargetAndroidDecompiler() {
		return targetAndroidDecompiler.getValue();
	}

	/**
	 * @param decompiler
	 * 		JVM decompiler to add.
	 */
	public void register(@Nonnull JvmDecompiler decompiler) {
		jvmDecompilers.put(decompiler.getName(), decompiler);
	}

	/**
	 * @param decompiler
	 * 		Android decompiler to add.
	 */
	public void register(@Nonnull AndroidDecompiler decompiler) {
		androidDecompilers.put(decompiler.getName(), decompiler);
	}

	/**
	 * @param name
	 * 		Name of decompiler.
	 *
	 * @return Decompiler instance, or {@code null} if nothing by the ID was found.
	 */
	@Nullable
	public JvmDecompiler getJvmDecompiler(@Nonnull String name) {
		return jvmDecompilers.get(name);
	}

	/**
	 * @param name
	 * 		Name of decompiler.
	 *
	 * @return Decompiler instance, or {@code null} if nothing by the ID was found.
	 */
	@Nullable
	public AndroidDecompiler getAndroidDecompiler(@Nonnull String name) {
		return androidDecompilers.get(name);
	}

	/**
	 * @return Available JVM class decompilers.
	 */
	@Nonnull
	public Collection<JvmDecompiler> getJvmDecompilers() {
		return jvmDecompilers.values();
	}

	/**
	 * @return Available android class decompilers.
	 */
	@Nonnull
	public Collection<AndroidDecompiler> getAndroidDecompilers() {
		return androidDecompilers.values();
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public DecompilerManagerConfig getServiceConfig() {
		return config;
	}

	@Nonnull
	private JvmBytecodeFilter getLayeredJvmBytecodeFilter() {
		return new JvmBytecodeFilter() {
			@Nonnull
			@Override
			public byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode) {
				// Apply filters to the input bytecode first
				for (JvmBytecodeFilter filter : bytecodeFilters)
					bytecode = filter.filter(workspace, initialClassInfo, bytecode);

				// Setup filtering based on config
				byte[] filteredBytecode = bytecode;
				LazyValueHolder<ClassReader> reader = LazyValueHolder.forSupplier(() -> new ClassReader(filteredBytecode));
				LazyValueHolder<ClassWriter> cw = LazyValueHolder.forSupplier(() -> {
					// In most cases we want to pass the class-reader along to the class-writer.
					// This will allow some operations to be sped up internally by ASM.
					//
					// However, we can't do this is we're filtering debug information since it blanket copies all
					// debug attribute information without checking what the class-reader flags are.
					// Thus, when we're pruning debug info, we should pass 'null'.
					ClassReader backing = config.getFilterDebug().getValue() ? null : reader.get();
					return new ClassWriter(backing, 0);
				});
				ClassVisitor cv = null;

				// The things you want to 'filter' first need to appear last in this chain since we're building a chain
				// of visitors which delegate from one onto another.
				if (config.getFilterNonAsciiNames().getValue()) {
					cv = cw.get();
					cv = BogusNameRemovingVisitor.create(workspace, cv);
				}
				if (config.getFilterLongAnnotations().getValue()) {
					if (cv == null) cv = cw.get();
					cv = new LongAnnotationRemovingVisitor(cv, config.getFilterLongAnnotationsLength().getValue());
				}
				if (config.getFilterDuplicateAnnotations().getValue()) {
					if (cv == null) cv = cw.get();
					cv = new DuplicateAnnotationRemovingVisitor(cv);
				}
				if (config.getFilterIllegalAnnotations().getValue()) {
					if (cv == null) cv = cw.get();
					cv = new IllegalAnnotationRemovingVisitor(cv);
				}
				if (config.getFilterHollow().getValue()) {
					if (cv == null) cv = cw.get();
					cv = new ClassHollowingVisitor(cv, EnumSet.allOf(ClassHollowingVisitor.Item.class));
				}
				if (config.getFilterSignatures().getValue()) {
					if (cv == null) cv = cw.get();
					cv = new IllegalSignatureRemovingVisitor(cv);
				}
				if (config.getFilterDebug().getValue() && cv == null)
					cv = cw.get();

				// If no filtering has been requested, we never need to initialize the reader or writer.
				// Just return the original bytecode passed in.
				if (cv == null)
					return bytecode;

				try {
					int readFlags = config.getFilterDebug().getValue() ? ClassReader.SKIP_DEBUG : 0;
					reader.get().accept(cv, readFlags);
					return cw.get().toByteArray();
				} catch (Throwable t) {
					logger.error("Error applying filters to class '{}'", initialClassInfo.getName(), t);
					return bytecode;
				}
			}
		};
	}
}
