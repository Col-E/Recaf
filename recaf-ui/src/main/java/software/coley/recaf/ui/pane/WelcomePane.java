package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.ui.dnd.DragAndDrop;
import software.coley.recaf.ui.dnd.FileDropListener;
import software.coley.recaf.ui.dnd.WorkspaceLoadingDropListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Pane displayed when first opening Recaf.
 *
 * @author Matt Coley
 */
@Dependent
public class WelcomePane extends BorderPane implements Navigable {
	@Inject
	public WelcomePane(@Nonnull WorkspaceLoadingDropListener listener) {
		DragAndDrop.installFileSupport(this, listener);

		// TODO: Content
		//   - Recent files
		//   - Tip of the day sorta thing?
		setCenter(new Label("welcome"));
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return PathNodes.unique("welcome");
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// no-op
	}
}
