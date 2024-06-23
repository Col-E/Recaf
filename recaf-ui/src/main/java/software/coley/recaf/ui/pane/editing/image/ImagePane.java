package software.coley.recaf.ui.pane.editing.image;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.ImageFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.ImageCanvas;
import software.coley.recaf.ui.control.PannableView;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.ByteHeaderUtil;
import software.coley.recaf.util.Icons;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Displays a {@link ImageFileInfo} in a pannable view, with some extra utility.
 *
 * @author Matt Coley
 */
@Dependent
public class ImagePane extends StackPane implements FileNavigable, UpdatableNavigable {
	private static final Logger logger = Logging.get(ImagePane.class);
	private final ImageCanvas imageView = new ImageCanvas();
	protected FilePathNode path;

	@Inject
	public ImagePane() {
		PannableView imageWrapper = new PannableView(imageView);

		ColorAdjustmentControls colorAdjustmentControls = new ColorAdjustmentControls();
		StackPane.setAlignment(colorAdjustmentControls, Pos.BOTTOM_CENTER);
		StackPane.setMargin(colorAdjustmentControls, new Insets(7));

		getChildren().addAll(imageWrapper, colorAdjustmentControls);
	}

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// no-op
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			FileInfo info = filePath.getValue();
			if (info.isImageFile()) {
				byte[] content = info.getRawContent();

				if ("ico".equals(info.getFileExtension()) && ByteHeaderUtil.match(content, ByteHeaderUtil.ICO)) {
					// JavaFX doesn't directly support ICO files, so we need to adapt it.
					try {
						Image image = Icons.convertIcoToFxImage(content);
						if (image != null)
							imageView.setImage(image);
						else {
							logger.error("Failed to decode ICO image, no bundled images in file");
							setDisable(true);
						}
					} catch (IOException ex) {
						logger.error("Failed to decode ICO image", ex);
						setDisable(true);
					}
				} else {
					Image image = new Image(new ByteArrayInputStream(content));
					imageView.setImage(image);
				}
			}
		}
	}

	/**
	 * Control to manipulate the image effect.
	 */
	private class ColorAdjustmentControls extends Group {
		private ColorAdjustmentControls() {
			Animations.setupShowOnHover(this);

			GridPane box = new GridPane();
			box.setAlignment(Pos.CENTER);
			box.setVgap(5);
			box.setHgap(5);
			box.setPadding(new Insets(5, 5, 5, 10));

			FontIconView brightnessIcon = new FontIconView(CarbonIcons.LIGHT);
			Slider brightnessSlider = new Slider(-1, 1, 0);
			brightnessSlider.valueProperty().addListener((ob, old, cur) -> imageView.setBrightness(cur.doubleValue()));

			box.add(brightnessIcon, 0, 0);
			box.add(brightnessSlider, 1, 0);
			box.getStyleClass().addAll(Styles.BORDER_DEFAULT, Styles.BG_SUBTLE, "round-container");

			getChildren().add(box);
		}
	}
}
