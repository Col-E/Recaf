package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.hex.HexEditor;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;

/**
 * Displays {@link FileInfo} in a hex editor.
 *
 * @author Matt Coley
 */
@Dependent
public class HexFilePane extends FilePane {
	private final HexConfig config;
	@Inject
	public HexFilePane(@Nonnull HexConfig config) {
		this.config = config;
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
