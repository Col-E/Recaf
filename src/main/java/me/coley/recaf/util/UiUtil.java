package me.coley.recaf.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.workspace.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static me.coley.recaf.util.ClasspathUtil.resource;

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
		} else if (ext.endsWith("/")) {
			path = "icons/folder-source.png";
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
		if(resource instanceof DirectoryResource)
			return "icons/folder-source.png";
		else if(resource instanceof ArchiveResource)
			return "icons/jar.png";
		else if(resource instanceof ClassResource)
			return "icons/binary.png";
		else if(resource instanceof UrlResource)
			return "icons/link.png";
		else if(resource instanceof MavenResource)
			return "icons/data.png";
		// TODO: Unique debug/agent icon?
		else if(resource instanceof InstrumentationResource)
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
		createAccessToolTips(g, AccessFlag.Type.CLASS, access);
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
		createAccessToolTips(g, AccessFlag.Type.FIELD, access);
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
		createAccessToolTips(g, AccessFlag.Type.METHOD, access);
		return g;
	}

	private static void createAccessToolTips(Node node, AccessFlag.Type type, int access) {
		Set<String> accessFlags = AccessFlag.getApplicableFlags(type, access).stream()
				.map(AccessFlag::getName).collect(Collectors.toSet());
		Tooltip tooltip = new Tooltip(String.join(", ", accessFlags));
		// Show tooltip instantly, Tooltip.install(node, tooltip) has a significant delay
		node.setOnMouseEntered(event -> {
			if (!tooltip.getText().isEmpty()) {
				tooltip.show(node, event.getScreenX() + 10, event.getScreenY() + 1);
			}
		});
		node.setOnMouseExited(event -> tooltip.hide());
	}

	/**
	 * Convert raw bytes to an image.
	 *
	 * @param content
	 * 		Some raw bytes of a file.
	 *
	 * @return Image instance, if bytes represent a valid image, otherwise {@code null}.
	 */
	public static BufferedImage toImage(byte[] content) {
		try {
			return ImageIO.read(new ByteArrayInputStream(content));
		} catch(Exception ex) {
			return null;
		}
	}

	/**
	 * Convert a AWT image to a JavaFX image.
	 *
	 * @param img
	 * 		The image to convert.
	 *
	 * @return JavaFX image.
	 */
	public static WritableImage toFXImage(BufferedImage img) {
		// This is a stripped down version of "SwingFXUtils.toFXImage(img, fxImg)"
		int w = img.getWidth();
		int h = img.getHeight();
		// Ensure image type is ARGB.
		switch(img.getType()) {
			case BufferedImage.TYPE_INT_ARGB:
			case BufferedImage.TYPE_INT_ARGB_PRE:
				break;
			default:
				// Convert to ARGB
				BufferedImage converted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
				Graphics2D g2d = converted.createGraphics();
				g2d.drawImage(img, 0, 0, null);
				g2d.dispose();
				img = converted;
				break;
		}
		// Even if the image type is ARGB_PRE, we use "getIntArgbInstance()"
		// Using "getIntArgbPreInstance()" removes transparency.
		WritableImage fxImg = new WritableImage(w, h);
		PixelWriter pw = fxImg.getPixelWriter();
		int[] data = img.getRGB(0, 0, w, h, null, 0, w);
		pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), data, 0, w);
		return fxImg;
	}

	/**
	 * Configures the doc icon to use the Recaf logo on MacOS platforms.
	 */
	public static void setupMacDockIcon() {
		try {
			/* Why reflection?
			com.apple.eawt.Application is platform-specific class, that stored in apple-distributed rt.jar
			and if we use it directly, build must throw an error: "package com.apple.eawt does not exist"
			*/
			BufferedImage image = ImageIO.read(resource("icons/logo.png"));
			Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
			Object application = applicationClass.getMethod("getApplication").invoke(null);
			Method dockIconSetter = applicationClass.getMethod("setDockIconImage", java.awt.Image.class);
			dockIconSetter.invoke(application, image);
		} catch (Exception ignored) {
			// Just ignore if we can't load dock image, it's not critical.
		}
	}

	/**
	 * Play an animation that indicates success <i>(Thin green border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateSuccess(Node node, long millis) {
		animate(node, millis, 90, 255, 60);
	}

	/**
	 * Play an animation that indicates failure <i>(Thin red border)</i>.
	 *
	 * @param node
	 * 		Node to animate.
	 * @param millis
	 * 		Duration in milliseconds of fade.
	 */
	public static void animateFailure(Node node, long millis) {
		animate(node, millis, 255, 60, 40);
	}

	private static void animate(Node node, long millis, int r, int g, int b) {
		DoubleProperty dblProp = new SimpleDoubleProperty(1);
		dblProp.addListener((ob, o, n) -> {
			InnerShadow innerShadow = new InnerShadow();
			innerShadow.setBlurType(BlurType.ONE_PASS_BOX);
			innerShadow.setChoke(1);
			innerShadow.setRadius(5);
			innerShadow.setColor(Color.rgb(r, g, b, n.doubleValue()));
			node.setEffect(innerShadow);
		});
		Timeline timeline = new Timeline();
		KeyValue kv = new KeyValue(dblProp, 0);
		KeyFrame kf = new KeyFrame(Duration.millis(millis), kv);
		timeline.getKeyFrames().add(kf);
		timeline.play();
	}

	/**
	 *  Attempts to launch a browser to display a {@link URI}.
	 *
	 * @param uri
	 * 		URI to display.
	 *
	 * @throws IOException
	 * 		If the browser is not found, or it fails
	 * 		to be launched.
	 */
	public static void showDocument(URI uri) throws IOException {
		switch (OSUtil.getOSType()) {
			case MAC:
				Runtime.getRuntime().exec("open " + uri);
				break;
			case WINDOWS:
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + uri);
				break;
			case LINUX:
				Runtime rt = Runtime.getRuntime();
				String[] browsers = new String[]{"xdg-open", "google-chrome", "firefox", "opera",
					"konqueror", "mozilla"};

				for (String browser : browsers) {
					try (InputStream in = rt.exec(new String[]{"which", browser}).getInputStream()) {
						if (in.read() != -1) {
							rt.exec(new String[]{browser, uri.toString()});
							return;
						}
					}
				}
				throw new IOException("No browser found");
			default:
				throw new IllegalStateException("Unsupported OS");
		}
	}
}
