package me.coley.recaf.ui.controls.text;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.text.selection.*;
import me.coley.recaf.util.LangUtil;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Context menu handler for {@link BytecodeEditorPane}.
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
			if(selection instanceof ClassSelection) {
				handleClassType((ClassSelection) selection);
			} else if(selection instanceof MemberSelection) {
				handleMemberType((MemberSelection) selection);
			} else if(selection instanceof LabelSelection) {
				handleLabelType((LabelSelection) selection);
			} else if(selection instanceof JumpSelection) {
				handleJumpType((JumpSelection) selection);
			} else if(selection instanceof SwitchSelection) {
				handleSwitchType((SwitchSelection) selection);
			} else if(selection instanceof VariableSelection) {
				handleVariableType((VariableSelection) selection);
			}
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
		if (root == null)
			return null;
		AST ast = root.getAtLine(line);
		if(ast == null)
			return null;
		// Check for members
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
		}
		// Check for types
		else if(ast instanceof TypeInsnAST) {
			TypeInsnAST type = (TypeInsnAST) ast;
			return new ClassSelection(type.getType().getType(), false);
		}
		// Check for control flow
		else if(ast instanceof LabelAST) {
			LabelAST label = (LabelAST) ast;
			return new LabelSelection(label.getName().getName());
		} else if(ast instanceof JumpInsnAST) {
			JumpInsnAST jump = (JumpInsnAST) ast;
			return new JumpSelection(jump.getLabel().getName());
		} else if(ast instanceof TableSwitchInsnAST) {
			TableSwitchInsnAST swit = (TableSwitchInsnAST) ast;
			Map<String, String> map = new LinkedHashMap<>();
			int value = swit.getRangeMin().getIntValue();
			for(NameAST target : swit.getLabels()) {
				map.put(target.getName(), String.valueOf(value));
				value++;
			}
			return new SwitchSelection(map, swit.getDfltLabel().getName());
		} else if(ast instanceof LookupSwitchInsnAST) {
			LookupSwitchInsnAST swit = (LookupSwitchInsnAST) ast;
			Map<String, String> map = swit.getMapping().entrySet().stream()
					.map(e -> new AbstractMap.SimpleEntry<>(e.getValue().getName(), e.getKey().print()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
			return new SwitchSelection(map, swit.getDfltLabel().getName());
		}
		// Check for variable references
		else if(ast instanceof VarInsnAST) {
			VarInsnAST vari = (VarInsnAST) ast;
			return new VariableSelection(vari.getVariableName().getName());
		}
		return null;
	}

	@Override
	protected Object getCurrentSelection() {
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

	private void handleLabelType(LabelSelection selection) {
		ContextMenu menu = new ContextMenu();
		Menu refs = new Menu(LangUtil.translate("ui.edit.method.referrers"));
		for (AST ast : root.getChildren()) {
			if ((ast instanceof FlowController && ((FlowController) ast).targets().contains(selection.name)) ||
					(ast instanceof LineInsnAST && ((LineInsnAST) ast).getLabel().getName().equals(selection.name))) {
				MenuItem ref = new ActionMenuItem(ast.getLine() + ": " + ast.print(), () -> {
					int line = ast.getLine() - 1;
					codeArea.moveTo(line, 0);
					codeArea.requestFollowCaret();
				});
				refs.getItems().add(ref);
			}
		}
		if (refs.getItems().isEmpty())
			refs.setDisable(true);
		menu.getItems().add(refs);
		codeArea.setContextMenu(menu);
	}

	private void handleVariableType(VariableSelection selection) {
		ContextMenu menu = new ContextMenu();
		Menu refs = new Menu(LangUtil.translate("ui.edit.method.referrers"));
		for (AST ast : root.getChildren()) {
			if (ast instanceof VarInsnAST && ((VarInsnAST) ast).getVariableName().getName().equals(selection.name)) {
				MenuItem ref = new ActionMenuItem(ast.getLine() + ": " + ast.print(), () -> {
					int line = ast.getLine() - 1;
					codeArea.moveTo(line, 0);
					codeArea.requestFollowCaret();
				});
				refs.getItems().add(ref);
			}
		}
		if (refs.getItems().isEmpty())
			refs.setDisable(true);
		menu.getItems().add(refs);
		codeArea.setContextMenu(menu);
	}

	private void handleJumpType(JumpSelection selection) {
		ContextMenu menu = new ContextMenu();
		MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.follow"), () -> {
			for (AST ast : root.getChildren()) {
				if (ast instanceof LabelAST) {
					String name = ((LabelAST) ast).getName().getName();
					if(name.equals(selection.destination)) {
						int line = ast.getLine() - 1;
						codeArea.moveTo(line, 0);
						codeArea.requestFollowCaret();
					}
				}
			}
		});
		menu.getItems().add(jump);
		codeArea.setContextMenu(menu);
	}

	private void handleSwitchType(SwitchSelection selection) {
		ContextMenu menu = new ContextMenu();
		Menu refs = new Menu(LangUtil.translate("ui.edit.method.follow"));
		for(AST ast : root.getChildren()) {
			if(ast instanceof LabelAST) {
				String name = ((LabelAST) ast).getName().getName();
				String key = selection.mappings.get(name);
				if (key == null && name.equals(selection.dflt))
					key = "Default";
				if(key != null) {
					MenuItem ref = new ActionMenuItem(key + ": '" + ast.getLine() + ": " + ast.print() +"'", () -> {
						int line = ast.getLine() - 1;
						codeArea.moveTo(line, 0);
						codeArea.requestFollowCaret();
					});
					refs.getItems().add(ref);
				}
			}
		}
		if(refs.getItems().isEmpty())
			refs.setDisable(true);
		menu.getItems().add(refs);
		codeArea.setContextMenu(menu);
	}
}
