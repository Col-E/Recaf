package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.text.JavaPane;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import javax.tools.ToolProvider;
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
				// TODO: If sources are attached, work off of those
				//  - Keep a cache of the modified source
				//  - Otherwise, just decompile and pray for success
				String decompile = DecompileImpl.FF.create()
						.decompile(controller.getWorkspace(), path);
				JavaPane pane = new JavaPane(controller, resource);
				pane.setEditable(ToolProvider.getSystemJavaCompiler() != null);
				pane.setWrapText(false);
				pane.setText(decompile);
				setCenter(pane);
				break;
			case NODE_EDITOR:
				// TODO: like how Recaf was in 1.X
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				setCenter(hex);
				break;
		}
	}

	@Override
	protected void save() {
		// Handle saving for editing decompiled java
		if (getCenter() instanceof JavaPane) {
			try {
				// TODO: If editing a class with inners, return the inners's updated code as well
				current = ((JavaPane) getCenter()).save(path);
			} catch(UnsupportedOperationException ex) {
				Log.warn("Recompiling not supported. Please run Recaf with a JDK.", path);
				return;
			} catch(Exception ex) {
				Log.error(ex, "Failed recompiling code for '{}'", path);
				return;
			}
		}
		// Save content
		super.save();
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
