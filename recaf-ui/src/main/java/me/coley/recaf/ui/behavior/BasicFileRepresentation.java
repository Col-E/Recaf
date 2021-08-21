package me.coley.recaf.ui.behavior;

import javafx.scene.Node;
import me.coley.recaf.code.FileInfo;

import java.util.function.Consumer;

/**
 * A basic implementation of {@link FileRepresentation} capable of covering most file representation cases
 * with just some constructor parameters.
 *
 * @author Matt Coley
 */
public class BasicFileRepresentation implements FileRepresentation {
	private final Node view;
	private final Consumer<FileInfo> onUpdate;

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
		onUpdate.accept(newValue);
	}

	@Override
	public Node getNodeRepresentation() {
		return view;
	}
}
