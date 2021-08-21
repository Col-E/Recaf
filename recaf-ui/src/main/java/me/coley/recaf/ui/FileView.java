package me.coley.recaf.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.BasicFileRepresentation;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.control.PannableImageView;
import me.coley.recaf.util.ByteHeaderUtil;

/**
 * Display for a {@link FileView}.
 *
 * @author Matt Coley
 */
public class FileView extends BorderPane implements FileRepresentation, Cleanable {
	private FileRepresentation mainView;
	private FileInfo info;

	/**
	 * @param info
	 * 		Initial state of the file to display.
	 */
	public FileView(FileInfo info) {
		this.info = info;
		mainView = createViewForFile(info);
		setCenter(mainView.getNodeRepresentation());
	}

	@Override
	public void cleanup() {
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		this.info = newValue;
		if (mainView != null) {
			mainView.onUpdate(newValue);
		}
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	private static FileRepresentation createViewForFile(FileInfo info) {
		byte[] content = info.getValue();
		// Create representation based on file header
		if (ByteHeaderUtil.matchAny(content, ByteHeaderUtil.IMAGE_HEADERS)) {
			PannableImageView imageView = new PannableImageView(content);
			return new BasicFileRepresentation(imageView, newInfo -> imageView.setImage(newInfo.getValue()));
		} else {
			// TODO: Fallback to hex
			Label label = new Label("TODO: Fallback file view");
			return new BasicFileRepresentation(label, newInfo -> {});
		}
	}
}
