package software.coley.recaf.ui.control;

import atlantafx.base.controls.ModalPane;
import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;

/**
 * Modal pane exposed as an {@link EditorComponent}.
 *
 * @author Matt Coley
 */
public class ModalPaneComponent extends ModalPane implements EditorComponent {
	{
		setSkin(createDefaultSkin());
	}

	@Override
	public void install(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().add(0, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().remove(this);
	}
}
