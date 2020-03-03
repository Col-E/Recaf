package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ClassEditor;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.node.ClassNodePane;
import me.coley.recaf.ui.controls.text.JavaPane;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

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
			case DECOMPILE: {
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
							.decompile(controller, path);
					decompile = EscapeUtil.unescapeUnicode(decompile);
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
				pane.forgetHistory();
				break;
			}
			case NODE_EDITOR: {
				// TODO: like how Recaf was in 1.X
				ClassNodePane pane = null;
				if(getCenter() instanceof ClassNodePane) {
					pane = (ClassNodePane) getCenter();
					pane.setup();
				} else {
					ClassReader cr = controller.getWorkspace().getClassReader(path);
					ClassNode node = ClassUtil.getNode(cr, ClassReader.SKIP_FRAMES);
					pane = new ClassNodePane(controller, node);
					setCenter(pane);
				}
				break;
			}
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
		// TODO: If editing a class with inners, update the inners as well
		//  - Editor controls implement ClassEditor, which has "Map<> save()"
		//  - Should update any modified inner classes

		// Handle saving for editing decompiled java
		if (getCenter() instanceof JavaPane) {
			try {
				current = ((ClassEditor) getCenter()).save(path).get(path);
			} catch(UnsupportedOperationException ex) {
				Log.warn("Recompiling not supported. Please run Recaf with a JDK.", path);
				return;
			} catch(Exception ex) {
				Log.error(ex, "Failed recompiling code for '{}'", path);
				return;
			}
		} else if (getCenter() instanceof ClassNodePane) {
			try {
				current = ((ClassEditor) getCenter()).save(path).get(path);
			} catch(Exception ex) {
				Log.error(ex, "Failed saving changes for '{}'", path);
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
		if (getCenter() instanceof  ClassEditor)
			((ClassEditor)getCenter()).selectMember(name, desc);
	}


	/**
	 * @return Mode that indicated which view to use for modifying classes.
	 */
	private ClassMode getClassMode() {
		return controller.config().display().classEditorMode;
	}

	/**
	 * Viewport editor type.
	 */
	public enum ClassMode {
		DECOMPILE, NODE_EDITOR, HEX
	}
}
