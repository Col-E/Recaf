package software.coley.recaf.services.window;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import software.coley.collections.observable.ObservableList;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.config.WindowScaleConfig;
import software.coley.recaf.ui.pane.ScalePane;
import software.coley.recaf.ui.window.IdentifiableStage;
import software.coley.recaf.util.NodeEvents;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
	private final WindowScaleConfig scaleConfig;
	private final ObservableList<Stage> activeWindows = new ObservableList<>();
	private final Map<String, Stage> windowMappings = new HashMap<>();
	private final Map<Stage, Screen> lastStageScreen = new IdentityHashMap<>();
	private final Map<Stage, Boolean> pendingStageRecentering = new IdentityHashMap<>();

	@Inject
	public WindowManager(@Nonnull WindowStyling windowStyling, @Nonnull WindowManagerConfig config,
	                     @Nonnull WindowScaleConfig scaleConfig, @Nonnull Instance<IdentifiableStage> stages) {
		this.windowStyling = windowStyling;
		this.config = config;
		this.scaleConfig = scaleConfig;

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

		applyScale(stage);

		// Add custom stylesheets if any are registered.
		if (!windowStyling.getStylesheetUris().isEmpty())
			NodeEvents.runOnceIfPresentOrOnChange(stage.sceneProperty(),
					scene -> scene.getStylesheets().addAll(windowStyling.getStylesheetUris()));

		// When a window is about to show, check if the main window has moved to a different screen since the last time
		// this subwindow was visible. If so, center the subwindow on the main window.
		stage.addEventFilter(WindowEvent.WINDOW_SHOWING, e -> {
			Stage mainWindow = windowMappings.get(WIN_MAIN);
			if (mainWindow == null || stage == mainWindow)
				return;

			Screen mainScreen = getScreenForStage(mainWindow);
			if (mainScreen == null)
				return;

			// Schedule a recenter if the main window has moved since the last time this stage was visible.
			// We can't do this yet since the stage content hasn't been sized.
			Screen lastScreen = lastStageScreen.get(stage);
			if (lastScreen == null || mainScreen != lastScreen)
				pendingStageRecentering.put(stage, true);
		});

		// Record when windows are 'active' based on visibility.
		// We're using event filters so users can still do things like 'stage.setOnShown(...)' and not interfere with
		// our window tracking logic in here
		stage.addEventFilter(WindowEvent.WINDOW_SHOWN, e -> {
			logger.trace("Stage showing: {}", id);
			activeWindows.add(stage);

			// Check if we had a pending recenter for this stage.
			// Since we're in the 'shown' event, the stage's content should be properly sized and ready to be centered if needed.
			if (pendingStageRecentering.remove(stage) != null) {
				Stage mainWindow = windowMappings.get(WIN_MAIN);
				if (mainWindow != null && stage != mainWindow) {
					Screen mainScreen = getScreenForStage(mainWindow);
					Dimension2D size = getStageSize(stage);
					if (mainScreen != null && size != null)
						recenterStage(stage, mainWindow, mainScreen, size);
				}
			}

			// Track the screen the stage is currently on so we can detect moves for future re-centers.
			lastStageScreen.put(stage, getScreenForStage(stage));
		});
		stage.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> {
			logger.trace("Stage hiding: {}", id);
			activeWindows.remove(stage);
			pendingStageRecentering.remove(stage);

			// Anonymous stages can be pruned from the id->stage map.
			// They are not meant to be persistent. But, we register them anyway for our duplicate register check above.
			if (id.startsWith(ANON_PREFIX)) {
				logger.trace("Stage pruned: {} ({})", id, stage.getTitle());
				windowMappings.remove(id);
				lastStageScreen.remove(stage);
			}
		});

		// If state is already visible, add it right away.
		if (stage.isShowing()) activeWindows.add(stage);

		// Register id --> stage
		windowMappings.put(id, stage);
		logger.trace("Register stage: {}", id);
	}

	/**
	 * Wraps the stage's scene root in a {@link ScalePane}
	 */
	private void applyScale(@Nonnull Stage stage) {
		var scale = scaleConfig.scaleProperty();
		stage.sceneProperty().addListener((_, _, scene) -> wrapSceneRoot(scene, scale));
		wrapSceneRoot(stage.getScene(), scale);
	}

	private static void wrapSceneRoot(@Nullable Scene scene, @Nonnull DoubleProperty scale) {
		if (scene == null)
			return;

		var root = scene.getRoot();
		if (root == null || root instanceof ScalePane)
			return;

		scene.setRoot(new ScalePane(root, scale));
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

	/**
	 * Centers the stage on the main window, ensuring it stays within the bounds of the screen.
	 *
	 * @param stage
	 * 		Stage to center.
	 * @param mainWindow
	 * 		Main window to center on.
	 * @param mainScreen
	 * 		Main window's screen, used for bounds checking.
	 * @param stageSize
	 * 		Size of the stage to center.
	 */
	private static void recenterStage(@Nonnull Stage stage, @Nonnull Stage mainWindow, @Nonnull Screen mainScreen,
	                                  @Nonnull Dimension2D stageSize) {
		Rectangle2D bounds = mainScreen.getVisualBounds();
		double centeredX = mainWindow.getX() + ((mainWindow.getWidth() - stageSize.getWidth()) / 2);
		double centeredY = mainWindow.getY() + ((mainWindow.getHeight() - stageSize.getHeight()) / 2);
		double maxX = Math.max(bounds.getMinX(), bounds.getMaxX() - stageSize.getWidth());
		double maxY = Math.max(bounds.getMinY(), bounds.getMaxY() - stageSize.getHeight());

		stage.setX(Math.clamp(centeredX, bounds.getMinX(), maxX));
		stage.setY(Math.clamp(centeredY, bounds.getMinY(), maxY));
	}

	/**
	 * Determines which {@link Screen} the given stage's center is on.
	 *
	 * @param stage
	 * 		Stage to find the screen for.
	 *
	 * @return Screen containing the stage's center point.
	 */
	@Nullable
	private Screen getScreenForStage(@Nonnull Stage stage) {
		double centerX = stage.getX() + (stage.getWidth() / 2);
		double centerY = stage.getY() + (stage.getHeight() / 2);
		for (Screen screen : Screen.getScreens()) {
			// Check true center, and then top center (in case stage is dragged near the bottom of the screen).
			Rectangle2D bounds = screen.getBounds();
			if (bounds.contains(centerX, centerY) || bounds.contains(centerX, stage.getY()))
				return screen;
		}
		return null;
	}

	/**
	 * Attempts to get the size of the stage.
	 *
	 * @param stage
	 * 		Stage to get the size of.
	 *
	 * @return Stage size, if known.
	 */
	@Nullable
	private static Dimension2D getStageSize(@Nonnull Stage stage) {
		double width = stage.getWidth();
		double height = stage.getHeight();
		if (isRealized(width) && isRealized(height))
			return new Dimension2D(width, height);

		// Fallback to min-size if set.
		width = stage.getMinWidth();
		height = stage.getMinHeight();
		if (isRealized(width) && isRealized(height))
			return new Dimension2D(width, height);

		return null;
	}

	private static boolean isRealized(double value) {
		return Double.isFinite(value) && value > 0;
	}
}
