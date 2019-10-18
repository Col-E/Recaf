package me.coley.recaf.util;

import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.workspace.*;

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

	/**
	 * @param resource
	 * 		Resource instance.
	 *
	 * @return Icon path based on the type of resource.
	 */
	public static String getResourceIcon(JavaResource resource) {
		if(resource instanceof JarResource)
			return "icons/jar.png";
		else if(resource instanceof ClassResource)
			return "icons/binary.png";
		else if(resource instanceof UrlResource)
			return "icons/link.png";
		else if(resource instanceof MavenResource)
			return "icons/data.png";
		// TODO: Unique debug/agent icon?
		else if(resource instanceof DebuggerResource || resource instanceof InstrumentationResource)
			return "icons/data.png";
		return "icons/binary.png";
	}
}
