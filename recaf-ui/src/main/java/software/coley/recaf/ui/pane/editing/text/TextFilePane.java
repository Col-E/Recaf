package software.coley.recaf.ui.pane.editing.text;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.binary.HexAdapter;
import software.coley.recaf.ui.pane.editing.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.jvm.TextEditorType;

/**
 * Displays {@link TextFileInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class TextFilePane extends FilePane {
	private final Instance<TextPane> textProvider;
	private final HexConfig hexConfig;
	private TextEditorType editorType = TextEditorType.TEXT;

	@Inject
	public TextFilePane(@Nonnull HexConfig hexConfig, @Nonnull Instance<TextPane> textProvider) {
		this.hexConfig = hexConfig;
		this.textProvider = textProvider;
	}

	/**
	 * @return Current editor display type.
	 */
	@Nonnull
	public TextEditorType getEditorType() {
		return editorType;
	}

	/**
	 * @param editorType
	 * 		New editor display type.
	 */
	public void setEditorType(@Nonnull TextEditorType editorType) {
		if (this.editorType != editorType) {
			this.editorType = editorType;
			refreshDisplay();
		}
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (getCenter() != null)
			return;

		// Update content in pane.
		TextEditorType type = getEditorType();
		switch (type) {
			case TEXT -> setDisplay(textProvider.get());
			case HEX -> setDisplay(new HexAdapter(hexConfig));
			default -> throw new IllegalStateException("Unknown editor type: " + type.name());
		}
	}
}
