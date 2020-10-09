package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.text.BytecodeEditorPane;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.util.Map;

/**
 * Wrapper for {@link BytecodeEditorPane}.
 *
 * @author Matt
 */
public class BytecodeViewport extends EditorViewport {
	private final BytecodeEditorPane pane;
	private final ClassViewport host;
	private final String owner;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param host
	 * 		Class viewport.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param owner
	 * 		Class that declares the Member.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public BytecodeViewport(GuiController controller, ClassViewport host, JavaResource resource,
							String owner, String name, String desc) {
		this(controller, host, resource, owner, new BytecodeEditorPane(controller, owner, name, desc));
	}

	/**
	 * @param controller
	 * 		Controller context.
	 * @param host
	 * 		Class viewport.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param owner
	 * 		Class that declares the Member.
	 * @param editor
	 * 		Member editor.
	 */
	public BytecodeViewport(GuiController controller, ClassViewport host, JavaResource resource,
							String owner, BytecodeEditorPane editor) {
		super(controller, resource, owner);
		this.pane = editor;
		this.host = host;
		this.owner = owner;
		setCenter(pane);
		pane.setWrapText(controller.config().display().forceWordWrap);
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
		// Try to disassemble
		boolean success = pane.disassemble();
		pane.setEditable(success);
		if (!success)
			pane.setText("// Failed to disassemble: " + path);
	}

	@Override
	public void save() {
		try {
			current = pane.assemble();
		} catch(Throwable t) {
			Log.error(t, "Uncaught exception when assembling method");
			UiUtil.animateFailure(getCenter(), 500);
		}
		if (current == null)
			return;
		super.save();
		// Update viewport
		if (host != null) {
			ClassViewport view = host;
			// Check of host holds the class that defines the method, if not, see if that class is open
			if(!owner.equals(view.path))
				view = controller.windows().getMainWindow().getClassViewport(owner);
			if(view != null)
				view.updateView();
		}
	}
}
