package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AudioFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.ImageFileInfo;
import software.coley.recaf.info.NativeLibraryFileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.info.VideoFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.pane.editing.binary.DecodingXmlPane;
import software.coley.recaf.ui.pane.editing.binary.ElfPane;
import software.coley.recaf.ui.pane.editing.binary.PePane;
import software.coley.recaf.ui.pane.editing.binary.hex.HexAdapter;
import software.coley.recaf.ui.pane.editing.binary.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.media.AudioPane;
import software.coley.recaf.ui.pane.editing.media.ImagePane;
import software.coley.recaf.ui.pane.editing.media.VideoPane;
import software.coley.recaf.ui.pane.editing.text.TextPane;
import software.coley.recaf.util.ByteHeaderUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static software.coley.recaf.ui.pane.editing.FileDisplayMode.*;

/**
 * Displays various kinds of {@link FileInfo} content by delegating to another view based on the file type.
 *
 * @author Matt Coley
 */
@Dependent
public class FilePane extends AbstractContentPane<FilePathNode> implements FileNavigable {
	private static final Logger logger = Logging.get(FilePane.class);
	private final Instance<TextPane> textProvider;
	private final Instance<ImagePane> imageProvider;
	private final Instance<VideoPane> videoProvider;
	private final Instance<AudioPane> audioProvider;
	private final Instance<PePane> execPeProvider;
	private final Instance<ElfPane> execElfProvider;
	private final Instance<DecodingXmlPane> binaryXmlProvider;
	private final HexConfig hexConfig;
	private final KeybindingConfig keys;
	private final List<FileDisplayMode> fileDisplayModes = new ArrayList<>();
	private final EventHandler<KeyEvent> hexKeyAdapter = this::handleKeys;
	private FileDisplayMode mode = HEX;

	@Inject
	public FilePane(@Nonnull Instance<TextPane> textProvider,
	                @Nonnull Instance<AudioPane> audioProvider,
	                @Nonnull Instance<ImagePane> imageProvider,
	                @Nonnull Instance<VideoPane> videoProvider,
	                @Nonnull Instance<PePane> execPeProvider,
	                @Nonnull Instance<ElfPane> execElfProvider,
	                @Nonnull Instance<DecodingXmlPane> binaryXmlProvider,
	                @Nonnull HexConfig hexConfig,
	                @Nonnull KeybindingConfig keys) {
		this.textProvider = textProvider;
		this.audioProvider = audioProvider;
		this.imageProvider = imageProvider;
		this.videoProvider = videoProvider;
		this.execPeProvider = execPeProvider;
		this.execElfProvider = execElfProvider;
		this.binaryXmlProvider = binaryXmlProvider;
		this.hexConfig = hexConfig;
		this.keys = keys;

		addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeys);
	}

	public void setupForFileType(@Nonnull FileInfo info) {
		switch (info) {
			case TextFileInfo textFileInfo -> setFileDisplayModes(List.of(TEXT, HEX));
			case BinaryXmlFileInfo binaryXmlFileInfo -> setFileDisplayModes(List.of(TEXT_BINARY_XML, HEX));
			case VideoFileInfo videoFileInfo -> setFileDisplayModes(List.of(VIDEO, HEX));
			case AudioFileInfo audioFileInfo -> setFileDisplayModes(List.of(AUDIO, HEX));
			case ImageFileInfo imageFileInfo -> setFileDisplayModes(List.of(IMAGE, HEX));
			case NativeLibraryFileInfo nativeLibraryFileInfo -> {
				// TODO: Do we want to further specify the type hierarchy for different kinds of native libs?
				//  - If we don't this kind of pattern may be repeated elsewhere
				if (ByteHeaderUtil.match(info.getRawContent(), ByteHeaderUtil.PE)) {
					setFileDisplayModes(List.of(EXECUTABLE_PE, HEX));
				} else if (ByteHeaderUtil.match(info.getRawContent(), ByteHeaderUtil.ELF)) {
					setFileDisplayModes(List.of(EXECUTABLE_ELF, HEX));
				} else {
					setFileDisplayModes(List.of(HEX));
				}
			}
			default -> setFileDisplayModes(List.of(HEX));
		}
	}

	@Nonnull
	public List<FileDisplayMode> getFileDisplayModes() {
		return Collections.unmodifiableList(fileDisplayModes);
	}

	public void setFileDisplayModes(@Nonnull List<FileDisplayMode> modes) {
		if (modes.isEmpty())
			return;

		fileDisplayModes.clear();
		fileDisplayModes.addAll(modes);

		if (!modes.isEmpty())
			setFileDisplayMode(modes.getFirst());
	}

	@Nonnull
	public FileDisplayMode getMode() {
		return mode;
	}

	public void setFileDisplayMode(@Nonnull FileDisplayMode mode) {
		if (!fileDisplayModes.contains(mode)) {
			logger.error("Cannot set mode to {}, supported modes are set to: {}", mode, fileDisplayModes);
			return;
		}

		this.mode = mode;
		refreshDisplay();
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (hasDisplay())
			return;

		// Update content in pane.
		switch (mode) {
			case HEX -> setDisplay(new HexAdapter(hexConfig));
			case TEXT -> setDisplay(textProvider.get());
			case TEXT_BINARY_XML -> setDisplay(binaryXmlProvider.get());
			case IMAGE -> setDisplay(imageProvider.get());
			case AUDIO -> setDisplay(audioProvider.get());
			case VIDEO -> setDisplay(videoProvider.get());
			case EXECUTABLE_PE -> setDisplay(execPeProvider.get());
			case EXECUTABLE_ELF -> setDisplay(execElfProvider.get());
			default -> throw new IllegalStateException("Unknown file mode: " + mode.name());
		}
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// Update if class has changed.
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			pathUpdateListeners.forEach(listener -> listener.accept(filePath));

			// Initialize UI if it has not been done yet.
			if (!hasDisplay())
				generateDisplay();

			// Notify children of change.
			getNavigableChildren().forEach(child -> {
				if (child instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(path);
			});
		}
	}

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}

	private void handleKeys(@Nonnull KeyEvent e) {
		if ((getDisplay() instanceof HexAdapter adapter)) {
			// Using event filter here because the hex-editor otherwise consumes key events.
			if (keys.getSave().match(e))
				ThreadUtil.run(adapter::save);
			else if (keys.getUndo().match(e)) {
				Bundle<?> bundle = path.getValueOfType(Bundle.class);
				if (bundle != null)
					bundle.decrementHistory(path.getValue().getName());
			}
		}
	}
}
