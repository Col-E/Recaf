package me.coley.recaf.ui.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.*;

import java.nio.file.Files;
import java.nio.file.Path;

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

	/**
	 * @param path
	 * 		Path to file to represent.
	 *
	 * @return Node to represent the file.
	 */
	public static Node getPathIcon(Path path) {
		String name = path.toString();
		if (Files.isDirectory(path))
			return new IconView(FOLDER);
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			String ext = name.substring(dotIndex + 1).toLowerCase();
			switch (ext) {
				default:
				case "jar":
				case "war":
					return new IconView(FILE_JAR);
				case "class":
					return new IconView(FILE_CLASS);
				case "zip":
					return new IconView(FILE_ZIP);
			}
		}
		// Unknown
		return new IconView(FILE_BINARY);
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
			return new IconView(ANDROID);
		}
		// Different icons for different content sources
		ContentSource src = resource.getContentSource();
		if (src instanceof JarContentSource || src instanceof WarContentSource || src instanceof MavenContentSource) {
			return new IconView(FILE_JAR);
		} else if (src instanceof DirectoryContentSource) {
			return new IconView(FOLDER);
		} else if (src instanceof ZipContentSource) {
			return new IconView(FILE_ZIP);
		} else if (src instanceof ClassContentSource) {
			if (resource.getClasses().isEmpty()) {
				// Fallback, this should not occur since the class content should contain exactly one item
				return new IconView(FILE_CLASS);
			} else {
				CommonClassInfo cls = resource.getClasses().values().iterator().next();
				return getClassIcon(cls);
			}
		}
		// Default to jar
		return new IconView(FILE_JAR);
	}

	/**
	 * @param info
	 * 		Class to represent.
	 *
	 * @return Node to represent the class.
	 */
	public static Node getClassIcon(CommonClassInfo info) {
		if (AccessFlag.isAnnotation(info.getAccess())) {
			return new IconView(ANNOTATION);
		} else if (AccessFlag.isInterface(info.getAccess())) {
			return new IconView(INTERFACE);
		} else if (AccessFlag.isEnum(info.getAccess())) {
			return new IconView(ENUM);
		}
		// Normal class, consider other edge cases
		boolean isAbstract = AccessFlag.isAbstract(info.getAccess());
		if (!getGraph().getCommon(info.getName(), "java/lang/Throwable").equals("java/lang/Object")) {
			return new IconView(isAbstract ? CLASS_ABSTRACT_EXCEPTION : CLASS_EXCEPTION);
		} else if (info.getName().matches(".+\\$\\d+")) {
			return new IconView(CLASS_ANONYMOUS);
		} else if (isAbstract) {
			return new IconView(CLASS_ABSTRACT);
		}
		// Default, normal class
		return new IconView(CLASS);
	}

	/**
	 * @param method
	 * 		Method information.
	 *
	 * @return Node to represent the method's modifiers.
	 */
	public static Node getMethodIcon(MethodInfo method) {
		StackPane stack = new StackPane();
		if (AccessFlag.isAbstract(method.getAccess())) {
			stack.getChildren().add(new IconView(METHOD_ABSTRACT));
		} else {
			stack.getChildren().add(new IconView(METHOD));
		}
		if (AccessFlag.isFinal(method.getAccess())) {
			stack.getChildren().add(new IconView(ACCESS_FINAL));
		}
		if (AccessFlag.isStatic(method.getAccess())) {
			stack.getChildren().add(new IconView(ACCESS_STATIC));
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
		stack.getChildren().add(new IconView(FIELD));
		if (AccessFlag.isFinal(field.getAccess())) {
			stack.getChildren().add(new IconView(ACCESS_FINAL));
		}
		if (AccessFlag.isStatic(field.getAccess())) {
			stack.getChildren().add(new IconView(ACCESS_STATIC));
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
			return new IconView(ACCESS_PRIVATE);
		} else if (AccessFlag.isProtected(access)) {
			return new IconView(ACCESS_PROTECTED);
		} else if (AccessFlag.isPublic(access)) {
			return new IconView(ACCESS_PUBLIC);
		}
		return new IconView(ACCESS_PACKAGE);
	}

	/**
	 * @return Current {@link InheritanceGraph}.
	 */
	private static InheritanceGraph getGraph() {
		return RecafUI.getController().getServices().getInheritanceGraph();
	}
}
