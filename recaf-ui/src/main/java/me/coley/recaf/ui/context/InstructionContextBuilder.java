package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.pane.assembler.AssemblerPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import static me.coley.recaf.ui.util.Menus.action;
import static me.coley.recaf.ui.util.Menus.createHeader;

/**
 * Context menu builder for method instructions.
 *
 * @author Matt Coley
 */
public class InstructionContextBuilder extends ContextBuilder {
	private CommonClassInfo ownerInfo;
	private MethodInfo methodInfo;

	/**
	 * @param owner
	 * 		Class information about selected item's defining class.
	 *
	 * @return Builder.
	 */
	public InstructionContextBuilder setOwnerInfo(CommonClassInfo owner) {
		ownerInfo = owner;
		return this;
	}

	/**
	 * @param method
	 * 		Method declaring the instruction.
	 *
	 * @return Builder.
	 */
	public InstructionContextBuilder setMethodInfo(MethodInfo method) {
		methodInfo = method;
		return this;
	}

	@Override
	public ContextMenu build() {
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(methodInfo.getName(), Icons.getMethodIcon(methodInfo)));
		menu.getItems().add(action("menu.goto.method", Icons.OPEN, this::openDefinition));
		menu.getItems().add(action("menu.goto.instruction", Icons.OPEN, this::openInstruction));
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = ownerInfo.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForClass(name);
		if (resource == null)
			resource = workspace.getResources().getContainingForDexClass(name);
		if (resource == null)
			logger.warn("Could not find container resource for class {}", name);
		return resource;
	}

	private void openDefinition() {
		CommonUX.openMember(ownerInfo, methodInfo);
	}

	private void openInstruction() {
		String name = ownerInfo.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (ownerInfo instanceof ClassInfo) {
				// Open assembler
				AssemblerPane assembler = new AssemblerPane();
				assembler.setTargetMember(methodInfo);
				assembler.onUpdate(ownerInfo);
				// TODO: Highlight instruction
				new GenericWindow(assembler, 800, 600).show();
			} else if (ownerInfo instanceof DexClassInfo) {
				// TODO: Copy dex member
				logger.warn("Android currently unsupported");
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}
}
