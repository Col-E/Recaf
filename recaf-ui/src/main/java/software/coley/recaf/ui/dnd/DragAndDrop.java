package software.coley.recaf.ui.dnd;

import jakarta.annotation.Nonnull;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Drag and drop utilities.
 *
 * @author Matt Coley
 */
public class DragAndDrop {
	private static final Logger logger = Logging.get(DragAndDrop.class);

	/**
	 * Install drag-drop support on the given region.
	 *
	 * @param region
	 * 		Control to support.
	 * @param listener
	 * 		Behavior when file content is dropped into the region.
	 */
	public static void installFileSupport(@Nonnull Region region, @Nonnull FileDropListener listener) {
		region.setOnDragOver(e -> onDragOver(region, e));
		region.setOnDragDropped(e -> onDragDropped(region, e, listener));
		region.setOnDragEntered(e -> onDragEntered(region, e, listener));
		region.setOnDragExited(e -> onDragExited(region, e, listener));
	}

	/**
	 * Handle drag-over.
	 *
	 * @param region
	 * 		Region dragged over.
	 * @param event
	 * 		Drag event.
	 */
	private static void onDragOver(@Nonnull Region region, @Nonnull DragEvent event) {
		// TODO: Some platforms may report files via 'text/uri-list'
		//  - Hard to test due to diversity of linux desktop environments, but seems to only be reported on linux.
		//  - In general we'd want to support 'hasUrl' as well here and below then map the uris to paths.
		if (event.getGestureSource() != region && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			event.consume();
		}
	}

	/**
	 * Pass drag-drop to listener.
	 *
	 * @param region
	 * 		Region dropped completed on.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drop completed.
	 */
	private static void onDragDropped(@Nonnull Region region, @Nonnull DragEvent event, @Nonnull FileDropListener listener) {
		Dragboard db = event.getDragboard();
		boolean success = true;
		if (db.hasFiles()) {
			try {
				List<Path> paths = db.getFiles().stream()
						.map(File::toPath)
						.collect(Collectors.toList());
				listener.onDragDrop(region, event, paths);
			} catch (IOException ex) {
				logger.error("Failed drag-and-drop due to IO", ex);
				success = false;
			} catch (Throwable ex) {
				logger.error("Failed drag-and-drop due to unhanded error", ex);
				success = false;
			}
			event.consume();
		}
		event.setDropCompleted(success);
	}

	/**
	 * Pass drag-entering to listener.
	 *
	 * @param region
	 * 		Region dragged over.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drag enters.
	 */
	private static void onDragEntered(@Nonnull Region region, @Nonnull DragEvent event, @Nonnull FileDropListener listener) {
		listener.onDragEnter(region, event);
		event.consume();
	}

	/**
	 * Pass drag-leaving to listener.
	 *
	 * @param region
	 * 		Region dragged over.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drag leaves.
	 */
	private static void onDragExited(@Nonnull Region region, @Nonnull DragEvent event, @Nonnull FileDropListener listener) {
		listener.onDragExit(region, event);
		event.consume();
	}
}