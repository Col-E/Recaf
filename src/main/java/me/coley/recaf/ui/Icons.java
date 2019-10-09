package me.coley.recaf.ui;

import javafx.scene.Node;

import java.io.File;

/**
 * Utility for icons.
 *
 * @author Matt
 */
public class Icons {
	/**
	 * @param file
	 * 		Some file.
	 *
	 * @return Icon to represent the file type.
	 */
	public static Node getFileIcon(File file) {
		// Icon for folder
		if (file.isDirectory())
			return null;
		// TODO: Fetch icon based on file extension
		//  - Maybe have a utility wrapper for files?
		//  - Based on extension, store:
		//    - icon
		//    - text formatting (if text source)
		//       - like Code2HTML
		//    - misc file info
		//
		// - txt/json/other-text-types
		// - java
		// - jar
		// - class
		return null;
	}
}
