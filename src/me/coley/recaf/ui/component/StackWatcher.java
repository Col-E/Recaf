package me.coley.recaf.ui.component;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.InsnListEditor.OpcodeList;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

// TODO: When user updates bytecode, regen frames
public class StackWatcher extends Stage implements ListChangeListener<AbstractInsnNode>, ChangeListener<Number> {
	private final ClassNode owner;
	private final MethodNode method;
	private final OpcodeList list;
	private final TableView<SourceValue> stack = new TableView<>();
	private List<Frame<SourceValue>> frames;

	public StackWatcher(ClassNode owner, MethodNode method, OpcodeList list) {
		this.owner = owner;
		this.method = method;
		this.list = list;
		setupUI();
	}

	@SuppressWarnings("unchecked")
	private void setupUI() {
		// setup table
		// TODO: second column for "ui.edit.method.stackhelper.colvalue"
		TableColumn<SourceValue, Node> colInterp = new TableColumn<>(Lang.get("ui.edit.method.stackhelper.colopcode"));
		colInterp.setCellValueFactory(cell -> {
			SourceValue v = cell.getValue();
			Node box;
			if (v != null) {
				box = FormatFactory.opcodeSet(v.insns, method);
			} else {
				box = new TextHBox();
				((TextHBox) box).append("?");
			}
			return JavaFX.observable(box);
		});
		colInterp.setCellFactory(cell -> new TableCell<SourceValue, Node>() {
			@Override
			protected void updateItem(Node box, boolean empty) {
				super.updateItem(box, empty);
				if (empty || box == null) {
					setGraphic(null);
				} else {
					setGraphic(box);
				}
			}
		});
		colInterp.setMinWidth(300);
		stack.getColumns().addAll(colInterp);
		// create scene
		Scene scene = JavaFX.scene(stack, 400, 300);
		setScene(scene);
	}

	public void update() {
		try {
			Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
			frames = Arrays.asList(analyzer.analyze(owner.name, method));
		} catch (Exception e) {

		}
	}

	public void select(int selected) {
		// offset by one so that the selected index is the change that the
		// selected opcode does
		int index = selected + 1;
		stack.getItems().clear();
		if (frames != null && index >= 0 && index < frames.size() - 1) {
			Frame<SourceValue> frame = frames.get(index);
			for (int s = 0; s < frame.getStackSize(); s++) {
				SourceValue val;
				try {
					val = frame.getStack(s);
				} catch (IndexOutOfBoundsException e) {
					val = new SourceValue(0);
				}
				stack.getItems().add(val);
			}
		}
		stack.refresh();
	}

	/**
	 * Listener for selection changes. Update UI.
	 */
	@Override
	public void changed(ObservableValue<? extends Number> ob, Number last, Number current) {
		select(current.intValue());
	}

	/**
	 * Listener for content changes. Regenerate stack-frames.
	 */
	@Override
	public void onChanged(Change<? extends AbstractInsnNode> change) {
		update();
	}
}
