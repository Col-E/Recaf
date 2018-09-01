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
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Threads;

public class InsnInserter extends BorderPane {
	private final InsnListEditor list;

	public InsnInserter(InsnListEditor list, AbstractInsnNode ain) {
		this.list = list;
		List<OpcodeObj> opcodePanels = Arrays.asList(new OpcodeObj.InsnObj(), new OpcodeObj.VarObj(), new OpcodeObj.TypeObj(),
				new OpcodeObj.FieldObj(), new OpcodeObj.MethodObj(), new OpcodeObj.IndyObj(), new OpcodeObj.JumpObj(),
				new OpcodeObj.LabelObj(), new OpcodeObj.LdcObj(), new OpcodeObj.IincObj(), new OpcodeObj.TableSwitchObj(),
				new OpcodeObj.LookupSwitchObj(), new OpcodeObj.LineObj(), new OpcodeObj.MultiANewArrayObj());
		// tabs
		TabPane tabs = new TabPane();
		for (OpcodeObj obj : opcodePanels) {
			BorderPane pane = new BorderPane();
			ReflectiveOpcodeSheet propertySheet = new ReflectiveOpcodeSheet(list, obj.getOpcode());;

			String name = obj.getOpcode().getClass().getSimpleName();
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
			ActionButton btnAdd = new ActionButton(Lang.get("misc.add"), () -> {
				add(ain, obj.getOpcode(), comboLocation.getValue());
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
			ObservableList<AbstractInsnNode> obsList = list.getOpcodeList().getItems();
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
	private static abstract class OpcodeObj {
		abstract AbstractInsnNode getOpcode();
		
		private static class InsnObj extends OpcodeObj {
			private final InsnNode node = new InsnNode(Opcodes.NOP);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class VarObj extends OpcodeObj {
			private final VarInsnNode node = new VarInsnNode(Opcodes.ALOAD, 0);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class TypeObj extends OpcodeObj {
			private final TypeInsnNode node = new TypeInsnNode(Opcodes.NEW, "ex/Type");
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class FieldObj extends OpcodeObj {
			private final FieldInsnNode node = new FieldInsnNode(Opcodes.GETFIELD, "owner", "name", "Ltype;");
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class MethodObj extends OpcodeObj {
			private final MethodInsnNode node = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "owner", "name", "()Ltype;", false);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class IndyObj extends OpcodeObj {
			private final InvokeDynamicInsnNode node = new InvokeDynamicInsnNode("name", "desc", 
					new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()Ltype;", false),
					// args
					Type.getType("Ltype;"),
					new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()Ltype;", false),
					Type.getType("Ltype;")
					);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class JumpObj extends OpcodeObj {
			private final JumpInsnNode node = new JumpInsnNode(Opcodes.GOTO, null);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class LabelObj extends OpcodeObj {
			private final LabelNode node = new LabelNode();
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class LdcObj extends OpcodeObj {
			private final LdcInsnNode node = new LdcInsnNode("string");
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class IincObj extends OpcodeObj {
			private final IincInsnNode node = new IincInsnNode(0,1);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class TableSwitchObj extends OpcodeObj {
			private final TableSwitchInsnNode node = new TableSwitchInsnNode(0, 1, null);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class LookupSwitchObj extends OpcodeObj {
			private final LookupSwitchInsnNode node = new LookupSwitchInsnNode(null, new int[0], new LabelNode[0] );
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class LineObj extends OpcodeObj {
			private final LineNumberNode node = new LineNumberNode(0, null);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
		
		private static class MultiANewArrayObj extends OpcodeObj {
			private final MultiANewArrayInsnNode node = new MultiANewArrayInsnNode("Ltype;", 1);
			@Override
			AbstractInsnNode getOpcode() {return node;}
		}
	}
	//@formatter:on
}
