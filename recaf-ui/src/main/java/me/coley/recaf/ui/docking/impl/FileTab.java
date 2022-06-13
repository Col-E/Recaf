package me.coley.recaf.ui.docking.impl;

import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.util.EscapeUtil;

/**
 * Wrapper around content representing a {@link me.coley.recaf.code.FileInfo}.
 *
 * @author Matt Coley
 * @see FileRepresentation
 */
public class FileTab extends DockTab {
	private final FileRepresentation fileRepresentation;

	/**
	 * @param title
	 * 		Title of the tab.
	 * @param fileRepresentation
	 * 		Representation of the file.
	 */
	public FileTab(String title, FileRepresentation fileRepresentation) {
		super(EscapeUtil.escape(title), fileRepresentation.getNodeRepresentation());
		this.fileRepresentation = fileRepresentation;
	}

	/**
	 * @return Representation of the file.
	 */
	public FileRepresentation getFileRepresentation() {
		return fileRepresentation;
	}
}
