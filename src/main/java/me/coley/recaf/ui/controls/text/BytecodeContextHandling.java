package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.text.selection.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Context menu handler for {@link BytecodePane}.
 *
 * @author Matt
 */
public class BytecodeContextHandling extends ContextHandling {
	private RootAST root;

	/**
	 * @param controller
	 * 		Controller to use.
	 * @param codeArea
	 * 		Controller to pull info from.
	 */
	public BytecodeContextHandling(GuiController controller, CodeArea codeArea) {
		super(controller, codeArea);
		// Set context selection action
		onContextRequest(selection -> {
			if (selection instanceof ClassSelection) {
				handleClassType((ClassSelection) selection);
			} else if (selection instanceof MemberSelection){
				MemberSelection ms = (MemberSelection) selection;
				handleMemberType((MemberSelection) selection);
			}
			// TODO: actions for other selection types, see note below
		});
	}

	/**
	 * @param ast
	 * 		Analyzed bytecode.
	 */
	public void setAST(RootAST ast) {
		this.root = ast;
	}

	@Override
	protected Object getSelection(TwoDimensional.Position pos) {
		int line = pos.getMajor()+1;
		int offset = pos.getMinor();
		AST ast = root.getAtLine(line);
		if(ast == null)
			return null;
		if(ast instanceof MethodInsnAST) {
			MethodInsnAST method = (MethodInsnAST) ast;
			if(offset >= method.getOwner().getStart() && offset <= method.getName().getStart())
				return new ClassSelection(method.getOwner().getType(), false);
			else
				return new MemberSelection(method.getOwner().getType(),
						method.getName().getName(), method.getDesc().getDesc(), false);
		} else if(ast instanceof FieldInsnAST) {
			FieldInsnAST field = (FieldInsnAST) ast;
			if(offset >= field.getOwner().getStart() && offset <= field.getName().getStart())
				return new ClassSelection(field.getOwner().getType(), false);
			else
				return new MemberSelection(field.getOwner().getType(),
						field.getName().getName(), field.getDesc().getDesc(), false);
		} else if(ast instanceof TypeInsnAST) {
			TypeInsnAST type = (TypeInsnAST) ast;
			return new ClassSelection(type.getType().getType(), false);
		}
		/*
		 TODO: Create selection types for labels/jumps/switches
			  Label
		          References:          List of insns that reference this label
		      Jump
		          Goto target:         Goto target label
		      Switch
		          Targets: [K:V...]    List of keys to target labels
		 */
		return null;
	}

	private void handleClassType(ClassSelection selection) {
		codeArea.setContextMenu(menu().controller(controller)
				.declaration(selection.dec)
				.ofClass(selection.name));
	}

	private void handleMemberType(MemberSelection selection) {
		ContextBuilder cb = menu().controller(controller)
				.declaration(selection.dec);
		if (selection.method())
			codeArea.setContextMenu(cb.ofMethod(selection.owner, selection.name, selection.desc));
		else
			codeArea.setContextMenu(cb.ofField(selection.owner, selection.name, selection.desc));
	}
}
