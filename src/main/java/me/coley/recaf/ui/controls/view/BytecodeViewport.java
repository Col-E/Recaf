package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.text.BytecodePane;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.util.Map;

/**
 * Wrapper for {@link me.coley.recaf.ui.controls.text.BytecodePane}.
 *
 * @author Matt
 */
public class BytecodeViewport extends EditorViewport {
	private final BytecodePane pane;
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
	 * 		Class that declares the method.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	public BytecodeViewport(GuiController controller, ClassViewport host, JavaResource resource,
							String owner, String name, String desc) {
		super(controller, resource, owner);
		this.pane = new BytecodePane(controller, owner, name, desc);
		this.host = host;
		this.owner = owner;
		setCenter(pane);
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
	protected void save() {
		current = pane.assemble();
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
