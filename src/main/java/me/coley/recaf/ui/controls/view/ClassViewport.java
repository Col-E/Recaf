package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.util.Map;

/**
 * Multi-view wrapper for classes in resources.
 *
 * @author Matt
 */
public class ClassViewport extends EditorViewport {
	/**
	 * @param controller
	 * 		Controller context.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param path
	 * 		Path to file.
	 */
	public ClassViewport(GuiController controller, JavaResource resource, String path) {
		super(controller, resource, path);
	}

	@Override
	protected History getHistory(String path) {
		return resource.getClassHistory(path);
	}

	@Override
	protected Map<String, byte[]> getMap() {
		return resource.getClasses();
	}

	@Override
	protected void updateView() {
		switch(getClassMode()) {
			case DECOMPILE:
				// TODO: shows decompiled code, allow actions to run on selections
				break;
			case NODE_EDITOR:
				// TODO: more like how Recaf was in 1.X
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				setCenter(hex);
				break;
		}
	}

	/**
	 * @return Mode that indicated which view to use for modifying classes.
	 */
	public ClassMode getClassMode() {
		return controller.config().display().classEditorMode;
	}

	/**
	 * Viewport editor type.
	 */
	public enum ClassMode {
		DECOMPILE, NODE_EDITOR, HEX
	}
}
