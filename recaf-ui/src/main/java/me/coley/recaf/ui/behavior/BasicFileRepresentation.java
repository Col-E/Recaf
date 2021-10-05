package me.coley.recaf.ui.behavior;

import javafx.scene.Node;
import me.coley.recaf.code.FileInfo;

import java.util.function.Consumer;

/**
 * A basic implementation of {@link FileRepresentation} capable of covering most file representation cases
 * with just some constructor parameters. Any file being represented with this will be read-only and not editable.
 *
 * @author Matt Coley
 */
public class BasicFileRepresentation implements FileRepresentation {
	private final Node view;
	private final Consumer<FileInfo> onUpdate;
	private FileInfo info;

	/**
	 * @param view
	 * 		Display node.
	 * @param onUpdate
	 * 		File update consumer.
	 */
	public BasicFileRepresentation(Node view, Consumer<FileInfo> onUpdate) {
		this.view = view;
		this.onUpdate = onUpdate;
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		info = newValue;
		onUpdate.accept(newValue);
	}

	@Override
	public SaveResult save() {
		throw new UnsupportedOperationException("Basic representations do not support saving!");
	}

	@Override
	public boolean supportsEditing() {
		// Basic representations do not support editing.
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return view;
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return info;
	}
}
