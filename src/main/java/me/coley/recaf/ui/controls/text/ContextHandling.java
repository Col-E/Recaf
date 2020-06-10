package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.RenamingTextField;
import me.coley.recaf.ui.controls.text.selection.ClassSelection;
import me.coley.recaf.ui.controls.text.selection.MemberSelection;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.workspace.JavaResource;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.function.Consumer;

/**
 * Generic context handler.
 *
 * @author Matt
 */
public abstract class ContextHandling {
	protected final GuiController controller;
	protected final CodeArea codeArea;
	private Consumer<Object> consumer;

	/**
	 * @param controller
	 * 		Controller to use.
	 * @param codeArea
	 * 		Text editor events originate from.
	 */
	public ContextHandling(GuiController controller, CodeArea codeArea) {
		this.codeArea = codeArea;
		this.controller = controller;
		codeArea.setOnMousePressed(e -> {
			// Only accept right-click presses
			if (e.getButton() != MouseButton.SECONDARY)
				return;
			// Reset
			codeArea.setContextMenu(null);
			// Mouse to location
			CharacterHit hit = codeArea.hit(e.getX(), e.getY());
			int charPos = hit.getInsertionIndex();
			codeArea.getCaretSelectionBind().displaceCaret(charPos);
			TwoDimensional.Position pos = codeArea.offsetToPosition(charPos,
					TwoDimensional.Bias.Backward);
			// Get selection
			Object selection = getSelection(pos);
			if (selection == null)
				return;
			if (consumer != null)
				consumer.accept(selection);
		});
	}

	/**
	 * Goto the selected item's definition.
	 */
	public void gotoSelectedDef() {
		Object selection = getCurrentSelection();
		// Goto class or member definition
		if (selection instanceof ClassSelection) {
			String owner = ((ClassSelection) selection).name;
			JavaResource resource = controller.getWorkspace().getContainingResourceForClass(owner);
			controller.windows().getMainWindow().openClass(resource, owner);
		} else if (selection instanceof MemberSelection) {
			MemberSelection ms = (MemberSelection) selection;
			JavaResource resource = controller.getWorkspace().getContainingResourceForClass(ms.owner);
			ClassViewport view = controller.windows().getMainWindow().openClass(resource, ms.owner);
			Platform.runLater(() -> view.selectMember(ms.name, ms.desc));
		}
	}

	/**
	 * Open rename field for the class.
	 */
	public void openRenameInput() {
		Object selection = getCurrentSelection();
		Window main = controller.windows().getMainWindow().getStage();
		// Goto class or member definition
		if (selection instanceof ClassSelection) {
			String name = ((ClassSelection) selection).name;
			RenamingTextField popup = RenamingTextField.forClass(controller, name);
			popup.show(main);
		} else if (selection instanceof MemberSelection) {
			MemberSelection ms = (MemberSelection) selection;
			String owner = ms.owner;
			String name = ms.name;
			String desc = ms.desc;
			RenamingTextField popup = RenamingTextField.forMember(controller, owner, name, desc);
			popup.show(main);
		}
	}

	/**
	 * @return Selection value at current caret position.
	 */
	protected abstract Object getSelection(TwoDimensional.Position pos);

	/**
	 * @return Selection value at current caret position.
	 */
	protected abstract Object getCurrentSelection();

	/**
	 * @param consumer
	 * 		Action to take when the user interacts with some selected content.
	 */
	protected void onContextRequest(Consumer<Object> consumer) {
		this.consumer = consumer;
	}
}
