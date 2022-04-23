package me.coley.recaf.ui.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.util.*;
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
	public static final String FILE_TEXT = "icons/file/text.png";
	public static final String FILE_CODE = "icons/file/text-code.png";
	public static final String FILE_ZIP = "icons/file/zip.png";
	public static final String FILE_JAR = "icons/file/jar.png";
	public static final String FILE_CLASS = "icons/file/class.png";
	public static final String FILE_IMAGE = "icons/file/image.png";
	public static final String FILE_AUDIO = "icons/file/audio.png";
	public static final String FILE_PROGRAM = "icons/file/program.png";
	public static final String FILE_LIBRARY = "icons/file/library.png";
	// Action
	public static final String ACTION_COPY = "icons/copy.png";
	public static final String ACTION_DELETE = "icons/delete.png";
	public static final String ACTION_EDIT = "icons/edit.png";
	public static final String ACTION_MOVE = "icons/move.png";
	public static final String ACTION_SEARCH = "icons/search.png";
	// Tools / Tabs
	public static final String T_STRUCTURE = "icons/structure.png";
	public static final String T_TREE = "icons/tree.png";
	// Misc
	public static final String COMPUTER = "icons/computer.png";
	public static final String VM = "icons/vm.png";
	public static final String LOGO = "icons/logo.png";
	public static final String ANDROID = "icons/android.png";
	public static final String OPEN = "icons/open.png";
	public static final String WARNING = "icons/warning.png";
	public static final String ERROR = "icons/error.png";
	public static final String COMPILE = "icons/compile.png";
	public static final String DECOMPILE = "icons/decompile.png";
	public static final String CODE = "icons/code.png";
	public static final String SYNTHETIC = "icons/synthetic.png";
	public static final String EYE = "icons/eye.png";
	public static final String EYE_DISABLED = "icons/eye-disabled.png";
	public static final String CASE_SENSITIVITY = "icons/case-sensitive.png";
	public static final String REFERENCE = "icons/ref.png";
	public static final String QUOTE = "icons/quote.png";
	public static final String CONFIG = "icons/settings.png";
	public static final String WORKSPACE = "icons/workspace.png";
	public static final String INFO = "icons/info.png";
	public static final String HELP = "icons/help.png";
	public static final String PLUS = "icons/plus.png";
	public static final String OPEN_FILE = "icons/open-file.png";
	public static final String RECENT = "icons/recent.png";
	public static final String SAVE = "icons/save.png";
	public static final String EXPORT = "icons/export.png";
	public static final String CLOSE = "icons/close.png";
	public static final String SWAP = "icons/swap.png";
	public static final String NUMBERS = "icons/numbers.png";
	public static final String KEYBOARD = "icons/keyboard.png";
	public static final String DOCUMENTATION = "icons/documentation.png";
	public static final String GITHUB = "icons/github.png";
	public static final String DISCORD = "icons/discord.png";
	public static final String SMART = "icons/brain.png";
	public static final String CHILDREN = "icons/children.png";
	public static final String PARENTS = "icons/parents.png";
	public static final String WORD = "icons/word.png";
	public static final String REGEX = "icons/regex.png";
	public static final String PLAY = "icons/play.png";
	public static final String FORWARD = "icons/forward.png";
	public static final String BACKWARD = "icons/backward.png";

	private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Image> SCALED_IMAGE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Returns {@link ImageView} that uses cached image for rendering.
	 *
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Graphic of image.
	 */
	public static ImageView getImageView(String path) {
		return new ImageView(getImage(path));
	}

	/**
	 * Returns {@link IconView} that uses cached image for rendering.
	 *
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Graphic of image.
	 */
	public static IconView getIconView(String path) {
		return getIconView(path, IconView.DEFAULT_ICON_SIZE);
	}

	/**
	 * Returns {@link IconView} that uses cached image for rendering.
	 *
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 * @param size
	 * 		Image width/height.
	 *
	 * @return Graphic of image.
	 */
	public static IconView getIconView(String path, int size) {
		return new IconView(getImage(path), size);
	}

	/**
	 * Returns {@link IconView} that uses cached image for rendering.
	 * This also scales the image, giving it an anti-aliased look.
	 *
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Graphic of image.
	 */
	public static IconView getScaledIconView(String path) {
		return getScaledIconView(path, IconView.DEFAULT_ICON_SIZE);
	}

	/**
	 * Returns {@link IconView} that uses cached image for rendering.
	 * This also scales the image, giving it an anti-aliased look.
	 *
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 * @param size
	 * 		Image width/height.
	 *
	 * @return Graphic of image.
	 */
	public static IconView getScaledIconView(String path, int size) {
		return new IconView(getScaledImage(path, size), size);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Cached image.
	 */
	public static Image getImage(String path) {
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
		return image;
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 * @param size
	 * 		Desired image size.
	 *
	 * @return Cached image.
	 */
	public static Image getScaledImage(String path, int size) {
		String key = path + "-x" + size;
		Image image = SCALED_IMAGE_CACHE.get(key);
		if (image == null) {
			InputStream stream = ResourceUtil.resource(path);
			image = new Image(stream, size, size, true, true);
			Image cached = SCALED_IMAGE_CACHE.putIfAbsent(key, image);
			if (cached != null) {
				IOUtil.closeQuietly(stream);
				image = cached;
			}
		}
		return image;
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
		return getPathIcon(name);
	}

	/**
	 * @param name
	 * 		Path/name of file to represent.
	 *
	 * @return Node to represent the file.
	 */
	public static Node getPathIcon(String name) {
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			String ext = name.substring(dotIndex + 1).toLowerCase();
			switch (ext) {
				default:
				case "dex":
				case "apk":
					return getIconView(ANDROID);
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
	 * @param file
	 * 		File information.
	 *
	 * @return Node to represent the file type.
	 */
	public static Node getFileIcon(FileInfo file) {
		String name = file.getName();
		Node graphic;
		int[] match = null;
		byte[] data = file.getValue();
		if (ByteHeaderUtil.matchAny(data, ByteHeaderUtil.ARCHIVE_HEADERS)) {
			String lower = name.toLowerCase();
			if (lower.endsWith(".jar")) {
				graphic = getIconView(Icons.FILE_JAR);
			} else {
				graphic = getIconView(Icons.FILE_ZIP);
			}
		} else if (ByteHeaderUtil.matchAny(data, ByteHeaderUtil.IMAGE_HEADERS)) {
			graphic = getIconView(Icons.FILE_IMAGE);
		} else if ((match = ByteHeaderUtil.getMatch(data, ByteHeaderUtil.PROGRAM_HEADERS)) != null) {
			if (match == ByteHeaderUtil.DEX) {
				graphic = getIconView(Icons.ANDROID);
			} else if (match == ByteHeaderUtil.CLASS) {
				graphic = getIconView(Icons.FILE_CLASS);
			} else if (match == ByteHeaderUtil.DYLIB_32 || match == ByteHeaderUtil.DYLIB_64) {
				graphic = getIconView(Icons.FILE_LIBRARY);
			} else {
				String lower = name.toLowerCase();
				if (lower.endsWith(".dll")) {
					graphic = getIconView(Icons.FILE_LIBRARY);
				} else {
					graphic = getIconView(Icons.FILE_PROGRAM);
				}
			}
		} else if (ByteHeaderUtil.matchAny(data, ByteHeaderUtil.AUDIO_HEADERS)) {
			graphic = getIconView(Icons.FILE_AUDIO);
		} else {
			if (StringUtil.isText(data)) {
				String ext = file.getExtension().toLowerCase();
				if (Languages.get(ext) != Languages.NONE) {
					graphic = getIconView(Icons.FILE_CODE);
				} else {
					graphic = getIconView(Icons.FILE_TEXT);
				}
			} else {
				graphic = getIconView(Icons.FILE_BINARY);
			}
		}
		return graphic;
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
