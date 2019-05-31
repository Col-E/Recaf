package me.coley.recaf.ui.component;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import me.coley.recaf.ui.component.editor.InsnListEditor;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Threads;

public class InsnInserter extends BorderPane {
	private final InsnListEditor list;

	public InsnInserter(InsnListEditor list, AbstractInsnNode ain) {
		this.list = list;
		List<BaseInsnObj> insnPanels = Arrays.asList(new BaseInsnObj.InsnObj(), new BaseInsnObj.VarObj(), new BaseInsnObj.TypeObj(),
				new BaseInsnObj.IntObj(), new BaseInsnObj.FieldObj(), new BaseInsnObj.MethodObj(), new BaseInsnObj.IndyObj(),
				new BaseInsnObj.JumpObj(), new BaseInsnObj.LabelObj(), new BaseInsnObj.LdcObj(), new BaseInsnObj.IincObj(),
				new BaseInsnObj.TableSwitchObj(), new BaseInsnObj.LookupSwitchObj(), new BaseInsnObj.LineObj(),
				new BaseInsnObj.MultiANewArrayObj());
		// tabs
		TabPane tabs = new TabPane();
		for (BaseInsnObj obj : insnPanels) {
			BorderPane pane = new BorderPane();
			ReflectiveInsnSheet propertySheet = new ReflectiveInsnSheet(list, obj.getInsn());
			String name = obj.getInsn().getClass().getSimpleName();
			if (name.equals("InsnNode")) {
				name = name.substring(0, name.length() - "Node".length());
			} else if (name.contains("InsnNode")) {
				name = name.substring(0, name.length() - "InsnNode".length());
			}
			Tab tab = new Tab(name);
			tab.closableProperty().set(false);
			pane.setCenter(propertySheet);
			tab.setContent(pane);
			tabs.getTabs().add(tab);
			// Bottom bar
			ComboBox<InsertMode> comboLocation = new ComboBox<>(JavaFX.observableList(InsertMode.values()));
			comboLocation.setValue(InsertMode.AFTER);
			ActionButton btnAdd = new ActionButton(Lang.get("misc.insert"), () -> {
				add(ain, obj.getInsn(), comboLocation.getValue());
			});
			HBox bar = new HBox();
			bar.getChildren().addAll(comboLocation, btnAdd);
			bar.prefWidthProperty().bind(pane.widthProperty());
			pane.setBottom(bar);
		}
		setCenter(tabs);
	}

	/**
	 * Add opcode to instructions.
	 * 
	 * @param location
	 *            Anchor node.
	 * @param created
	 *            Node to add.
	 * @param mode
	 *            Direction from anchor to insert node at.
	 */
	private void add(AbstractInsnNode location, AbstractInsnNode created, InsertMode mode) {
		Threads.runFx(() -> {
			// update underlying list
			ObservableList<AbstractInsnNode> obsList = list.getInsnList().getItems();
			int index = obsList.indexOf(location);
			if (mode == InsertMode.BEFORE) {
				obsList.add(index, created);
			} else {
				obsList.add(index + 1, created);
			}
			// close inserter window
			Stage stage = (Stage) getScene().getWindow();
			stage.close();
		});
	}

	//@formatter:off
	private static abstract class BaseInsnObj {
		abstract AbstractInsnNode getInsn();
		
		private static class InsnObj extends BaseInsnObj {
			private final InsnNode node = new InsnNode(Opcodes.NOP);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class IntObj extends BaseInsnObj {
			private final IntInsnNode node = new IntInsnNode(Opcodes.BIPUSH, 0);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class VarObj extends BaseInsnObj {
			private final VarInsnNode node = new VarInsnNode(Opcodes.ALOAD, 0);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class TypeObj extends BaseInsnObj {
			private final TypeInsnNode node = new TypeInsnNode(Opcodes.NEW, "ex/Type");
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class FieldObj extends BaseInsnObj {
			private final FieldInsnNode node = new FieldInsnNode(Opcodes.GETFIELD, "owner", "name", "Ltype;");
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class MethodObj extends BaseInsnObj {
			private final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "owner", "name", "()Ltype;", false);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class IndyObj extends BaseInsnObj {
			private final InvokeDynamicInsnNode node = new InvokeDynamicInsnNode("name", "desc", 
					new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()Ltype;", false),
					// args
					Type.getType("Ltype;"),
					new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()Ltype;", false),
					Type.getType("Ltype;")
					);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class JumpObj extends BaseInsnObj {
			private final JumpInsnNode node = new JumpInsnNode(Opcodes.GOTO, null);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class LabelObj extends BaseInsnObj {
			private final LabelNode node = new LabelNode();
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class LdcObj extends BaseInsnObj {
			private final LdcInsnNode node = new LdcInsnNode("string");
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class IincObj extends BaseInsnObj {
			private final IincInsnNode node = new IincInsnNode(0,1);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class TableSwitchObj extends BaseInsnObj {
			private final TableSwitchInsnNode node = new TableSwitchInsnNode(0, 1, null);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class LookupSwitchObj extends BaseInsnObj {
			private final LookupSwitchInsnNode node = new LookupSwitchInsnNode(null, new int[0], new LabelNode[0] );
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class LineObj extends BaseInsnObj {
			private final LineNumberNode node = new LineNumberNode(0, null);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
		
		private static class MultiANewArrayObj extends BaseInsnObj {
			private final MultiANewArrayInsnNode node = new MultiANewArrayInsnNode("Ltype;", 1);
			@Override
			AbstractInsnNode getInsn() {return node;}
		}
	}
	//@formatter:on
}
