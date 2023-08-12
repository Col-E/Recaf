package software.coley.recaf.ui.pane.editing.image;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.animation.FadeTransition;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
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

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * Displays a {@link ImageFileInfo} in a pannable view, with some extra utility.
 *
 * @author Matt Coley
 */
@Dependent
public class ImagePane extends StackPane implements FileNavigable, UpdatableNavigable {
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
				Image image = new Image(new ByteArrayInputStream(info.getRawContent()));
				imageView.setImage(image);
			}
		}
	}

	/**
	 * Control to manipulate the image effect.
	 */
	private class ColorAdjustmentControls extends Group {
		private ColorAdjustmentControls() {
			setupShowOnHover();

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

		private void setupShowOnHover() {
			double hiddenOpacity = 0.1;
			DoubleProperty opacity = opacityProperty();
			opacity.set(hiddenOpacity);

			FadeTransition show = new FadeTransition(Duration.millis(250), this);
			show.setToValue(1.0);

			FadeTransition hide = new FadeTransition(Duration.millis(250), this);
			hide.setToValue(hiddenOpacity);

			setOnMouseEntered(e -> {
				show.setFromValue(opacity.doubleValue());
				show.play();
			});
			setOnMouseExited(e -> {
				hide.setFromValue(opacity.doubleValue());
				hide.play();
			});
		}
	}
}
