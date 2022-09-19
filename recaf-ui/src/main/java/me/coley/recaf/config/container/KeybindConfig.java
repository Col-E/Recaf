package me.coley.recaf.config.container;

import javafx.scene.Parent;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.binds.Binding;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Representation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.Undoable;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.NodeEvents;

import static javafx.scene.input.KeyCode.*;
import static me.coley.recaf.config.binds.Binding.newBind;
import static me.coley.recaf.config.binds.BindingCreator.OSBinding.newOsBind;
import static me.coley.recaf.config.binds.BindingCreator.bindings;
import static me.coley.recaf.util.PlatformType.MAC;

/**
 * Config container for keybindings.
 *
 * @author Matt Coley
 */
public class KeybindConfig implements ConfigContainer {
	/**
	 * Open find prompt in the current editor, if it is supported.
	 */
	@Group("navigation")
	@ConfigID("find")
	public Binding find = newBind(CONTROL, F);

	/**
	 * Close current tab.
	 */
	@Group("navigation")
	@ConfigID("closetab")
	public Binding closeTab = bindings(
			newBind(CONTROL, W),
			newOsBind(MAC, newBind(META, W))
	).buildKeyBindingForCurrentOS();

	/**
	 * Close current tab.
	 */
	@Group("navigation")
	@ConfigID("fullscreen")
	public Binding fullscreen = newBind(F11);

	/**
	 * Open a search prompt to quickly access classes and files.
	 */
	@Group("navigation")
	@ConfigID("quicknav")
	public Binding quickNav = bindings(
			newBind(CONTROL, G),
			newOsBind(MAC, newBind(META, G))
	).buildKeyBindingForCurrentOS();

	/**
	 * Save changes in current editor.
	 */
	@Group("edit")
	@ConfigID("save")
	public Binding save = bindings(
			newBind(CONTROL, S),
			newOsBind(MAC, newBind(META, S))
	).buildKeyBindingForCurrentOS();

	/**
	 * Undo last change in current class.
	 */
	@Group("edit")
	@ConfigID("undo")
	public Binding undo = bindings(
			newBind(CONTROL, U),
			newOsBind(MAC, newBind(META, U))
	).buildKeyBindingForCurrentOS();

	/**
	 * Increase font size by one.
	 */
	@Group("appearance")
	@ConfigID("fontsize.up")
	public Binding fontSizeUp = newBind(CONTROL, ADD);

	/**
	 * Decrease font size by one.
	 */
	@Group("appearance")
	@ConfigID("fontsize.down")
	public Binding fontSizeDown = newBind(CONTROL, SUBTRACT);

	/**
	 * Goto the definition of whatever the caret position is over.
	 */
	@Group("code")
	@ConfigID("gotodef")
	public Binding gotoDef = newBind(F3);

	/**
	 * Rename whatever the caret position is over.
	 */
	@Group("code")
	@ConfigID("rename")
	public Binding rename = newBind(CONTROL, R);

	/**
	 * Rename whatever the caret position is over.
	 */
	@Group("code")
	@ConfigID("searchref")
	public Binding searchReferences = newBind(CONTROL, H);

	/**
	 * Initiate suggestion prompt for current text.
	 */
	@Group("code")
	@ConfigID("suggest")
	public Binding suggest = newBind(CONTROL, SPACE);

	@Override
	public String iconPath() {
		return Icons.KEYBOARD;
	}

	@Override
	public String internalName() {
		return "conf.binding";
	}

	/**
	 * Track if the user is updating a {@link Binding}.
	 * Used to prevent overlapping binds from being fired during the bind process.
	 */
	private transient boolean isEditingBind;

	/**
	 * @param isEditingBind
	 * 		New editing state.
	 */
	public void setIsUpdating(boolean isEditingBind) {
		this.isEditingBind = isEditingBind;
	}

	/**
	 * Used to prevent overlapping binds from being fired during the bind process.
	 *
	 * @return Bind editing state.
	 */
	public boolean isEditingBind() {
		return isEditingBind;
	}

	/**
	 * @param parent
	 * 		Component to install editor keybinds into.
	 */
	public void installEditorKeys(Parent parent) {
		NodeEvents.addKeyPressHandler(parent, e -> {
			// Shouldn't happen, but just for sanity
			if (isEditingBind())
				return;
			// Standard editor binds
			Representation representation = (Representation) parent;
			if (representation.supportsEditing() && save.match(e)) {
				SaveResult result = representation.save();
				// Visually indicate result
				if (result == SaveResult.SUCCESS) {
					Animations.animateSuccess(representation.getNodeRepresentation(), 1000);
				} else if (result == SaveResult.FAILURE) {
					Animations.animateFailure(representation.getNodeRepresentation(), 1000);
				}
			}
			if (representation instanceof Undoable && undo.match(e)) {
				((Undoable) representation).undo();
			}
			// Class specific binds
			if (representation instanceof ClassRepresentation) {
				ClassRepresentation classRepresentation = (ClassRepresentation) representation;
				if (classRepresentation.isMemberSelectionReady()) {
					// TODO: Rename current selection
				}
			}
		});
	}
}
