package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.text.JavaPane;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.io.PrintWriter;
import java.io.StringWriter;
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
	public void updateView() {
		switch(getClassMode()) {
			case DECOMPILE:
				// Get or create pane
				JavaPane pane = null;
				if (getCenter() instanceof JavaPane) {
					pane = (JavaPane) getCenter();
				} else {
					pane = new JavaPane(controller, resource);
					pane.setWrapText(false);
					pane.setEditable(pane.canCompile() && resource.isPrimary());
					setCenter(pane);
				}
				// Decompile
				String decompile = null;
				try {
					decompile = controller.config().decompile().decompiler.create()
							.decompile(controller.getWorkspace(), path);
				} catch(Exception ex) {
					// Print decompile error
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					decompile = LangUtil.translate("decompile.fail") + "\n\nError Message: "
							+ ex.getMessage() + "\n\nStackTrace:\n" + sw.toString();
					pane.setEditable(false);
				}
				// Update text
				pane.setText(decompile);
				break;
			case NODE_EDITOR:
				// TODO: like how Recaf was in 1.X
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				hex.setEditable(resource.isPrimary());
				setCenter(hex);
				break;
		}
	}

	@Override
	protected void save() {
		// Handle saving for editing decompiled java
		if (getCenter() instanceof JavaPane) {
			try {
				// TODO: If editing a class with inners, update the inners as well
				current = ((JavaPane) getCenter()).save(path).get(path);
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
	 * Jump to the definition of the given member.
	 *
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public void selectMember(String name, String desc) {
		if (getCenter() instanceof  JavaPane)
			((JavaPane)getCenter()).selectMember(name, desc);
		// TODO: When NODE_EDITOR-mode is implemented, add support
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
