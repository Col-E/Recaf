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

	// TODO: Maintain a list of JvmInputFilters
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
	public CompletableFuture<DecompileResult> decompile(Workspace workspace, JvmClassInfo classInfo) {
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
	public CompletableFuture<DecompileResult> decompile(JvmDecompiler decompiler, Workspace workspace, JvmClassInfo classInfo) {
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
	public CompletableFuture<DecompileResult> decompile(Workspace workspace, AndroidClassInfo classInfo) {
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
	public CompletableFuture<DecompileResult> decompile(AndroidDecompiler decompiler, Workspace workspace, AndroidClassInfo classInfo) {
		return CompletableFuture.supplyAsync(() -> decompiler.decompile(workspace, classInfo), decompileThreadPool);
	}

	/**
	 * @return Preferred JVM decompiler.
	 */
	public JvmDecompiler getTargetJvmDecompiler() {
		return targetJvmDecompiler.getValue();
	}

	/**
	 * @return Preferred Android decompiler.
	 */
	public AndroidDecompiler getTargetAndroidDecompiler() {
		return targetAndroidDecompiler.getValue();
	}

	/**
	 * @param decompiler
	 * 		JVM decompiler to add.
	 */
	public void register(JvmDecompiler decompiler) {
		jvmDecompilers.put(decompiler.getName(), decompiler);
	}

	/**
	 * @param decompiler
	 * 		Android decompiler to add.
	 */
	public void register(AndroidDecompiler decompiler) {
		androidDecompilers.put(decompiler.getName(), decompiler);
	}

	/**
	 * @param name
	 * 		Name of decompiler.
	 *
	 * @return Decompiler instance, or {@code null} if nothing by the ID was found.
	 */
	@Nullable
	public JvmDecompiler getJvmDecompiler(String name) {
		return jvmDecompilers.get(name);
	}

	/**
	 * @param name
	 * 		Name of decompiler.
	 *
	 * @return Decompiler instance, or {@code null} if nothing by the ID was found.
	 */
	@Nullable
	public AndroidDecompiler getAndroidDecompiler(String name) {
		return androidDecompilers.get(name);
	}

	/**
	 * @return Available JVM class decompilers.
	 */
	public Collection<JvmDecompiler> getJvmDecompilers() {
		return jvmDecompilers.values();
	}

	/**
	 * @return Available android class decompilers.
	 */
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
