package software.coley.recaf.util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import software.coley.recaf.ui.control.IconView;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graphic utilities for SVG.
 *
 * @author Matt Coley
 */
public class SVG {
	private static final Map<RenderingHints.Key, Object> STROKE_HINTS = Map.of(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
	private static final SVGDocument EMPTY_DOCUMENT;
	public static final String EXCEPTION_BREAKPOINT = "icons/exception-breakpoint.svg";
	public static final String REF_READ = "icons/read-access.svg";
	public static final String REF_WRITE = "icons/write-access.svg";
	public static final String TYPE_CONVERSION = "icons/type-conversion.svg";
	private static final Map<String, SVGDocument> DOCUMENT_CACHE = new ConcurrentHashMap<>();

	static {
		EMPTY_DOCUMENT = read(new ByteArrayInputStream(("<svg width=\"0\" height=\"0\" xmlns=\"http://www.w3.org/2000/svg\"></svg>").getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link SVG}.
	 *
	 * @return SVG node of file contents sized to an icon.
	 */
	@Nonnull
	public static Node ofIconFile(@Nonnull String path) {
		Node graphic = ofFile(path, IconView.DEFAULT_ICON_SIZE, STROKE_HINTS);
		graphic.prefWidth(IconView.DEFAULT_ICON_SIZE);
		return graphic;
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link SVG}.
	 * @param size
	 * 		Desired image width/height.
	 *
	 * @return SVG node of file contents.
	 */
	@Nonnull
	public static Node ofFile(@Nonnull String path, int size) {
		return ofFile(path, size, size);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link SVG}.
	 * @param size
	 * 		Desired image width/height.
	 * @param renderingHints
	 * 		KV pairs for {@link RenderingHints} entries.
	 *
	 * @return SVG node of file contents.
	 */
	@Nonnull
	public static Node ofFile(@Nonnull String path, int size,
	                          @Nullable Map<RenderingHints.Key, Object> renderingHints) {
		return ofFile(path, size, size, renderingHints);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link SVG}.
	 * @param width
	 * 		Desired image width.
	 * @param height
	 * 		Desired image height.
	 *
	 * @return SVG node of file contents.
	 */
	@Nonnull
	public static Node ofFile(@Nonnull String path, int width, int height) {
		return ofFile(path, width, height, null);
	}

	/**
	 * @param path
	 * 		Path to local image. See constants defined in {@link SVG}.
	 * @param width
	 * 		Desired image width.
	 * @param height
	 * 		Desired image height.
	 * @param renderingHints
	 * 		KV pairs for {@link RenderingHints} entries.
	 *
	 * @return SVG node of file contents.
	 */
	@Nonnull
	public static Node ofFile(@Nonnull String path, int width, int height,
	                          @Nullable Map<RenderingHints.Key, Object> renderingHints) {
		SVGDocument document = DOCUMENT_CACHE.computeIfAbsent(path, SVG::read);

		// Render to image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		if (renderingHints != null) // Apply user-provided rendering hints
			renderingHints.forEach(g::setRenderingHint);
		document.render(null, g);
		g.dispose();

		// Convert to FX compatible image
		return new ImageView(Icons.convertToFxImage(image));
	}

	@Nonnull
	private static SVGDocument read(@Nonnull String path) {
		try (InputStream is = ResourceUtil.resource(path)) {
			return read(is);
		} catch (Exception ex) {
			return EMPTY_DOCUMENT;
		}
	}

	@Nonnull
	private static SVGDocument read(@Nullable InputStream is) {
		if (is == null)
			throw new IllegalStateException();

		// The optimal code for this is:
		//   return new SVGLoader().load(is);
		// But JDK 24+ complains that @NotNull from THEIR CODE is missing in OUR classpath
		// even though it is not directly referenced in OUR CODE. What I really don't get
		// is that this is only a problem HERE with JSVG, despite this annotation being used
		// in other libraries without issue.
		// This is slower, but we also cache the results, so I don't care.
		// See: https://github.com/Col-E/Recaf/issues/933
		try {
			SVGLoader loader = new SVGLoader();
			Method m = SVGLoader.class.getDeclaredMethod("load", InputStream.class);
			return (SVGDocument) m.invoke(loader, is);
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}
}
