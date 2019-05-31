package me.coley.recaf.ui.component;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.function.Consumer;

import org.controlsfx.control.PopOver;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.Lang;

/**
 * Control for selecting labels in a method.
 * 
 * @author Matt
 */
public class LabelButton extends Button {
	/**
	 * MethodNode to pull labels from.
	 */
	private final MethodNode method;
	/**
	 * Label currently displayed.
	 */
	private LabelNode label;
	/**
	 * Popover to show list in.
	 */
	private PopOver popover;
	/**
	 * Task called when label is updated in {@link #setLabel(LabelNode)}.
	 */
	private Consumer<LabelNode> updateTask;

	public LabelButton(String initial, MethodNode method) {
		this.method = method;
		setText(initial);
		setupPopover();
		getStyleClass().add("lblbutton");
	}

	public LabelButton(LabelNode label, MethodNode method) {
		this.method = method;
		setLabel(label);
		setupPopover();
		getStyleClass().add("lblbutton");
	}

	/**
	 * @return Selected label.
	 */
	public LabelNode getLabel() {
		return label;
	}

	/**
	 * Set label.
	 * 
	 * @param label
	 */
	public void setLabel(LabelNode label) {
		this.label = label;
		setText(null);
		if (label == null) {
			setGraphic(FormatFactory.name(Lang.get("ui.bean.opcode.label.nullvalue")));
		} else {
			setGraphic(FormatFactory.insnNode(label, method));
		}
		if (updateTask != null) {
			updateTask.accept(label);
		}
	}

	/**
	 * Set label to {@code null}.
	 */
	public void clear() {
		setLabel(null);
	}

	/**
	 * Set action for when label is updated.
	 * 
	 * @param updateTask
	 */
	public void setUpdateTask(Consumer<LabelNode> updateTask) {
		this.updateTask = updateTask;
	}

	private void setupPopover() {
		ListView<LabelNode> view = new ListView<>();
		view.getSelectionModel().selectedItemProperty().addListener((ov, old, current) -> {
			setLabel(current);
		});
		// label display
		view.setCellFactory(cell -> new ListCell<LabelNode>() {
			@Override
			protected void updateItem(LabelNode node, boolean empty) {
				super.updateItem(node, empty);
				if (empty || node == null) {
					setGraphic(null);
				} else {
					setGraphic(FormatFactory.insnNode(node, method));
				}
			}
		});
		// populate label view
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.LABEL) {
				view.getItems().add((LabelNode) ain);
			}
		}
		popover = new PopOver(view);
		popover.setAnimated(false);
		popover.setTitle(Lang.get("ui.bean.method.label.name"));
		setOnAction(e -> {
			Point m = MouseInfo.getPointerInfo().getLocation();
			popover.show(this);
			popover.setX(m.getX() - 5);
			popover.setY(m.getY() - 35);
		});
	}
}
