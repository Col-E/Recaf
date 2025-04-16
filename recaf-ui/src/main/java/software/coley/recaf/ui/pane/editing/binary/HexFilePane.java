package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.input.KeyEvent;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.hex.HexConfig;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.bundle.Bundle;

/**
 * Displays {@link FileInfo} in a hex editor.
 *
 * @author Matt Coley
 */
@Dependent
public class HexFilePane extends FilePane {
	private final HexConfig config;

	@Inject
	public HexFilePane(@Nonnull KeybindingConfig keys, @Nonnull HexConfig config) {
		this.config = config;

		// Setup keybindings - Using event filter here because the hex-editor otherwise consumes key events.
		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!(getCenter() instanceof HexAdapter adapter))
				return;
			if (keys.getSave().match(e))
				ThreadUtil.run(adapter::save);
			else if (keys.getUndo().match(e)) {
				Bundle<?> bundle = path.getValueOfType(Bundle.class);
				if (bundle != null)
					bundle.decrementHistory(path.getValue().getName());
			}
		});
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (getCenter() != null)
			return;

		// Update content in pane.
		setDisplay(new HexAdapter(config));
	}
}
