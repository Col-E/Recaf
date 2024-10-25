package software.coley.recaf.services.window;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import software.coley.collections.observable.ObservableList;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.window.IdentifiableStage;
import software.coley.recaf.util.NodeEvents;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages active {@link Stage} windows.
 *
 * @author Matt Coley
 * @see IdentifiableStage Type added to children of {@link Stage} to support automatic discovery.
 * @see WindowFactory Service for creating new windows.
 */
@ApplicationScoped
public class WindowManager implements Service {
	private static final Logger logger = Logging.get(WindowManager.class);
	public static final String SERVICE_ID = "window-manager";
	private static final String ANON_PREFIX = "anon-";
	// Built-in window keys
	public static final String WIN_MAIN = "main";
	public static final String WIN_REMOTE_VMS = "remote-vms";
	public static final String WIN_CONFIG = "config";
	public static final String WIN_INFO = "system-information";
	public static final String WIN_SCRIPTS = "script-manager";
	public static final String WIN_MAP_PROGRESS = "mapping-progress";
	public static final String WIN_QUICK_NAV = "quick-nav";
	// Manager instance data
	private final WindowStyling windowStyling;
	private final WindowManagerConfig config;
	private final ObservableList<Stage> activeWindows = new ObservableList<>();
	private final Map<String, Stage> windowMappings = new HashMap<>();

	@Inject
	public WindowManager(@Nonnull WindowStyling windowStyling, @Nonnull WindowManagerConfig config,
	                     @Nonnull Instance<IdentifiableStage> stages) {
		this.windowStyling = windowStyling;
		this.config = config;

		// Register identifiable stages.
		// These will be @Dependent scoped, so we need to be careful with their instances.
		// Interacting with named windows should always be done through this class to prevent duplicate allocations.
		for (IdentifiableStage stage : stages)
			register(stage.getId(), stage.asStage());
	}

	/**
	 * Register listeners on the stage to monitor active state.
	 *
	 * @param stage
	 * 		Stage to register.
	 */
	public void registerAnonymous(@Nonnull Stage stage) {
		register(ANON_PREFIX + UUID.randomUUID(), stage);
	}

	/**
	 * Registers listeners on the stage to monitor active state.
	 *
	 * @param identifiableStage
	 * 		Stage to register.
	 */
	public void register(@Nonnull IdentifiableStage identifiableStage) {
		register(identifiableStage.getId(), identifiableStage.asStage());
	}

	/**
	 * Register listeners on the stage to monitor active state.
	 *
	 * @param id
	 * 		Unique stage identifier.
	 * @param stage
	 * 		Stage to register.
	 */
	public void register(@Nonnull String id, @Nonnull Stage stage) {
		// Validate input, ensuring duplicate allocations are not allowed.
		if (windowMappings.containsKey(id))
			throw new IllegalStateException("The stage ID was already registered: " + id);

		// Add custom stylesheets if any are registered.
		if (!windowStyling.getStylesheetUris().isEmpty())
			NodeEvents.runOnceIfPresentOrOnChange(stage.sceneProperty(),
					scene -> scene.getStylesheets().addAll(windowStyling.getStylesheetUris()));

		// Record when windows are 'active' based on visibility.
		// We're using event filters so users can still do things like 'stage.setOnShown(...)' and not interfere with
		// our window tracking logic in here
		stage.addEventFilter(WindowEvent.WINDOW_SHOWN, e -> {
			logger.trace("Stage showing: {}", id);
			activeWindows.add(stage);
		});
		stage.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> {
			logger.trace("Stage hiding: {}", id);
			activeWindows.remove(stage);

			// Anonymous stages can be pruned from the id->stage map.
			// They are not meant to be persistent. But, we register them anyway for our duplicate register check above.
			if (id.startsWith(ANON_PREFIX)) {
				logger.trace("Stage pruned: {} ({})", id, stage.getTitle());
				windowMappings.remove(id);
			}
		});

		// If state is already visible, add it right away.
		if (stage.isShowing()) activeWindows.add(stage);

		// Register id --> stage
		windowMappings.put(id, stage);
		logger.trace("Register stage: {}", id);
	}

	/**
	 * Do not use this list to iterate over if within your loop you will be closing/creating windows.
	 * This will cause a {@link ConcurrentModificationException}. Wrap this result in a new collection
	 * if you want to do that.
	 *
	 * @return Active windows.
	 */
	@Nonnull
	public Collection<Stage> getActiveWindows() {
		return activeWindows;
	}

	/**
	 * @param id
	 * 		Window identifier.
	 *
	 * @return Window by ID,
	 * or {@code null} if no associated window for the ID exists.
	 */
	@Nullable
	public Stage getWindow(@Nonnull String id) {
		return windowMappings.get(id);
	}

	/**
	 * @return Window for main display.
	 */
	@Nonnull
	public Stage getMainWindow() {
		return Objects.requireNonNull(getWindow(WIN_MAIN));
	}

	/**
	 * @return Window for the remote VM list.
	 */
	@Nonnull
	public Stage getRemoteVmWindow() {
		return Objects.requireNonNull(getWindow(WIN_REMOTE_VMS));
	}

	/**
	 * @return Window for the config display.
	 */
	@Nonnull
	public Stage getConfigWindow() {
		return Objects.requireNonNull(getWindow(WIN_CONFIG));
	}

	/**
	 * @return Window for the system information display.
	 */
	@Nonnull
	public Stage getSystemInfoWindow() {
		return Objects.requireNonNull(getWindow(WIN_INFO));
	}

	/**
	 * @return Window for the system information display.
	 */
	@Nonnull
	public Stage getScriptManagerWindow() {
		return Objects.requireNonNull(getWindow(WIN_SCRIPTS));
	}

	/**
	 * @return Window for the current mapping preview display.
	 */
	@Nonnull
	public Stage getMappingPreviewWindow() {
		return Objects.requireNonNull(getWindow(WIN_MAP_PROGRESS));
	}

	/**
	 * @return Window for quick navigation display.
	 */
	@Nonnull
	public Stage getQuickNav() {
		return Objects.requireNonNull(getWindow(WIN_QUICK_NAV));
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public WindowManagerConfig getServiceConfig() {
		return config;
	}
}
