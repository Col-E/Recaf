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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.util.Callback;
import me.coley.recaf.bytecode.analysis.LinkedInterpreter;
import me.coley.recaf.bytecode.analysis.SourceAnalyzer;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

public class StackWatcher extends Stage implements ListChangeListener<AbstractInsnNode>, ChangeListener<Number> {
	private final ClassNode owner;
	private final MethodNode method;
	private final TableView<SourceValue> stack = new TableView<>();
	private final TableView<SourceValue> local = new TableView<>();
	private List<Frame<SourceValue>> frames;

	public StackWatcher(ClassNode owner, MethodNode method) {
		this.owner = owner;
		this.method = method;
		setupUI();
	}

	private void setupUI() {
		setTitle(Lang.get("ui.edit.method.stackhelper.title") + method.name + method.desc);
		// setup table
		// TODO: second column for "ui.edit.method.stackhelper.colstackvalue"
		Callback<CellDataFeatures<SourceValue, Node>, ObservableValue<Node>> val2Node = val -> {
			SourceValue v = val.getValue();
			Node box;
			if (v != null) {
				box = FormatFactory.insnsNode(v.insns, method);
			} else {
				box = new TextHBox();
				((TextHBox) box).append("?");
			}
			return JavaFX.observable(box);
		};
		Callback<TableColumn<SourceValue, Node>, TableCell<SourceValue,Node>> col2Cell = cell -> new TableCell<SourceValue, Node>() {
			@Override
			protected void updateItem(Node box, boolean empty) {
				super.updateItem(box, empty);
				if (empty || box == null) {
					setGraphic(null);
				} else {
					setGraphic(box);
				}
			}
		};
		Callback<TableColumn<SourceValue, Node>, TableCell<SourceValue,Node>> col2Row = cell -> new TableCell<SourceValue, Node>() {
			@Override
			protected void updateItem(Node box, boolean empty) {
				super.updateItem(box, empty);
				if (empty || box == null) {
					setText(null);
				} else if (getTableRow() != null){
					setText(String.valueOf(getTableRow().getIndex()));
				}
			}
		};
		TableColumn<SourceValue, Node> colIndex = new TableColumn<>(Lang.get("ui.edit.method.stackhelper.colindex"));
		colIndex.setCellValueFactory(val2Node);
		colIndex.setCellFactory(col2Row);
		colIndex.setMinWidth(20);
		TableColumn<SourceValue, Node> colStackSrc = new TableColumn<>(Lang.get("ui.edit.method.stackhelper.colstackopcode"));
		colStackSrc.setCellValueFactory(val2Node);
		colStackSrc.setCellFactory(col2Cell);
		colStackSrc.setMinWidth(300);
		TableColumn<SourceValue, Node> colLocalSrc = new TableColumn<>(Lang.get("ui.edit.method.stackhelper.collocalopcode"));
		colLocalSrc.setCellValueFactory(val2Node);
		colLocalSrc.setCellFactory(col2Cell);
		colLocalSrc.setMinWidth(300);
		stack.getColumns().add(colIndex);
		stack.getColumns().add(colStackSrc);
		local.getColumns().add(colIndex);
		local.getColumns().add(colLocalSrc);
		// create scene
		Scene scene = JavaFX.scene(new SplitPane(stack, local), 700, 400);
		setScene(scene);
	}

	public void update() {
		try {
			SourceAnalyzer analyzer = new SourceAnalyzer(new LinkedInterpreter());
			frames = Arrays.asList(analyzer.analyze(owner.name, method));
		} catch (Exception e) {}
	}

	public void select(int selected) {
		// offset by one so that the selected index is the change that the
		// selected opcode does
		int index = selected + 1;
		stack.getItems().clear();
		local.getItems().clear();
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
			for (int l = 0; l < frame.getLocals(); l++) {
				SourceValue val;
				try {
					val = frame.getLocal(l);
				} catch (IndexOutOfBoundsException e) {
					// TODO: Allow for local variables to have 'default' values
					//
					// Primary example would be 'this' and method arguments.
					val = new SourceValue(0);
				}
				local.getItems().add(val);
			}
		}
		stack.refresh();
		local.refresh();
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
