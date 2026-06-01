package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.animation.LabelByteAnimationTransition;
import software.coley.recaf.util.Lang;

/**
 * Overlay shown while file-backed content is being computed in the background.
 *
 * @author Matt Coley
 */
public class ByteLoadingOverlay extends VBox {
	private final Label detail = new Label();
	private final LabelByteAnimationTransition transition = new LabelByteAnimationTransition(detail);

	/**
	 * @param titleKey
	 * 		Translation key for the loading title.
	 */
	public ByteLoadingOverlay(@Nonnull String titleKey) {
		Label title = new BoundLabel(Lang.getBinding(titleKey));
		title.getStyleClass().add(Styles.TITLE_3);
		detail.getStyleClass().add(Styles.TEXT_SUBTLE);
		detail.setFont(new Font("JetBrains Mono", 12));

		getChildren().addAll(new Spacer(Orientation.VERTICAL), title, detail, new Spacer(Orientation.VERTICAL));
		getStyleClass().add("background");
		setFillWidth(true);
		setAlignment(Pos.CENTER);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		setVisible(false);
		setManaged(false);
	}

	/**
	 * Show the overlay and update its animated byte preview from the given file.
	 *
	 * @param info
	 * 		File whose raw bytes should be previewed.
	 */
	public void show(@Nonnull FileInfo info) {
		setVisible(true);
		setManaged(true);
		transition.update(info);
		transition.play();
	}

	/**
	 * Hide the overlay.
	 */
	public void hide() {
		transition.stop();
		detail.setText("");
		setVisible(false);
		setManaged(false);
	}
}
