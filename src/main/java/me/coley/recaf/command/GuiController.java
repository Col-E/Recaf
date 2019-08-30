package me.coley.recaf.command;

import java.io.File;

/**
 * Gui controller.
 *
 * @author Matt
 */
public class GuiController extends Controller {
	/**
	 * @param workspace
	 * 		Initial workspace file. Can point to a file to load <i>(class, jar)</i> or a workspace
	 * 		configuration <i>(json)</i>.
	 */
	public GuiController(File workspace) {
		super(workspace);
	}

	@Override
	public void run() {
		throw new UnsupportedOperationException("TODO");
	}
}
