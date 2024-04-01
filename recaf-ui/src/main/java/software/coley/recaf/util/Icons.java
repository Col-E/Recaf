package software.coley.recaf.util;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.ui.control.IconView;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icon and graphic utilities.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unused")
public class Icons {
	private static final Image EMPTY_IMAGE = new WritableImage(1, 1);
	// Class definitions
	public static final String CLASS = "icons/class/class.png";
	public static final String CLASS_ANONYMOUS = "icons/class/class_anonymous.png";
	public static final String CLASS_ABSTRACT = "icons/class/class_abstract.png";
	public static final String CLASS_EXCEPTION = "icons/class/class_exception.png";
	public static final String CLASS_ABSTRACT_EXCEPTION = "icons/class/class_abstract_exception.png";
	public static final String ANNOTATION = "icons/class/annotation.png";
	public static final String INTERFACE = "icons/class/interface.png";
	public static final String ENUM = "icons/class/enum.png";
	public static final String NULL = "icons/class/null.png";
	public static final String PRIMITIVE = "icons/class/prim.png";
	public static final String UNINITIALIZED = "icons/class/uninitialized.png";
	public static final String INTERNAL = "icons/class/internal.png";
	public static final String ARRAY = "icons/class/array.png";
	// Member definitions
	public static final String FIELD = "icons/member/field.png";
	public static final String METHOD = "icons/member/method.png";
	public static final String FIELD_N_METHOD = "icons/member/field_n_method.png";
	public static final String CLASS_N_FIELD_N_METHOD = "icons/member/class_n_field_n_method.png";
	public static final String METHOD_ABSTRACT = "icons/member/method_abstract.png";
	// Access modifiers
	public static final String ACCESS_ALL_VISIBILITY = "icons/modifier/all_visibility.png";
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
	public static final String FILE_VIDEO = "icons/file/video.png";
	public static final String FILE_PROGRAM = "icons/file/program.png";
	public static final String FILE_LIBRARY = "icons/file/library.png";
	// Misc
	public static final String LOGO = "icons/logo.png";
	public static final String ANDROID = "icons/android.png";
	public static final String SYNTHETIC = "icons/synthetic.png";
	public static final String DISCORD = "icons/discord.png";
	public static final String REGEX = "icons/regex.png";
	public static final String SORT_ALPHABETICAL = "icons/sort-alphabetical.png";
	public static final String SORT_VISIBILITY = "icons/sort-visibility.png";
	public static final String PHANTOM = "icons/phantom.png";

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
		return IMAGE_CACHE.computeIfAbsent(path, k -> safeCreateImage(path));
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
	 * Get node to represent common visibility/access modifiers. Technically works on classes too.
	 *
	 * @param access
	 * 		Access modifiers.
	 *
	 * @return Node to represent the access modifier.
	 */
	public static Node getVisibilityIcon(int access) {
		if (Modifier.isPrivate(access)) {
			return getIconView(ACCESS_PRIVATE);
		} else if (Modifier.isProtected(access)) {
			return getIconView(ACCESS_PROTECTED);
		} else if (Modifier.isPublic(access)) {
			return getIconView(ACCESS_PUBLIC);
		}
		return getIconView(ACCESS_PACKAGE);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Lazy provider of the image.
	 */
	public static IconProvider createProvider(String path) {
		return () -> getIconView(path);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link Icons}.
	 *
	 * @return Graphic of image, or an empty image if the path could not be resolved.
	 */
	private static Image safeCreateImage(String path) {
		try {
			return new Image(ResourceUtil.resource(path));
		} catch (NullPointerException ex) {
			return EMPTY_IMAGE;
		}
	}
}
