package me.coley.recaf.ui.component.constructor;

import org.objectweb.asm.tree.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Parse;

/**
 * Local variable representation via a bunch of inputs.
 * 
 * @author Matt
 */
public class LocalVariableNodeConstructor extends BorderPane implements Constructor<LocalVariableNode> {
	private final ListView<LocalVariableNode> view;
	private final TextField name, desc, signature, index;
	private final LabelButton start, end;
	private final MethodNode method;

	public LocalVariableNodeConstructor(MethodNode method, ListView<LocalVariableNode> view) {
		this.view = view;
		this.method = method;
		HBox menuPane = new HBox();
		index = new TextField();
		name = new TextField();
		desc = new TextField();
		signature = new TextField();
		start = new LabelButton(Lang.get("ui.bean.method.localvariable.start"), method);
		end = new LabelButton(Lang.get("ui.bean.method.localvariable.end"), method);
		index.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.index.tooltip")));
		name.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.name.tooltip")));
		desc.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariables.desc.tooltip")));
		signature.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariables.signature.tooltip")));
		index.setOnAction(e -> name.requestFocus());
		name.setOnAction(e -> desc.requestFocus());
		desc.setOnAction(e -> signature.requestFocus());
		signature.setOnAction(e -> signature.requestFocus());
		// numeric-only textfield.
		index.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					index.setText(newValue.replaceAll("[^\\d]", ""));
				}
				if (newValue.isEmpty() || index.getText().isEmpty()) {
					index.setText("0");
				}
			}
		});
		menuPane.getChildren().addAll(index, name, desc, signature, start, end);
		setCenter(menuPane);
	}

	public LocalVariableNodeConstructor(MethodNode method, ListView<LocalVariableNode> view, LocalVariableNode variable) {
		this(method, view);
		start.setLabel(variable.start);
		end.setLabel(variable.end);
		index.setText(String.valueOf(variable.index));
		name.setText(variable.name);
		desc.setText(variable.desc);
		signature.setText(variable.signature);
		start.setUpdateTask(l -> variable.start = l);
		end.setUpdateTask(l -> variable.end = l);
		index.setOnKeyReleased(e -> {
			if (Parse.isInt(index.getText())) {
				variable.index = Integer.parseInt(index.getText());
			}
		});
		name.setOnKeyReleased(e -> {
			variable.name = name.getText();
		});
		desc.setOnKeyReleased(e -> {
			variable.desc = desc.getText();
		});
		signature.setOnKeyReleased(e -> {
			variable.signature = getSignature();
		});
	}

	@Override
	public LocalVariableNode get() {
		int i = Integer.parseInt(index.getText());
		return new LocalVariableNode(name.getText(), desc.getText(), getSignature(), start.getLabel(), end.getLabel(), i);
	}

	@Override
	public void reset() {
		index.setText(String.valueOf(method.localVariables.size()));
		name.setText("");
		desc.setText("");
		signature.setText("");
		start.setLabel(null);
		end.setLabel(null);
	}

	@Override
	public void finish() {
		view.getItems().add(get());
	}

	private String getSignature() {
		String text = signature.getText();
		if (text.isEmpty()) {
			text = null;
		}
		return text;
	}
}