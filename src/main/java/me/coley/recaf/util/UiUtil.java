package me.coley.recaf.util;

import javafx.scene.Group;
import javafx.scene.Node;
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
			else if(Arrays.asList("jar", "war").contains(ext))
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
		if(resource instanceof ArchiveResource)
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
	 * @param access
	 * 		Class modifiers.
	 *
	 * @return Graphic representing class's attributes.
	 */
	public static Node createClassGraphic(int access) {
		Group g = new Group();
		// Root icon
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

	/**
	 * @param access
	 * 		Field modifiers.
	 *
	 * @return Graphic representing fields's attributes.
	 */
	public static Node createFieldGraphic(int access) {
		Group g = new Group();
		// Root icon
		String base = null;
		if(AccessFlag.isPublic(access))
			base = "icons/modifier/field_public.png";
		else if(AccessFlag.isProtected(access))
			base = "icons/modifier/field_protected.png";
		else if(AccessFlag.isPrivate(access))
			base = "icons/modifier/field_private.png";
		else
			base = "icons/modifier/field_default.png";
		g.getChildren().add(new IconView(base));
		// Add modifiers
		if(AccessFlag.isStatic(access))
			g.getChildren().add(new IconView("icons/modifier/static.png"));
		if(AccessFlag.isFinal(access))
			g.getChildren().add(new IconView("icons/modifier/final.png"));
		if(AccessFlag.isBridge(access) || AccessFlag.isSynthetic(access))
			g.getChildren().add(new IconView("icons/modifier/synthetic.png"));
		return g;
	}

	/**
	 * @param access
	 * 		Field modifiers.
	 *
	 * @return Graphic representing fields's attributes.
	 */
	public static Node createMethodGraphic(int access) {
		Group g = new Group();
		// Root icon
		String base = null;
		if(AccessFlag.isPublic(access))
			base = "icons/modifier/method_public.png";
		else if(AccessFlag.isProtected(access))
			base = "icons/modifier/method_protected.png";
		else if(AccessFlag.isPrivate(access))
			base = "icons/modifier/method_private.png";
		else
			base = "icons/modifier/method_default.png";
		g.getChildren().add(new IconView(base));
		// Add modifiers
		if(AccessFlag.isStatic(access))
			g.getChildren().add(new IconView("icons/modifier/static.png"));
		else if(AccessFlag.isNative(access))
			g.getChildren().add(new IconView("icons/modifier/native.png"));
		else if(AccessFlag.isAbstract(access))
			g.getChildren().add(new IconView("icons/modifier/abstract.png"));
		if(AccessFlag.isFinal(access))
			g.getChildren().add(new IconView("icons/modifier/final.png"));
		if(AccessFlag.isBridge(access) || AccessFlag.isSynthetic(access))
			g.getChildren().add(new IconView("icons/modifier/synthetic.png"));
		return g;
	}
}
