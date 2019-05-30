package me.coley.recaf.ui.component.editor;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.*;
import me.coley.recaf.bytecode.analysis.Verify;
import me.coley.recaf.bytecode.analysis.Verify.VerifyResults;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.config.impl.ConfBlocks;
import me.coley.recaf.config.impl.ConfKeybinds;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.*;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.ui.component.ActionMenuItem;
import me.coley.recaf.ui.component.InsnCell;
import me.coley.recaf.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Editor for method instructions.
 *
 * @author Matt
 */
public class InsnListEditor extends BorderPane {
	private final InstructionList insnList;
	private final ClassNode owner;
	private final MethodNode method;

	public InsnListEditor(ClassNode owner, MethodNode method) {
		this.owner = owner;
		this.method = method;
		this.insnList = new InstructionList(method.instructions);
		setCenter(insnList);
		checkVerify();
	}

	public InsnListEditor(ClassNode owner, MethodNode method, AbstractInsnNode insn) {
		this(owner, method);
	}

	public InstructionList getInsnList() {
		return insnList;
	}

	public ClassNode getClassNode() {
		return owner;
	}

	public MethodNode getMethod() {
		return method;
	}

	@Listener
	private void onClassDirty(ClassDirtyEvent event) {
		if (event.getNode() == owner) {
			checkVerify();
		}
	}

	@Listener
	private void onClassRevert(HistoryRevertEvent event) {
		// This code has become irrelevant, so it should be closed to prevent
		// possible confusion.
		if (event.getName().equals(owner.name)) {
			getScene().getWindow().hide();
		}
	}

	@Listener
	private void onClassRename(ClassRenameEvent event) {
		// This code has become irrelevant, so it should be closed to prevent
		// possible confusion.
		if (event.getOriginalName().equals(owner.name)) {
			getScene().getWindow().hide();
		}
	}

	private void checkVerify() {
		if (ConfASM.instance().doVerify()) {
			VerifyResults res = Verify.checkValid(owner.name, method);
			if (res.valid()) {
				if (!insnList.getStyleClass().contains("verify-pass")) {
					insnList.getStyleClass().add("verify-pass");
				}
				insnList.getStyleClass().remove("verify-fail");
			} else {
				if (!insnList.getStyleClass().contains("verify-fail")) {
					insnList.getStyleClass().add("verify-fail");
				}
				insnList.getStyleClass().remove("verify-pass");
			}
			insnList.updateVerification(res);
		}
	}

	/**
	 * Instruction list wrapper.
	 *
	 * @author Matt
	 */
	public class InstructionList extends ListView<AbstractInsnNode> {
		/**
		 * Work-around for being unable to style ListView's cells. Instead a
		 * cache of the HBox's of the instruction is maintained.
		 */
		private final Map<AbstractInsnNode, InsnHBox> nodeLookup = new LinkedHashMap<>();
		/**
		 * Method instruction list. Updated when ListView fires change events.
		 */
		private final InsnList instructions;
		/**
		 * Last verification results.
		 */
		private VerifyResults verif;

		public InstructionList(InsnList instructions) {
			this.instructions = instructions;
			setupModel();
			setupListeners();
		}

		private void setupModel() {
			getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			getItems().addAll(instructions.toArray());
			// Create format entry for instructions.
			setCellFactory(cell -> new InsnCell(this));
			//
			if (getItems().size() == 0) {
				// Hack to allow insertion of an initial instruction.
				ContextMenu ctx = new ContextMenu();
				ctx.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
					// Add basic starter instructions
					LabelNode start = new LabelNode();
					InsnNode ret = new InsnNode(Opcodes.RETURN);
					LabelNode end = new LabelNode();
					// Add 'this'
					if (method.localVariables == null) {
						method.localVariables = new ArrayList<>();
					}
					if (method.localVariables.size() == 0 && !AccessFlag.isStatic(method.access)) {
						method.localVariables.add(new LocalVariableNode("this", "L" + owner.name + ";", null, start, end, 0));
						method.maxLocals = 1;
					}
					getItems().addAll(start, ret, end);
					setContextMenu(null);
					refreshList();
				}));
				setContextMenu(ctx);
			}
			// add instructions to item list
			refreshList();
		}

		private void setupListeners() {
			// If I read the docs right, changes are ranges without breaks.
			// If you have instructions [A-Z] removing ABC+XYZ will make two changes.
			getItems().addListener((ListChangeListener.Change<? extends AbstractInsnNode> c) -> {
				while (c.next()) {
					if (c.wasRemoved()) {
						onRemove(c.getRemoved());
					} else if (c.wasAdded()) {
						onAdd(c.getFrom(), c.getAddedSubList());
					}
					// Update checks *should not* be required due to the
					// reflective nature of how they are edited + instance
					// sharing between InsnList and this ListView.
				}
				// update size after potential non-updating removals
				setSize(getItems().size());
			});
			getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
				onSelect(getSelectionModel().getSelectedItems());
			});
			// delete key to remove items
			setOnKeyPressed(event -> {
				if (event.getCode().equals(KeyCode.DELETE)) {
					getItems().removeAll(selectionModelProperty().getValue().getSelectedItems());
				}
			});
			//Keybinds for duplicate line
			addEventHandler(KeyEvent.KEY_RELEASED,e->{
				ConfKeybinds instance = ConfKeybinds.instance();
				if (instance.active && !e.isControlDown()) {
					return;
				}
				if (!e.getCode().getName().equalsIgnoreCase(instance.duplicate)) {
					return;
				}
				copySelectionLines();
				pasteInstructions();
			});
			//Keybinds for shift up node
			addEventHandler(KeyEvent.KEY_RELEASED,e->{
				ConfKeybinds instance = ConfKeybinds.instance();
				if (instance.active && !e.isControlDown()) {
					return;
				}
				if (!e.getCode().getName().equalsIgnoreCase(instance.shift_up)) {
					return;
				}
				AbstractInsnNode node = getSelectionModel().getSelectedItem();
				if (node == null) {
					return;
				}
				if (node.getPrevious() != null) {
					InsnUtil.shiftUp(instructions, getSelectionModel().getSelectedItems());
					Bus.post(new ClassDirtyEvent(owner));
					refreshList();
					sortList();
				}
			});
			//Keybinds for shift down node
			addEventHandler(KeyEvent.KEY_RELEASED,e->{
				ConfKeybinds instance = ConfKeybinds.instance();
				if (instance.active && !e.isControlDown()) {
					return;
				}
				if (!e.getCode().getName().equalsIgnoreCase(instance.shift_down)) {
					return;
				}
				AbstractInsnNode node = getSelectionModel().getSelectedItem();
				if (node == null) {
					return;
				}
				if (node.getNext() != null) {
					InsnUtil.shiftDown(instructions, getSelectionModel().getSelectedItems());
					Bus.post(new ClassDirtyEvent(owner));
					refreshList();
					sortList();
				}
			});
			// Keybinds for instruction search
			addEventHandler(KeyEvent.KEY_RELEASED, e -> {
				ConfKeybinds keys = ConfKeybinds.instance();
				if (keys.active && !e.isControlDown()) {
					return;
				}
				if (!e.getCode().getName().equalsIgnoreCase(keys.find)) {
					return;
				}
				BorderPane bp = new BorderPane();
				Scene sc = JavaFX.scene(bp, 225, 28);
				Stage st = JavaFX.stage(sc, Lang.get("ui.bean.method.instructions.find.title"), true);
				TextField txtInput = new TextField();
				Button btnSearch = new Button();
				btnSearch.setText(Lang.get("ui.bean.method.instructions.find.confirm"));
				bp.setCenter(txtInput);
				bp.setRight(btnSearch);
				BorderPane.setAlignment(btnSearch, Pos.CENTER);
				Runnable r = () -> {
					// Close find window
					st.close();
					// Input validation
					String input = txtInput.getText();
					if (input == null || input.isEmpty()) {
						return;
					}
					// Update selection with matching results
					getSelectionModel().clearSelection();
					nodeLookup.entrySet().stream().filter(entry -> {
						String text = entry.getValue().getText().toLowerCase();
						return text.contains(input.toLowerCase());
					}).map(Map.Entry::getKey).forEach(abstractInsnNode -> {
						getSelectionModel().select(abstractInsnNode);
					});
					// Alert that nothing was found
					if (getSelectionModel().isEmpty()) {
						Toolkit.getDefaultToolkit().beep();
					}
				};
				btnSearch.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
					if (event.getButton() == MouseButton.PRIMARY) {
						r.run();
					}
				});
				st.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
					if (event.getCode() == KeyCode.ENTER) {
						r.run();
					}
				});
				st.show();
			});
			// Keybinds for copy/paste
			addEventHandler(KeyEvent.KEY_RELEASED, e -> {
				// Only continue of control is held
				ConfKeybinds keys = ConfKeybinds.instance();
				if (keys.active && !e.isControlDown()) {
					return;
				}
				String code = e.getCode().getName();
				if (code.equalsIgnoreCase(keys.copy)) {
					copySelectionLines();
				} else if (code.equalsIgnoreCase(keys.paste)) {
					pasteInstructions();
				}
			});
		}

		public void sortList() {
			// Why would we need to sort this list by index?
			// Because removing and re-insertion throws exceptions that
			// I don't know how to resolve.
			// So this is the "temporary" fix that probably isn't
			// "temporary" at all.
			getItems().sort((o1, o2) -> {
				int i1 = instructions.indexOf(o1);
				int i2 = instructions.indexOf(o2);
				return Integer.compare(i1, i2);
			});
		}

		private void copySelectionLines(){
			// Copy list... don't want to store the observable one
			List<AbstractInsnNode> clone = new ArrayList<>(getSelectionModel().getSelectedItems());
			String key = FormatFactory.insnsString(clone, method);
			Clipboard.setContent(key, clone);
		}

		private void pasteInstructions(){
			// Don't bother if recent copy-value isn't a list
			if (!Clipboard.isRecentType(List.class)) return;
			// Clone because ASM nodes are linked lists...
			// - Can't have those shared refs across multiple methods
			List<AbstractInsnNode> clone = ConfBlocks.clone(method.instructions, Clipboard.getRecent());
			if (clone == null) return;
			// Insert into list
			int index = getSelectionModel().getSelectedIndex();
			if (index == -1) {
				return;
			}
			if (index < getItems().size() - 1) {
				// Add after selection
				getItems().addAll(index + 1, clone);
			} else {
				// Add to end
				getItems().addAll(clone);
			}
		}

		private void onAdd(int start, List<? extends AbstractInsnNode> added) {
			// Create InsnList to add to existing list.
			InsnList insn = new InsnList();
			added.forEach(ain -> {
				createFormat(ain);
				insn.add(ain);
			});
			// start marks the beginning of the range of added opcodes.
			if (start == 0) {
				// insert to start of list
				instructions.insert(insn);
			} else {
				// get opcode[start -1] and add the opcodes after it
				// This puts them in the intended start location.
				AbstractInsnNode location = instructions.get(start - 1);
				instructions.insert(location, insn);
			}
			refreshList();
			Bus.post(new ClassDirtyEvent(owner));
		}

		private void onRemove(List<? extends AbstractInsnNode> removed) {
			// remove from lookup cache
			removed.forEach(ain -> nodeLookup.remove(ain));
			// remove from instructions linked list
			if (removed.size() > 1) {
				AbstractInsnNode insnStart = removed.get(0);
				AbstractInsnNode insnEnd = removed.get(removed.size() - 1);
				InsnUtil.link(method.instructions, insnStart, insnEnd);
			} else {
				instructions.remove(removed.get(0));
			}
			refreshList();
			Bus.post(new ClassDirtyEvent(owner));
		}

		private void onSelect(ObservableList<AbstractInsnNode> selected) {
			List<InsnHBox> list = new ArrayList<>();
			for (AbstractInsnNode ain : instructions.toArray()) {
				InsnHBox box = nodeLookup.get(ain);
				if (box != null) {
					list.add(box);
				}
			}
			list.forEach(cell -> {
				cell.getStyleClass().remove("op-selected");
			});
			selected.forEach(ain -> {
				updateReferenced(ain, list);
				mark(ain, list, "op-selected");
			});
		}

		/**
		 * Marks referenced opcodes with a custom style class.
		 *
		 * @param ain
		 * @param list
		 */
		private void updateReferenced(AbstractInsnNode ain, List<InsnHBox> list) {
			list.forEach(cell -> {
				cell.getStyleClass().remove("op-jumpdest");
				cell.getStyleClass().remove("op-jumpdest-fail");
				cell.getStyleClass().remove("op-jumpdest-reverse");
				cell.getStyleClass().remove("op-varmatch");
			});
			if (ain == null) {
				return;
			}
			switch (ain.getType()) {
			case AbstractInsnNode.JUMP_INSN:
				mark(ain.getNext(), list, "op-jumpdest-fail");
				mark(((JumpInsnNode) ain).label, list, "op-jumpdest");
				break;
			case AbstractInsnNode.LOOKUPSWITCH_INSN:
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
				mark(lsin.dflt, list, "op-jumpdest-fail");
				// TODO: Show associated keys to destinations
				lsin.labels.forEach(l -> mark(l, list, "op-jumpdest"));
				break;
			case AbstractInsnNode.TABLESWITCH_INSN:
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
				// TODO: Show associated keys to destinations
				mark(tsin.dflt, list, "op-jumpdest-fail");
				tsin.labels.forEach(l -> mark(l, list, "op-jumpdest"));
				break;
			case AbstractInsnNode.VAR_INSN: {
				int var = ((VarInsnNode) ain).var;
				// Show other opcodes that modify the variable
				for (AbstractInsnNode insn : getItems()) {
					if (insn.getType() == AbstractInsnNode.VAR_INSN && ((VarInsnNode) insn).var == var) {
						mark(insn, list, "op-varmatch");
					} else if (insn.getType() == AbstractInsnNode.IINC_INSN && ((IincInsnNode) insn).var == var) {
						mark(insn, list, "op-varmatch");
					}
				}
				break;
			}
			case AbstractInsnNode.IINC_INSN: {
				int var = ((IincInsnNode) ain).var;
				// Show other opcodes that modify the variable
				for (AbstractInsnNode insn : getItems()) {
					if (insn.getType() == AbstractInsnNode.VAR_INSN && ((VarInsnNode) insn).var == var) {
						mark(insn, list, "op-varmatch");
					} else if (insn.getType() == AbstractInsnNode.IINC_INSN && ((IincInsnNode) insn).var == var) {
						mark(insn, list, "op-varmatch");
					}
				}
				break;
			}
			case AbstractInsnNode.FIELD_INSN:
				FieldInsnNode fin = (FieldInsnNode) ain;
				// Show other opcodes that modify the field
				for (AbstractInsnNode insn : getItems()) {
					if (insn.getType() == AbstractInsnNode.FIELD_INSN) {
						FieldInsnNode other = ((FieldInsnNode) insn);
						if (fin.name.equals(other.name) && fin.desc.equals(other.desc) && fin.owner.equals(other.owner)) {
							mark(insn, list, "op-varmatch");
						}
					}
				}
				break;
			case AbstractInsnNode.LINE:
				LineNumberNode lin = (LineNumberNode) ain;
				mark(lin.start, list, "op-jumpdest");
				break;
			case AbstractInsnNode.LABEL:
				// reverse lookup
				for (AbstractInsnNode insn : getItems()) {
					if (insn.getType() == AbstractInsnNode.JUMP_INSN) {
						JumpInsnNode other = (JumpInsnNode) insn;
						if (other.label.equals(ain)) {
							mark(insn, list, "op-jumpdest-reverse");
						}
					} else if (insn.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
						LookupSwitchInsnNode other = (LookupSwitchInsnNode) insn;
						for (LabelNode ln : other.labels) {
							if (ln.equals(insn)) {
								mark(insn, list, "op-jumpdest-reverse");
							}
						}
					} else if (insn.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
						TableSwitchInsnNode other = (TableSwitchInsnNode) insn;
						for (LabelNode ln : other.labels) {
							if (ln.equals(insn)) {
								mark(insn, list, "op-jumpdest-reverse");
							}
						}
					} else if (insn.getType() == AbstractInsnNode.LINE) {
						LineNumberNode line = (LineNumberNode) insn;
						if (line.start.equals(ain)) {
							mark(insn, list, "op-jumpdest-reverse");
						}
					}
				}
				break;
			}

		}

		/**
		 * Applies the given class to the cell at the index matching the
		 * opcode's index in the items property.
		 *
		 * @param ain
		 *            Opcode to mark.
		 * @param list
		 *            List of cells.
		 * @param clazz
		 *            Class to apply to cell.
		 */
		private void mark(AbstractInsnNode ain, List<InsnHBox> list, String clazz) {
			int index = getItems().indexOf(ain);
			if (index >= 0 && index < list.size()) {
				// this automatically refreshes the node too, so the style
				// should be instantly applied
				list.get(index).getStyleClass().add(clazz);
			} else {
				Logging.error("Could not locate: " + ain + " @" + index + " with " + clazz);
			}
		}

		/**
		 * Sets the InsnList size through reflection. This is done to ensure
		 * cuts done via reflection are accounted for in the InsnList structure.
		 *
		 * @param size
		 *            New method instructions size.
		 */
		private void setSize(int size) {
			try {
				Field f = InsnList.class.getDeclaredField("size");
				f.setAccessible(true);
				f.setInt(method.instructions, size);
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		/**
		 * Update CSS and fire update for item that caused failiure, if there
		 * was a failure at all.
		 *
		 * @param verif
		 *            Verification results.
		 */
		public void updateVerification(VerifyResults verif) {
			this.verif = verif;
			AbstractInsnNode cause = verif.getCause();
			// Ensure there are values in nodeLookup.
			if (nodeLookup.size() == 0) {
				refreshList();
			}
			Threads.runFx(() -> {
				// create list of HBoxes
				List<InsnHBox> list = new ArrayList<>();
				for (AbstractInsnNode ain : instructions.toArray()) {
					InsnHBox box = nodeLookup.get(ain);
					if (box != null) {
						list.add(box);
					}
				}
				if (cause != null && getItems().contains(cause)) {
					// add failure style
					// mark(cause, list, "op-verif-fail");
					refreshItem(cause);
				} else if (cause == null) {
					// remove failure style
					list.forEach(cell -> {
						cell.getStyleClass().remove("op-verif-fail");
					});
				}
			});
		}

		/**
		 * Recreates the opcode representations.
		 */
		public void refreshList() {
			Threads.runFx(() -> {
				// regenerate opcode representations
				for (AbstractInsnNode ain : instructions.toArray()) {
					createFormat(ain);
				}
				// update visible cells
				refresh();
			});
		}

		/**
		 * Recreate the opcode representation for the given opcode.
		 *
		 * @param ain
		 *            Opcode to refresh.
		 */
		public void refreshItem(AbstractInsnNode ain) {
			Threads.runFx(() -> {
				createFormat(ain);
				refresh();
			});
		}

		/**
		 * Create an HBox representation of the given opcode, put it into the
		 * {@link #nodeLookup} map.
		 *
		 * @param ain
		 */
		private void createFormat(AbstractInsnNode ain) {
			nodeLookup.put(ain, FormatFactory.insnNode(ain, method));
		}

		public Map<AbstractInsnNode, InsnHBox> getNodeLookup() {
			return nodeLookup;
		}

		public VerifyResults getVerificationResults() {
			return verif;
		}

		public ClassNode getClassNode() {
			return InsnListEditor.this.getClassNode();
		}

		public MethodNode getMethod() {
			return InsnListEditor.this.getMethod();
		}

		public InsnList getInsns() {
			return instructions;
		}

		public InsnListEditor outer() {
			return InsnListEditor.this;
		}
	}
}
