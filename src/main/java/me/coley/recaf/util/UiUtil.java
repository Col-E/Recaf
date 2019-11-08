package me.coley.recaf.util;

import javafx.scene.Group;
import javafx.scene.Node;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.workspace.*;
import org.objectweb.asm.ClassReader;

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
	 * 		Workspace resource instance.
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

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Icon representing type of file <i>(Based on extension)</i>
	 */
	public static IconView createFileGraphic(String name) {
		return new IconView(getFileIcon(name));
	}

	/**
	 * @param resource
	 * 		Workspace resource containing the class.
	 * @param name
	 * 		Class name.
	 *
	 * @return Graphic representing class's attributes.
	 */
	public static Node createClassGraphic(JavaResource resource, String name) {
		Group g = new Group();
		// Root icon
		int access = new ClassReader(resource.getClasses().get(name)).getAccess();
		String base = "icons/class/class.png";
		if(AccessFlag.isEnum(access))
			base = "icons/class/enum.png";
		else if(AccessFlag.isAnnotation(access))
			base = "icons/class/annotation.png";
		else if(AccessFlag.isInterface(access))
			base = "icons/class/interface.png";
		g.getChildren().add(new IconView(base));
		// Add modifiers
		if(AccessFlag.isFinal(access) && !AccessFlag.isEnum(access))
			g.getChildren().add(new IconView("icons/modifier/final.png"));
		if(AccessFlag.isAbstract(access) && !AccessFlag.isInterface(access))
			g.getChildren().add(new IconView("icons/modifier/abstract.png"));
		if(AccessFlag.isBridge(access) || AccessFlag.isSynthetic(access))
			g.getChildren().add(new IconView("icons/modifier/synthetic.png"));
		return g;
	}
}
