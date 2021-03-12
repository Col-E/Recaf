package me.coley.recaf.ui.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.workspace.resource.CommonClassInfo;
import me.coley.recaf.workspace.resource.MemberInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.Opcodes;

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
	public static final String FILE_JAR = "icons/file/jar.png";
	// Misc
	public static final String LOGO = "icons/logo.png";
	public static final String ANDROID = "icons/android.png";
	public static final String EYE = "icons/eye.png";
	public static final String EYE_DISABLED = "icons/eye-disabled.png";
	public static final String CASE_SENSITIVITY = "icons/case-sensitive.png";

	/**
	 * @param resource
	 * 		Resource to represent.
	 *
	 * @return Node to represent the resource.
	 */
	public static Node getResourceIcon(Resource resource) {
		// TODO: Different icons for different content sources
		if (!resource.getDexClasses().isEmpty()) {
			return new IconView(ANDROID);
		} else {
			return new IconView(FILE_JAR);
		}
	}

	/**
	 * @param info
	 * 		Class to represent.
	 *
	 * @return Node to represent the class.
	 */
	public static Node getClassIcon(CommonClassInfo info) {
		// TODO: Cleanup access usage once access utility is added to project
		if ((info.getAccess() & Opcodes.ACC_ANNOTATION) > 0) {
			return new IconView(ANNOTATION);
		} else if ((info.getAccess() & Opcodes.ACC_INTERFACE) > 0) {
			return new IconView(INTERFACE);
		} else if ((info.getAccess() & Opcodes.ACC_ENUM) > 0) {
			return new IconView(ENUM);
		}
		// Normal class, consider other edge cases
		boolean isAbstract = (info.getAccess() & Opcodes.ACC_ABSTRACT) > 0;
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
	public static Node getMethodIcon(MemberInfo method) {
		StackPane stack = new StackPane();
		if ((method.getAccess() & Opcodes.ACC_ABSTRACT) > 0) {
			stack.getChildren().add(new IconView(METHOD_ABSTRACT));
		} else {
			stack.getChildren().add(new IconView(METHOD));
		}
		if ((method.getAccess() & Opcodes.ACC_FINAL) > 0) {
			stack.getChildren().add(new IconView(ACCESS_FINAL));
		}
		if ((method.getAccess() & Opcodes.ACC_STATIC) > 0) {
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
	public static Node getFieldIcon(MemberInfo field) {
		StackPane stack = new StackPane();
		stack.getChildren().add(new IconView(FIELD));
		if ((field.getAccess() & Opcodes.ACC_FINAL) > 0) {
			stack.getChildren().add(new IconView(ACCESS_FINAL));
		}
		if ((field.getAccess() & Opcodes.ACC_STATIC) > 0) {
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
		if ((access & Opcodes.ACC_PRIVATE) > 0) {
			return new IconView(ACCESS_PRIVATE);
		} else if ((access & Opcodes.ACC_PROTECTED) > 0) {
			return new IconView(ACCESS_PROTECTED);
		} else if ((access & Opcodes.ACC_PUBLIC) > 0) {
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
