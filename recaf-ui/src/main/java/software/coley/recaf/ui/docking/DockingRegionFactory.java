package software.coley.recaf.ui.docking;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import com.panemu.tiwulfx.control.dock.DetachableTabPaneFactory;
import jakarta.annotation.Nonnull;
import javafx.scene.control.TabPane;
import javafx.stage.WindowEvent;
import software.coley.recaf.ui.window.RecafScene;

/**
 * Factory for {@link DockingRegion}.
 * Handles callbacks back to {@link DockingManager}.
 *
 * @author Matt Coley
 */
public class DockingRegionFactory extends DetachableTabPaneFactory {
	private static final int SCENE_WIDTH = 600;
	private static final int SCENE_HEIGHT = 400;
	private final DockingManager manager;

	/**
	 * @param manager
	 * 		Associated docking manager.
	 */
	public DockingRegionFactory(@Nonnull DockingManager manager) {
		this.manager = manager;
	}

	@Override
	protected DockingRegion create() {
		return new DockingRegion(manager);
	}

	@Override
	protected void init(DetachableTabPane newTabPane) {
		newTabPane.setSceneFactory(param -> new RecafScene(param, SCENE_WIDTH, SCENE_HEIGHT));
		newTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
		newTabPane.setCloseIfEmpty(true);
		newTabPane.setDetachableTabPaneFactory(this);

		// Register closure callbacks
		if (newTabPane instanceof DockingRegion region) {
			region.setOnRemove(pane -> {
				// We can ignore the return value of 'onRegionClose' since by this point
				// the docking region should have no tabs. That is why its being removed.
				if (region.isCloseIfEmpty())
					manager.onRegionClose(region);
			});

			// When the containing window is closed, trigger the region closure callback.
			region.sceneProperty().addListener((obS, oldScene, newScene) -> {
				if (newScene != null) {
					newScene.windowProperty().addListener((obW, oldWindow, newWindow) ->
							newWindow.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
								if (!manager.onRegionClose(region))
									e.consume();
							}));
				}
			});
		}
	}
}
