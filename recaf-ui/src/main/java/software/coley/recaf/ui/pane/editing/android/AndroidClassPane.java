package software.coley.recaf.ui.pane.editing.android;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.ui.config.ClassEditingConfig;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.control.IconView;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.ui.pane.editing.tabs.InheritancePane;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;

/**
 * Displays {@link AndroidClassInfo} in a configurable manner.
 *
 * @author Matt Coley
 */
@Dependent
public class AndroidClassPane extends ClassPane {
	private final Instance<AndroidDecompilerPane> decompilerProvider;
	private AndroidClassEditorType editorType;

	@Inject
	public AndroidClassPane(@Nonnull ClassEditingConfig config,
							@Nonnull FieldsAndMethodsPane fieldsAndMethodsPane,
							@Nonnull InheritancePane inheritancePane,
							@Nonnull Instance<AndroidDecompilerPane> decompilerProvider) {
		editorType = config.getDefaultAndroidEditor().getValue();
		this.decompilerProvider = decompilerProvider;
		configureCommonSideTabs(fieldsAndMethodsPane, inheritancePane);
	}

	/**
	 * @return Current editor display type.
	 */
	@Nonnull
	public AndroidClassEditorType getEditorType() {
		return editorType;
	}

	/**
	 * @param editorType
	 * 		New editor display type.
	 */
	public void setEditorType(@Nonnull AndroidClassEditorType editorType) {
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
		AndroidClassEditorType type = getEditorType();
		switch (type) {
			case DECOMPILE -> setDisplay(decompilerProvider.get());
			case SMALI -> {
				// TODO: Create 'Editor' set-up for smali
				Label decompile = new Label("TODO: Smali");
				setDisplay(decompile);
			}
			default -> throw new IllegalStateException("Unknown editor type: " + type.name());
		}
	}
}
