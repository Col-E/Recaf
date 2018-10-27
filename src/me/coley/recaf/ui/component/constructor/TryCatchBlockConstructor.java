package me.coley.recaf.ui.component.constructor;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.Lang;

/**
 * Try-Catch representation via label-buttons and textfield for the caught type.
 * 
 * @author Matt
 */
public class TryCatchBlockConstructor extends BorderPane implements Constructor<TryCatchBlockNode> {
	private final ListView<TryCatchBlockNode> view;
	private final LabelButton start, end, handler;
	private final TextField type;

	public TryCatchBlockConstructor(MethodNode method, ListView<TryCatchBlockNode> view) {
		this.view = view;
		HBox menuPane = new HBox();
		start = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.start"), method);
		end = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.end"), method);
		handler = new LabelButton(Lang.get("ui.bean.method.trycatchblocks.handler"), method);
		type = new TextField();
		start.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.start.tooltip")));
		end.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.end.tooltip")));
		handler.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.handler.tooltip")));
		type.setTooltip(new Tooltip(Lang.get("ui.bean.method.trycatchblocks.type.tooltip")));
		type.setOnAction(e -> finish());
		menuPane.getChildren().addAll(start, end, handler, type);
		setCenter(menuPane);
	}

	public TryCatchBlockConstructor(MethodNode method, ListView<TryCatchBlockNode> view, TryCatchBlockNode node) {
		this(method, view);
		start.setLabel(node.start);
		end.setLabel(node.end);
		handler.setLabel(node.handler);
		start.setUpdateTask(l -> node.start = l);
		end.setUpdateTask(l -> node.end = l);
		handler.setUpdateTask(l -> node.handler = l);
		type.setText(node.type);
		type.setOnKeyReleased(e -> {
			node.type = getTypeText();
		});
		type.setOnAction(e -> {});
	}

	@Override
	public TryCatchBlockNode get() {
		// ASM wants type to be null to catch anything,
		return new TryCatchBlockNode(start.getLabel(), end.getLabel(), handler.getLabel(), getTypeText());
	}

	@Override
	public void reset() {
		start.clear();
		end.clear();
		handler.clear();
		type.setText("");
	}

	@Override
	public void finish() {
		view.getItems().add(get());
		reset();
	}

	private String getTypeText() {
		String text = type.getText();
		if (text.isEmpty()) {
			text = null;
		}
		return text;
	}

}