package me.coley.recaf.ui.component;


import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.recaf.Input;
import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.analysis.Verify;
import me.coley.recaf.bytecode.search.Parameter;
import me.coley.recaf.bytecode.search.StringMode;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.FxAssembler;
import me.coley.recaf.ui.FxSearch;
import me.coley.recaf.ui.component.editor.InsnListEditor;
import me.coley.recaf.util.*;
import org.controlsfx.control.PropertySheet;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.util.*;

public class InsnCell extends ListCell<AbstractInsnNode> {
	private final InsnListEditor.InstructionList list;

	public InsnCell(InsnListEditor.InstructionList list) {
		this.list = list;
	}

	@Override
	protected void updateItem(AbstractInsnNode node, boolean empty) {
		super.updateItem(node, empty);
		if(empty || node == null) {
			setGraphic(null);
		} else {
			InsnHBox box = list.getNodeLookup().get(node);
			BorderPane bp = new BorderPane(box);
			Verify.VerifyResults verif = list.getVerificationResults();
			if(verif != null && node == verif.getCause()) {
				String msg = verif.ex.getMessage().split("\n")[0];
				Label lbl = new Label(msg);
				bp.getStyleClass().add("op-verif-fail");
				bp.setRight(lbl);
			}
			setGraphic(bp);
			// wrapped in a mouse-click event so they're generated when clicked.
			// without the wrapper, the selection cannot be known.
			setOnMouseClicked(e -> setContextMenu(createMenu(e, node)));
		}
	}

	private ContextMenu createMenu(MouseEvent e, AbstractInsnNode node) {
		// context menu
		ContextMenu ctx = new ContextMenu();
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.edit"), () -> {
			showInsnEditor(node);
		}));
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.stackhelper"), () -> {
			StackWatcher stack = new StackWatcher(list.getClassNode(), list.getMethod());
			list.getSelectionModel().selectedIndexProperty().addListener(stack);
			list.getItems().addListener(stack);
			stack.update();
			stack.select(list.getSelectionModel().getSelectedIndex());
			stack.show();
		}));
		if(node.getPrevious() != null) {
			ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.move.up"), () -> {
				InsnUtil.shiftUp(list.getInsns(), list.getSelectionModel().getSelectedItems());
				Bus.post(new ClassDirtyEvent(list.getClassNode()));
				list.refreshList();
				list.sortList();
			}));
		}
		if(node.getNext() != null) {
			ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.move.down"), () -> {
				InsnUtil.shiftDown(list.getInsns(), list.getSelectionModel().getSelectedItems());
				Bus.post(new ClassDirtyEvent(list.getClassNode()));
				list.refreshList();
				list.sortList();
			}));
		}
		// default action to first context menu item (edit)
		if((e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) || (e.getButton() ==
				MouseButton.MIDDLE)) {
			showInsnEditor(node);
		}
		if(list.getSelectionModel().getSelectedItems().size() == 1) {
			Input in = Input.get();
			// type-specific options
			switch(node.getType()) {
				case AbstractInsnNode.FIELD_INSN:
					// open definition
					// search references
					FieldInsnNode fin = (FieldInsnNode) node;
					if(in.classes.contains(fin.owner)) {
						ClassNode fOwner = in.getClass(fin.owner);
						FieldNode field = getField(fOwner, fin);
						if(field != null) {
							ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method" +
									".define"), () -> {
								Bus.post(new FieldOpenEvent(fOwner, field, list));
							}));
						}
					}
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.search"), ()
							-> {
						Parameter p = Parameter.references(fin.owner, fin.name, fin.desc);
						p.setStringMode(StringMode.EQUALITY);
						FxSearch.open(p);
					}));
					break;
				case AbstractInsnNode.METHOD_INSN:
					// open definition
					// search references
					MethodInsnNode min = (MethodInsnNode) node;
					if(in.classes.contains(min.owner)) {
						ClassNode mOwner = in.getClass(min.owner);
						MethodNode method = getMethod(mOwner, min);
						if(method != null) {
							ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method" +
									".define"), () -> {
								Bus.post(new MethodOpenEvent(mOwner, method, list));
							}));
						}
					}
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.search"), ()
							-> {
						Parameter p = Parameter.references(min.owner, min.name, min.desc);
						p.setStringMode(StringMode.EQUALITY);
						FxSearch.open(p);
					}));
					break;
				case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
					// open definition
					// search references
					InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
					if(indy.bsmArgs.length >= 2 && indy.bsmArgs[1] instanceof Handle) {
						Handle h = (Handle) indy.bsmArgs[1];
						if(in.classes.contains(h.getOwner())) {
							ClassNode mOwner = in.getClass(h.getOwner());
							MethodNode method = getMethod(mOwner, h);
							if(mOwner != null && method != null) {
								ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method" +
										".define"), () -> {
									Bus.post(new MethodOpenEvent(mOwner, method, list));
								}));
							}
						}
						ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.search"),
								() -> {
							Parameter p = Parameter.references(h.getOwner(), h.getName(), h
									.getDesc());
							p.setStringMode(StringMode.EQUALITY);
							FxSearch.open(p);
						}));
					}

					break;
				case AbstractInsnNode.TYPE_INSN:
					// open definition
					// search references
					TypeInsnNode tin = (TypeInsnNode) node;
					if(in.classes.contains(tin.desc)) {
						ClassNode tOwner = in.getClass(tin.desc);
						if(tOwner != null) {
							ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method" +
									".define"), () -> {
								Bus.post(new ClassOpenEvent(tOwner));
							}));
						}
						ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.search"),
								() -> {
							FxSearch.open(Parameter.references(tin.desc, null, null));
						}));
					}
					break;
			}
		} else {
			ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.block.save"), () -> {
				showBlockSave();
			}));
		}
		// insert block
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.block.load"), () -> {
			showBlockLoad(node);
		}));
		// insert instruction
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.insertstandard"), () -> {
			showInsnInserter(node);
		}));
		// insert instructions
		ctx.getItems().add(new ActionMenuItem(Lang.get("ui.edit.method.insertassembly"), () -> {
			showInsnInserterAssembler(node);
		}));
		// remove instructions
		ctx.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> {
			list.getItems().removeAll(list.getSelectionModel().getSelectedItems());
		}));
		return ctx;
	}

	private void showInsnEditor(AbstractInsnNode node) {
		InsnEditor x = new InsnEditor(list.outer(), node);
		String t = Lang.get("misc.edit") + ":" + node.getClass().getSimpleName();
		Scene sc = JavaFX.scene(x, 500, 300);
		Stage st = JavaFX.stage(sc, t, true);
		st.setOnCloseRequest(a -> list.refreshItem(node));
		st.show();
	}

	private void showInsnInserter(AbstractInsnNode node) {
		// Opcode type select
		// ---> specific values
		// before / after insertion point
		InsnInserter x = new InsnInserter(list.outer(), node);
		String t = Lang.get("ui.edit.method.insert.title");
		Scene sc = JavaFX.scene(x, ScreenUtil.prefWidth() - 100, 400);
		Stage st = JavaFX.stage(sc, t, true);
		st.setOnCloseRequest(a -> list.refresh());
		st.show();
	}

	private void showInsnInserterAssembler(AbstractInsnNode node) {
		FxAssembler fx = FxAssembler.insns(list.getClassNode(), list.getMethod(), m -> {
			Threads.runFx(() -> {
				Collection<AbstractInsnNode> created = Arrays.asList(m.instructions.toArray());
				int index = list.getItems().indexOf(node);
				list.getItems().addAll(index + 1, created);
				list.refresh();
			});
		});
		fx.setMinWidth(400);
		fx.setMinHeight(200);
		fx.show();
	}

	private void showBlockSave() {
		BlockPane.Saver x = new BlockPane.Saver(list.getSelectionModel().getSelectedItems(), list.getMethod());
		String t = Lang.get("ui.edit.method.block.title");
		Scene sc = JavaFX.scene(x, ScreenUtil.prefWidth() - 100, 420);
		Stage st = JavaFX.stage(sc, t, true);
		st.show();
	}

	private void showBlockLoad(AbstractInsnNode node) {
		BlockPane.Inserter x = new BlockPane.Inserter(list.getSelectionModel().getSelectedItem(), list.outer());
		String t = Lang.get("ui.edit.method.block.title");
		Scene sc = JavaFX.scene(x, ScreenUtil.prefWidth() - 100, 460);
		Stage st = JavaFX.stage(sc, t, true);
		st.show();
	}

	private FieldNode getField(ClassNode owner, FieldInsnNode fin) {
		Optional<FieldNode> opt = owner.fields.stream()
				.filter(f -> f.name.equals(fin.name) && f.desc.equals(fin.desc))
				.findAny();
		return opt.orElse(null);
	}

	private MethodNode getMethod(ClassNode owner, MethodInsnNode min) {
		Optional<MethodNode> opt = owner.methods.stream()
				.filter(m -> m.name.equals(min.name) && m.desc.equals(min.desc))
				.findAny();
		return opt.orElse(null);
	}

	private MethodNode getMethod(ClassNode owner, Handle h) {
		Optional<MethodNode> opt = owner.methods.stream()
				.filter(m -> m.name.equals(h.getName()) && m.desc.equals(h.getDesc()))
				.findAny();
		return opt.orElse(null);
	}

	public class InsnEditor extends BorderPane {
		public InsnEditor(InsnListEditor list, AbstractInsnNode ain) {
			PropertySheet propertySheet = new ReflectiveInsnSheet(list, ain);
			VBox.setVgrow(propertySheet, Priority.ALWAYS);
			setCenter(propertySheet);
		}
	}
}