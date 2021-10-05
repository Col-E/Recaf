package me.coley.recaf.ui.control.hex;

import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.FileRepresentation;

/**
 * Extension of the hex viewer for file data.
 *
 * @author Matt Coley
 */
public class HexFileView extends HexView implements FileRepresentation {
	private FileInfo fileInfo;

	@Override
	public void onUpdate(FileInfo newValue) {
		this.fileInfo = newValue;
		onUpdate(newValue.getValue());
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return fileInfo;
	}
}
