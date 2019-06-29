package me.coley.recaf.util;

import java.net.URL;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import me.coley.logging.Level;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.AccessFlag;

/**
 * Icons.
 * 
 * @author Matt
 */
public class Icons {
	// toolbar
	public static final ImageView T_SEARCH, T_LOAD, T_EXPORT, T_SAVE, T_CONFIG, T_ATTACH;
	private static final int T_SIZE = 32;
	// logging levels
	public static final Image L_ERROR, L_WARN, L_INFO, L_FINE, L_TRACE;
	// access images
	public static final Image CL_PACKAGE, CL_ENUM, CL_ANNOTATION, CL_INTERFACE, CL_CLASS;
	public static final Image MOD_ABSTRACT, MOD_FINAL, MOD_NATIVE, MOD_STATIC, MOD_SYNTHETIC, MOD_TRANSIENT, MOD_VOLATILE;
	public static final Image F_DEFAULT, M_DEFAULT, F_PRIVATE, M_PRIVATE, F_PROTECTED, M_PROTECTED, F_PUBLIC, M_PUBLIC;
	// misc
	public static final Image LOGO, CONFIG, ATTACH, FIND, ABOUT_BAR;

	static {
		// misc
		LOGO = load("logo");
		CONFIG = load("config");
		ATTACH = load("attach");
		FIND = load("find");
		ABOUT_BAR = load("about-bar");
		// toolbar
		T_SEARCH = loadWithSize(T_SIZE, load("search"));
		T_LOAD = loadWithSize(T_SIZE, load("load"));
		T_EXPORT = loadWithSize(T_SIZE, load("export"));
		T_SAVE = loadWithSize(T_SIZE, load("save"));
		T_CONFIG = loadWithSize(T_SIZE, CONFIG);
		T_ATTACH = loadWithSize(T_SIZE, ATTACH);
		// Logging levels
		L_ERROR = loadLvl(Level.ERRR);
		L_WARN = loadLvl(Level.WARN);
		L_INFO = loadLvl(Level.INFO);
		L_FINE = loadLvl(Level.FINE);
		L_TRACE = loadLvl(Level.TRACE);
		// access images
		CL_PACKAGE = load("class/package");
		CL_ENUM = load("class/enum");
		CL_ANNOTATION = load("class/annotation");
		CL_INTERFACE = load("class/interface");
		CL_CLASS = load("class/class");
		//
		MOD_ABSTRACT = load("modifier/abstract");
		MOD_FINAL = load("modifier/final");
		MOD_NATIVE = load("modifier/native");
		MOD_STATIC = load("modifier/static");
		MOD_SYNTHETIC = load("modifier/synthetic");
		MOD_TRANSIENT = load("modifier/transient");
		MOD_VOLATILE = load("modifier/volatile");
		//
		F_DEFAULT = load("modifier/field_default");
		F_PRIVATE = load("modifier/field_private");
		F_PROTECTED = load("modifier/field_protected");
		F_PUBLIC = load("modifier/field_public");
		M_DEFAULT = load("modifier/method_default");
		M_PRIVATE = load("modifier/method_private");
		M_PROTECTED = load("modifier/method_protected");
		M_PUBLIC = load("modifier/method_public");
	}

	/**
	 * Get image representation of a logging level.
	 * 
	 * @param level
	 * @return
	 */
	public static Image getLog(Level level) {
		switch (level) {
		case ERRR:
			return L_ERROR;
		case WARN:
			return L_WARN;
		case INFO:
			return L_INFO;
		case FINE:
			return L_FINE;
		case TRACE:
			return L_TRACE;
		default:
			return null;
		}
	}

	/**
	 * Load an image into an ImageView with a predefined size.
	 * 
	 * @param size
	 *            Image size.
	 * @param image
	 *            Image to scale.
	 * @return Scaled ImageView.
	 */
	private static ImageView loadWithSize(int size, Image image) {
		ImageView iv = new ImageView(image);
		iv.setPreserveRatio(false);
		iv.setFitHeight(size);
		iv.setFitWidth(size);
		return iv;
	}

	/**
	 * Get image representation of a class by its access flags.
	 * 
	 * @param access
	 *            Flags <i>(Modifiers)</i>
	 * @return Image for flags.
	 */
	public static Group getClass(int access) {
		Image base = null;
		if (AccessFlag.isInterface(access)) {
			base = CL_INTERFACE;
		} else if (AccessFlag.isEnum(access)) {
			base = CL_ENUM;
		} else if (AccessFlag.isAnnotation(access)) {
			base = CL_ANNOTATION;
		} else {
			base = CL_CLASS;
		}
		Group g = new Group(new ImageView(base));
		if (!AccessFlag.isInterface(access) && AccessFlag.isAbstract(access)) {
			g.getChildren().add(new ImageView(MOD_ABSTRACT));
		}
		if (!AccessFlag.isEnum(access) && AccessFlag.isFinal(access)) {
			g.getChildren().add(new ImageView(MOD_FINAL));
		}
		if (AccessFlag.isNative(access)) {
			g.getChildren().add(new ImageView(MOD_NATIVE));
		}
		if (AccessFlag.isStatic(access)) {
			g.getChildren().add(new ImageView(MOD_STATIC));
		}
		if (AccessFlag.isSynthetic(access)) {
			g.getChildren().add(new ImageView(MOD_SYNTHETIC));
		}
		return g;
	}

	/**
	 * Get image representation of a member by its access flags.
	 * 
	 * @param access
	 *            Flags <i>(Modifiers)</i>
	 * @return Image for flags.
	 */
	public static Group getMember(int access, boolean method) {
		Group g = null;
		if (AccessFlag.isPublic(access)) {
			g = new Group(new ImageView(method ? M_PUBLIC : F_PUBLIC));
		} else if (AccessFlag.isProtected(access)) {
			g = new Group(new ImageView(method ? M_PROTECTED : F_PROTECTED));
		} else if (AccessFlag.isPrivate(access)) {
			g = new Group(new ImageView(method ? M_PRIVATE : F_PRIVATE));
		} else {
			g = new Group(new ImageView(method ? M_DEFAULT : F_DEFAULT));
		}

		if (!AccessFlag.isInterface(access) && AccessFlag.isAbstract(access)) {
			g.getChildren().add(new ImageView(MOD_ABSTRACT));
		}
		if (!AccessFlag.isEnum(access) && AccessFlag.isFinal(access)) {
			g.getChildren().add(new ImageView(MOD_FINAL));
		}
		if (AccessFlag.isNative(access)) {
			g.getChildren().add(new ImageView(MOD_NATIVE));
		}
		if (AccessFlag.isStatic(access)) {
			g.getChildren().add(new ImageView(MOD_STATIC));
		}
		if (AccessFlag.isSynthetic(access)) {
			g.getChildren().add(new ImageView(MOD_SYNTHETIC));
		}
		if (AccessFlag.isBridge(access)) {
			g.getChildren().add(new ImageView(method ? MOD_SYNTHETIC : MOD_VOLATILE));
		}
		return g;
	}

	/**
	 * Get single image for access. Intended usage for individual modifiers.
	 * 
	 * @param flag modifier flag.
	 * @return ImageView of flag.
	 */
	public static Node getAccess(AccessFlag flag, boolean method) {
		// @formatter:off
		Image image = F_DEFAULT;
		switch (flag) {
			case ACC_ENUM:          image = CL_ENUM;                            break;
			case ACC_INTERFACE:     image = CL_INTERFACE;                       break;
			case ACC_ANNOTATION:    image = CL_ANNOTATION;                      break;
			case ACC_PUBLIC:        image = method ? M_PUBLIC    : F_PUBLIC;    break;
			case ACC_PROTECTED:     image = method ? M_PROTECTED : F_PROTECTED; break;
			case ACC_PRIVATE:       image = method ? M_PRIVATE   : F_PRIVATE;   break;
			case ACC_ABSTRACT:      image = MOD_ABSTRACT;                       break;
			case ACC_FINAL:         image = MOD_FINAL;                          break;
			case ACC_NATIVE:        image = MOD_NATIVE;                         break;
			case ACC_STATIC:        image = MOD_STATIC;                         break;
			case ACC_SYNTHETIC:     image = MOD_SYNTHETIC;                      break;
			case ACC_TRANSIENT:     image = MOD_TRANSIENT;                      break;
			case ACC_BRIDGE:        if(!method) break; // else continue
			case ACC_VOLATILE:      image = MOD_VOLATILE;                       break;
			default:                image = F_DEFAULT;                          break;
		}
		// @formatter:on
		return new ImageView(image);
	}

	/**
	 * Load icon for logging level.
	 * 
	 * @param level
	 *            Logging level.
	 * @return Image to represent logging level.
	 */
	private static Image loadLvl(Level level) {
		String lvlStr = level.name().toLowerCase();
		try {
			String file = "resources/icons/log/" + lvlStr + ".png";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			return new Image(url.openStream());
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Load icon from name.
	 * 
	 * @param name
	 *            File name.
	 * @return Image by name.
	 */
	private static Image load(String name) {
		try {
			String file = "resources/icons/" + name + ".png";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			return new Image(url.openStream());
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Scale an image. Cannot be used for window icons.
	 * 
	 * @param source
	 *            Original image.
	 * @param size
	 *            New dimensions <i>(both width / height)</i>
	 * @return Scaled image.
	 */
	public static Image scale(Image source, int size) {
		ImageView imageView = new ImageView(source);
		imageView.setPreserveRatio(true);
		imageView.setFitWidth(size);
		imageView.setFitHeight(size);
		return imageView.snapshot(null, null);
	}
}
