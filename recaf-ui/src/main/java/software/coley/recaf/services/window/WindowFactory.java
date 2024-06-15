package software.coley.recaf.services.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.window.RecafStage;

/**
 * Creates new windows.
 *
 * @author Matt Coley
 * @see WindowManager Manages which windows are active.
 */
@ApplicationScoped
public class WindowFactory implements Service {
	public static final String SERVICE_ID = "window-factory";
	private final WindowFactoryConfig config;
	private final WindowManager windowManager;

	@Inject
	public WindowFactory(@Nonnull WindowFactoryConfig config, @Nonnull WindowManager windowManager) {
		this.config = config;
		this.windowManager = windowManager;
	}

	/**
	 * Create a stage around the scene and register it.
	 * <p>
	 * <b>Must be on FX thread when calling.</b>
	 *
	 * @param scene
	 * 		Scene to pass to the created stage.
	 * @param title
	 * 		Stage title binding.
	 * @param minWidth
	 * 		Minimum width of the stage.
	 * @param minHeight
	 * 		Minimum height of the stage.
	 *
	 * @return Created stage.
	 */
	@Nonnull
	public Stage createAnonymousStage(@Nonnull Scene scene, @Nonnull ObservableValue<String> title, int minWidth, int minHeight) {
		Stage stage = create(scene, minWidth, minHeight);
		stage.titleProperty().bind(title);
		windowManager.registerAnonymous(stage);
		return stage;
	}

	/**
	 * Create a stage around the scene and register it.
	 * <p>
	 * <b>Must be on FX thread when calling.</b>
	 *
	 * @param scene
	 * 		Scene to pass to the created stage.
	 * @param title
	 * 		Stage title text.
	 * @param minWidth
	 * 		Minimum width of the stage.
	 * @param minHeight
	 * 		Minimum height of the stage.
	 *
	 * @return Created stage.
	 */
	@Nonnull
	public Stage createAnonymousStage(@Nonnull Scene scene, @Nonnull String title, int minWidth, int minHeight) {
		Stage stage = create(scene, minWidth, minHeight);
		stage.setTitle(title);
		windowManager.registerAnonymous(stage);
		return stage;
	}

	@Nonnull
	private Stage create(@Nonnull Scene scene, int minWidth, int minHeight) {
		Stage stage = new RecafStage();
		stage.setScene(scene);
		stage.setMinWidth(minWidth);
		stage.setMinHeight(minHeight);
		return stage;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public WindowFactoryConfig getServiceConfig() {
		return config;
	}
}
