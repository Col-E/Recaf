package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.DesktopUtil;

import java.io.IOException;
import java.net.URI;

/**
 * Utility hyperlink to quickly apply a {@link StringBinding}, commonly for UI translations.
 *
 * @author Matt Coley
 */
public class BoundHyperlink extends Hyperlink implements Tooltipable {
	private static final Logger logger = Logging.get(BoundHyperlink.class);

	/**
	 * @param binding
	 * 		Text binding.
	 * @param graphic
	 * 		Label display icon.
	 * @param url
	 * 		URL to navigate to on-click.
	 */
	public BoundHyperlink(@Nonnull ObservableValue<String> binding, @Nullable Node graphic, @Nonnull String url) {
		this(binding, graphic, () -> {
			try {
				DesktopUtil.showDocument(URI.create(url));
			} catch (IOException ex) {
				logger.error("Failed to browse to {}", url, ex);
			}
		});
	}

	/**
	 * @param binding
	 * 		Text binding.
	 * @param graphic
	 * 		Label display icon.
	 * @param onClick
	 * 		On-click action to run.
	 */
	public BoundHyperlink(@Nonnull ObservableValue<String> binding, @Nullable Node graphic, @Nonnull Runnable onClick) {
		textProperty().bind(binding);
		setGraphic(graphic);
		setOnAction(e -> onClick.run());
	}

	/**
	 * Unbinds the old text property and rebinds to the given value.
	 *
	 * @param binding
	 * 		New value to bind to.
	 */
	public void rebind(@Nonnull ObservableValue<String> binding) {
		var property = textProperty();
		property.unbind();
		property.bind(binding);
	}
}
