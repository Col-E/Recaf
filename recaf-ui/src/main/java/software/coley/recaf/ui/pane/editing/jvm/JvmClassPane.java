package software.coley.recaf.ui.pane.editing.jvm;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.Node;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.ui.config.ClassEditingConfig;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.ui.pane.editing.SideTabsInjector;
import software.coley.recaf.ui.pane.editing.binary.hex.HexAdapter;
import software.coley.recaf.ui.pane.editing.binary.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.jvm.lowlevel.JvmLowLevelPane;
import software.coley.recaf.util.SceneUtils;

/**
 * Displays {@link JvmClassInfo} in a configurable manner.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmClassPane extends ClassPane {
	private final Instance<JvmDecompilerPane> decompilerProvider;
	private final Instance<JvmLowLevelPane> lowLevelProvider;
	private final HexConfig hexConfig;
	private JvmClassEditorType editorType;

	@Inject
	public JvmClassPane(@Nonnull ClassEditingConfig config,
	                    @Nonnull HexConfig hexConfig,
	                    @Nonnull SideTabsInjector sideTabsInjector,
	                    @Nonnull Instance<JvmDecompilerPane> decompilerProvider,
	                    @Nonnull Instance<JvmLowLevelPane> lowLevelProvider) {
		this.hexConfig = hexConfig;
		this.decompilerProvider = decompilerProvider;
		this.lowLevelProvider = lowLevelProvider;

		sideTabsInjector.injectLater(this);

		editorType = config.getDefaultJvmEditor().getValue();
	}

	/**
	 * @return Current editor display type.
	 */
	@Nonnull
	public JvmClassEditorType getEditorType() {
		return editorType;
	}

	/**
	 * @param editorType
	 * 		New editor display type.
	 */
	public void setEditorType(@Nonnull JvmClassEditorType editorType) {
		if (this.editorType != editorType) {
			this.editorType = editorType;
			refreshDisplay();
		}
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (hasDisplay())
			return;

		// Update content in pane.
		// We do not need to pass the path along to it here, since the calling context should do that.
		JvmClassEditorType type = getEditorType();
		switch (type) {
			case DECOMPILE -> setDisplay(decompilerProvider.get());
			case LOW_LEVEL -> setDisplay(lowLevelProvider.get());
			case HEX -> setDisplay(new HexAdapter(hexConfig));
			default -> throw new IllegalStateException("Unknown editor type: " + type.name());
		}
	}

	@Override
	public void requestFocus() {
		Node display = getDisplay();
		if (display != null)
			display.requestFocus();
	}
}
