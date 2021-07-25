package me.coley.recaf.ui.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.ResourceUtil;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icon and graphic utilities.
 *
 * @author Matt Coley
 */
public class Icons {
	// Class definitions
	public static final String CLASS = "icons/class/class.png";
	public static final String CLASS_ANONYMOUS = "icons/class/class_anonymous.png";
	public static final String CLASS_ABSTRACT = "icons/class/class_abstract.png";
	public static final String CLASS_EXCEPTION = "icons/class/class_exception.png";
	public static final String CLASS_ABSTRACT_EXCEPTION = "icons/class/class_abstract_exception.png";
	public static final String ANNOTATION = "icons/class/annotation.png";
	public static final String INTERFACE = "icons/class/interface.png";
	public static final String ENUM = "icons/class/enum.png";
	// Member definitions
	public static final String FIELD = "icons/member/field.png";
	public static final String METHOD = "icons/member/method.png";
	public static final String METHOD_ABSTRACT = "icons/member/method_abstract.png";
	// Access modifiers
	public static final String ACCESS_PUBLIC = "icons/modifier/public.png";
	public static final String ACCESS_PROTECTED = "icons/modifier/protected.png";
	public static final String ACCESS_PACKAGE = "icons/modifier/package.png";
	public static final String ACCESS_PRIVATE = "icons/modifier/private.png";
	public static final String ACCESS_FINAL = "icons/modifier/final.png";
	public static final String ACCESS_STATIC = "icons/modifier/static.png";
	// Folders
	public static final String FOLDER_SRC = "icons/file/folder-source.png";
	public static final String FOLDER_RES = "icons/file/folder-resource.png";
	public static final String FOLDER_PACKAGE = "icons/file/folder-package.png";
	public static final String FOLDER = "icons/file/folder.png";
	// Files
	public static final String FILE_BINARY = "icons/file/binary.png";
	public static final String FILE_ZIP = "icons/file/zip.png";
	public static final String FILE_JAR = "icons/file/jar.png";
	public static final String FILE_CLASS = "icons/file/class.png";
	// Misc
	public static final String LOGO = "icons/logo.png";
	public static final String ANDROID = "icons/android.png";
	public static final String EYE = "icons/eye.png";
	public static final String EYE_DISABLED = "icons/eye-disabled.png";
	public static final String CASE_SENSITIVITY = "icons/case-sensitive.png";

	private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Returns {@link IconView} that uses cached image
	 * for rendering.
	 *
	 * @param path path to the image.
	 *
	 * @return an icon.
	 */
	public static IconView getIconView(String path) {
		Image image = IMAGE_CACHE.get(path);
		if (image == null) {
			InputStream stream = ResourceUtil.resource(path);
			image = new Image(stream);
			Image cached = IMAGE_CACHE.putIfAbsent(path, image);
			if (cached != null) {
				IOUtil.closeQuietly(stream);
				image = cached;
			}
		}
		return new IconView(image);
	}

	/**
	 * @param path
	 * 		Path to file to represent.
	 *
	 * @return Node to represent the file.
	 */
	public static Node getPathIcon(Path path) {
		String name = path.toString();
		if (Files.isDirectory(path))
			return getIconView(FOLDER);
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			String ext = name.substring(dotIndex + 1).toLowerCase();
			switch (ext) {
				default:
				case "jar":
				case "war":
					return getIconView(FILE_JAR);
				case "class":
					return getIconView(FILE_CLASS);
				case "zip":
					return getIconView(FILE_ZIP);
			}
		}
		// Unknown
		return getIconView(FILE_BINARY);
	}

	/**
	 * @param resource
	 * 		Resource to represent.
	 *
	 * @return Node to represent the resource.
	 */
	public static Node getResourceIcon(Resource resource) {
		// Check if its an Android resource
		if (!resource.getDexClasses().isEmpty()) {
			return getIconView(ANDROID);
		}
		// Different icons for different content sources
		ContentSource src = resource.getContentSource();
		if (src instanceof JarContentSource || src instanceof WarContentSource || src instanceof MavenContentSource) {
			return getIconView(FILE_JAR);
		} else if (src instanceof DirectoryContentSource) {
			return getIconView(FOLDER);
		} else if (src instanceof ZipContentSource) {
			return getIconView(FILE_ZIP);
		} else if (src instanceof ClassContentSource) {
			if (resource.getClasses().isEmpty()) {
				// Fallback, this should not occur since the class content should contain exactly one item
				return getIconView(FILE_CLASS);
			} else {
				CommonClassInfo cls = resource.getClasses().values().iterator().next();
				return getClassIcon(cls);
			}
		}
		// Default to jar
		return getIconView(FILE_JAR);
	}

	/**
	 * @param info
	 * 		Class to represent.
	 *
	 * @return Node to represent the class.
	 */
	public static Node getClassIcon(CommonClassInfo info) {
		int access = info.getAccess();
		if (AccessFlag.isAnnotation(access)) {
			return getIconView(ANNOTATION);
		} else if (AccessFlag.isInterface(access)) {
			return getIconView(INTERFACE);
		} else if (AccessFlag.isEnum(access)) {
			return getIconView(ENUM);
		}
		// Normal class, consider other edge cases
		boolean isAbstract = AccessFlag.isAbstract(access);
		String name = info.getName();
		if (!getGraph().getCommon(name, "java/lang/Throwable").equals("java/lang/Object")) {
			return getIconView(isAbstract ? CLASS_ABSTRACT_EXCEPTION : CLASS_EXCEPTION);
		} else if (name.matches(".+\\$\\d+")) {
			return getIconView(CLASS_ANONYMOUS);
		} else if (isAbstract) {
			return getIconView(CLASS_ABSTRACT);
		}
		// Default, normal class
		return getIconView(CLASS);
	}

	/**
	 * @param method
	 * 		Method information.
	 *
	 * @return Node to represent the method's modifiers.
	 */
	public static Node getMethodIcon(MethodInfo method) {
		StackPane stack = new StackPane();
		int access = method.getAccess();
		ObservableList<Node> children = stack.getChildren();
		if (AccessFlag.isAbstract(access)) {
			children.add(getIconView(METHOD_ABSTRACT));
		} else {
			children.add(getIconView(METHOD));
		}
		if (AccessFlag.isFinal(access)) {
			children.add(getIconView(ACCESS_FINAL));
		}
		if (AccessFlag.isStatic(access)) {
			children.add(getIconView(ACCESS_STATIC));
		}
		return stack;
	}

	/**
	 * @param field
	 * 		Field information.
	 *
	 * @return Node to represent the field's modifiers.
	 */
	public static Node getFieldIcon(FieldInfo field) {
		StackPane stack = new StackPane();
		int access = field.getAccess();
		ObservableList<Node> children = stack.getChildren();
		children.add(getIconView(FIELD));
		if (AccessFlag.isFinal(access)) {
			children.add(getIconView(ACCESS_FINAL));
		}
		if (AccessFlag.isStatic(access)) {
			children.add(getIconView(ACCESS_STATIC));
		}
		return stack;
	}

	/**
	 * Get node to represent common visibility/access modifiers. Technically works on classes too.
	 *
	 * @param access
	 * 		Access modifiers.
	 *
	 * @return Node to represent the access modifier.
	 */
	public static Node getVisibilityIcon(int access) {
		if (AccessFlag.isPrivate(access)) {
			return getIconView(ACCESS_PRIVATE);
		} else if (AccessFlag.isProtected(access)) {
			return getIconView(ACCESS_PROTECTED);
		} else if (AccessFlag.isPublic(access)) {
			return getIconView(ACCESS_PUBLIC);
		}
		return getIconView(ACCESS_PACKAGE);
	}

	/**
	 * @return Current {@link InheritanceGraph}.
	 */
	private static InheritanceGraph getGraph() {
		return RecafUI.getController().getServices().getInheritanceGraph();
	}
}
