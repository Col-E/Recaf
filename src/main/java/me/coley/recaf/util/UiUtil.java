package me.coley.recaf.util;

import java.util.Arrays;

/**
 * Utilities for UI functions.
 *
 * @author Matt
 */
public class UiUtil {
	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Path to icon based on file extension.
	 */
	public static String getFileIcon(String name) {
		String path = null;
		String ext = name.toLowerCase();
		if(ext.contains(".")) {
			ext = ext.substring(ext.lastIndexOf(".") + 1);
			if(Arrays.asList("txt", "mf", "properties").contains(ext))
				path = "icons/text.png";
			else if(Arrays.asList("json", "xml", "html", "css", "js").contains(ext))
				path = "icons/text-code.png";
			else if(Arrays.asList("png", "gif", "jpeg", "jpg", "bmp").contains(ext))
				path = "icons/image.png";
			else if("jar".equals(ext))
				path = "icons/jar.png";
		}
		if(path == null)
			path = "icons/binary.png";
		return path;
	}
}
