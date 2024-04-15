package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Manager of multiple {@link Decompiler} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DecompilerManager implements Service {
	public static final String SERVICE_ID = "decompilers";
	private static final NoopJvmDecompiler NO_OP_JVM = NoopJvmDecompiler.getInstance();
	private static final NoopAndroidDecompiler NO_OP_ANDROID = NoopAndroidDecompiler.getInstance();
	private final ExecutorService decompileThreadPool = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID);
	private final Map<String, JvmDecompiler> jvmDecompilers = new TreeMap<>();
	private final Map<String, AndroidDecompiler> androidDecompilers = new TreeMap<>();
	private final DecompilerManagerConfig config;
	private final ObservableObject<JvmDecompiler> targetJvmDecompiler;
	private final ObservableObject<AndroidDecompiler> targetAndroidDecompiler;

	// TODO: Maintain a list of JvmBytecodeFilters
	//  - Add to existing decompilers
	//  - Add to anything that gets added later
	//  - Support removal
	//  We can have a UI module that maps a Config<Boolean> to including filters such as:
	//  - illegal signature stripping
	//  - bogus annotation stripping
	//  while maintaining the content in the actual class.

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
		return CompletableFuture.supplyAsync(() -> decompiler.decompile(workspace, classInfo), decompileThreadPool);
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
		for (JvmDecompiler decompiler : jvmDecompilers.values()) {
			decompiler.addJvmBytecodeFilter(filter);
		}
	}

	/**
	 * Removes an input bytecode filter from all {@link JvmDecompiler} instances.
	 *
	 * @param filter
	 * 		Filter to remove.
	 */
	public void removeJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter) {
		for (JvmDecompiler decompiler : jvmDecompilers.values()) {
			decompiler.removeJvmBytecodeFilter(filter);
		}
	}

	/**
	 * Adds an output text filter to all {@link Decompiler} instances.
	 *
	 * @param filter
	 * 		Filter to add.
	 */
	public void addOutputTextFilter(@Nonnull OutputTextFilter filter) {
		for (JvmDecompiler decompiler : jvmDecompilers.values())
			decompiler.addOutputTextFilter(filter);
		for (AndroidDecompiler decompiler : androidDecompilers.values())
			decompiler.addOutputTextFilter(filter);
	}

	/**
	 * Removes an output text filter from all {@link Decompiler} instances.
	 *
	 * @param filter
	 * 		Filter to remove.
	 */
	public void removeOutputTextFilter(@Nonnull OutputTextFilter filter) {
		for (JvmDecompiler decompiler : jvmDecompilers.values())
			decompiler.removeOutputTextFilter(filter);
		for (AndroidDecompiler decompiler : androidDecompilers.values())
			decompiler.removeOutputTextFilter(filter);
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
}
