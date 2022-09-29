package me.coley.recaf.ui.docking.impl;

import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.util.TextDisplayUtil;

import java.util.function.Consumer;

/**
 * Wrapper around content representing a {@link me.coley.recaf.code.FileInfo}.
 *
 * @author Matt Coley
 * @see FileRepresentation
 */
public class FileTab extends DockTab implements FontSizeChangeable {
	private final FileRepresentation fileRepresentation;

	/**
	 * @param title
	 * 		Title of the tab.
	 * @param fileRepresentation
	 * 		Representation of the file.
	 */
	public FileTab(String title, FileRepresentation fileRepresentation) {
		super(TextDisplayUtil.shortenEscapeLimit(title), fileRepresentation.getNodeRepresentation());
		this.fileRepresentation = fileRepresentation;
	}

	/**
	 * @return Representation of the file.
	 */
	public FileRepresentation getFileRepresentation() {
		return fileRepresentation;
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		if (fileRepresentation instanceof FontSizeChangeable)
			((FontSizeChangeable) fileRepresentation).bindFontSize(property);
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		if (fileRepresentation instanceof FontSizeChangeable)
			((FontSizeChangeable) fileRepresentation).applyEventsForFontSizeChange(consumer);
	}
}
