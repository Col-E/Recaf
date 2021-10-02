package me.coley.recaf.ui.behavior;

import me.coley.recaf.code.FileInfo;

/**
 * Children of this type represent a file, and thus should offer file oriented UX capabilities.
 *
 * @author Matt Coley
 */
public interface FileRepresentation extends Representation, Updatable<FileInfo> {
	/**
	 * @return Current file item being represented.
	 */
	FileInfo getCurrentFileInfo();
}
