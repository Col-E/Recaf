package software.coley.recaf.util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Graphic utilities for SVG.
 *
 * @author Matt Coley
 */
public class SVG {
	private static final SVGDocument EMPTY_DOCUMENT;
	public static final String REF_READ = "icons/read-access.svg";
	public static final String REF_WRITE = "icons/write-access.svg";
	private static final Map<String, SVGDocument> DOCUMENT_CACHE = new ConcurrentHashMap<>();

	static {
		EMPTY_DOCUMENT = new SVGLoader()
				.load(new ByteArrayInputStream(("<svg width=\"0\" height=\"0\" xmlns=\"http://www.w3.org/2000/svg\">" +
						"</svg>").getBytes(StandardCharsets.UTF_8)));
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
		SVGDocument document = DOCUMENT_CACHE.computeIfAbsent(path, p -> {
			try (InputStream is = ResourceUtil.resource(path)) {
				SVGLoader loader = new SVGLoader();
				return loader.load(is);
			} catch (Exception ex) {
				return EMPTY_DOCUMENT;
			}
		});

		// Render to image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		if (renderingHints != null) // Apply user-provided rendering hints
			renderingHints.forEach(g::setRenderingHint);
		document.render(null, g);
		g.dispose();

		// Convert to FX compatible image
		return new ImageView(convertToFxImage(image));
	}

	/**
	 * @param image
	 * 		Buffered image assumed to be in {@link BufferedImage#TYPE_INT_ARGB}.
	 *
	 * @return JavaFX image.
	 *
	 * @author <a href="https://stackoverflow.com/a/75703543">Kevin Bähre</a>
	 */
	@Nonnull
	private static Image convertToFxImage(@Nonnull BufferedImage image) {
		int[] type_int_agrb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		IntBuffer buffer = IntBuffer.wrap(type_int_agrb);
		PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
		PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(image.getWidth(), image.getHeight(), buffer, pixelFormat);
		return new WritableImage(pixelBuffer);
	}
}
