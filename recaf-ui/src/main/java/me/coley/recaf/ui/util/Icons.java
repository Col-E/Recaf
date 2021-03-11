package me.coley.recaf.ui.util;

import javafx.scene.Node;
import me.coley.recaf.RecafUI;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.workspace.resource.CommonClassInfo;
import me.coley.recaf.workspace.resource.DexClassInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.Opcodes;

/**
 * Icon and graphic utilities.
 *
 * @author Matt Coley
 */
public class Icons {
	public static final String FOLDER_SRC = "icons/file/folder-source.png";
	public static final String FOLDER_RES = "icons/file/folder-resource.png";
	public static final String FOLDER_PACKAGE = "icons/file/folder-package.png";
	public static final String FOLDER = "icons/file/folder.png";
	//
	public static final String CLASS = "icons/class/class.png";
	public static final String CLASS_ANONYMOUS = "icons/class/class_anonymous.png";
	public static final String CLASS_ABSTRACT = "icons/class/class_abstract.png";
	public static final String CLASS_EXCEPTION = "icons/class/class_exception.png";
	public static final String CLASS_ABSTRACT_EXCEPTION = "icons/class/class_abstract_exception.png";
	public static final String ANNOTATION = "icons/class/annotation.png";
	public static final String INTERFACE = "icons/class/interface.png";
	public static final String ENUM = "icons/class/enum.png";
	//
	public static final String FILE_BINARY = "icons/file/binary.png";
	//
	public static final String LOGO = "icons/logo.png";

	/**
	 * @param resource
	 * 		Resource to represent.
	 *
	 * @return Node to represent the resource.
	 */
	public static Node getIconForResource(Resource resource) {
		// TODO: Different icons for different content sources
		if (resource.getDexClasses().isEmpty()) {
			return new IconView("icons/file/jar.png");
		} else {
			return new IconView("icons/android.png");
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
			return new IconView(Icons.ANNOTATION);
		} else if ((info.getAccess() & Opcodes.ACC_INTERFACE) > 0) {
			return new IconView(Icons.INTERFACE);
		} else if ((info.getAccess() & Opcodes.ACC_ENUM) > 0) {
			return new IconView(Icons.ENUM);
		}
		// Normal class, consider other edge cases
		boolean isAbstract = (info.getAccess() & Opcodes.ACC_ABSTRACT) > 0;
		if (!getGraph().getCommon(info.getName(), "java/lang/Throwable").equals("java/lang/Object")) {
			return new IconView(isAbstract ? Icons.CLASS_ABSTRACT_EXCEPTION : Icons.CLASS_EXCEPTION);
		} else if (info.getName().matches(".+\\$\\d+")) {
			return new IconView(Icons.CLASS_ANONYMOUS);
		} else if (isAbstract) {
			return new IconView(Icons.CLASS_ABSTRACT);
		}
		// Default, normal class
		return new IconView(Icons.CLASS);
	}

	/**
	 * @return Current {@link InheritanceGraph}.
	 */
	private static InheritanceGraph getGraph() {
		return RecafUI.getController().getServices().getInheritanceGraph();
	}
}
