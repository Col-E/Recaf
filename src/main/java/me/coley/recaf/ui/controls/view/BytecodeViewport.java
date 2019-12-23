package me.coley.recaf.ui.controls.view;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.text.BytecodePane;
import me.coley.recaf.ui.controls.text.JavaPane;
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
	private final JavaPane host;
	private final String owner;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param host
	 * 		Editor pane containing the declaring class.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param owner
	 * 		Class that declares the method.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param access
	 * 		Method modifiers.
	 */
	public BytecodeViewport(GuiController controller, JavaPane host, JavaResource resource,
							String owner, String name, String desc, int access) {
		super(controller, resource, owner);
		this.pane = new BytecodePane(controller, resource, owner, name, desc, access);
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
		super.save();
		// Update viewport
		ClassViewport view = (ClassViewport) host.getParent();
		// Check of host holds the class that defines the method, if not, see if that class is open
		if(!owner.equals(view.path))
			view = controller.windows().getMainWindow().getClassViewport(owner);
		if(view != null)
			view.updateView();

	}
}
