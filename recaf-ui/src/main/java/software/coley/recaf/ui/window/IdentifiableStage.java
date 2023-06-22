package software.coley.recaf.ui.window;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.stage.Stage;
import software.coley.recaf.services.window.WindowManager;

/**
 * Identifiable stage.
 * <br>
 * Implementations of this class are passed to the {@link WindowManager} upon CDI initialization.
 * Typically, these identifiable stages will {@link Inject} additional UI components often marked as {@link Dependent}.
 * Because these stages are managed by the window manager, if we operate with windows through the {@link WindowManager}
 * we can ensure we treat these stages with {@link Dependent} components as singletons.
 *
 * @author Matt Coley
 * @see AbstractIdentifiableStage Base class to use when creating stages.
 */
public interface IdentifiableStage {
	/**
	 * @return Unique stage ID.
	 */
	String getId();

	/**
	 * @return Self as stage.
	 */
	Stage asStage();
}
