package me.coley.recaf.ui.component.constructor;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.ui.component.AccessButton;
import me.coley.recaf.ui.component.constructor.Constructor;
import me.coley.recaf.util.Lang;

/**
 * Parameter representation via a textfield and access-button.
 * 
 * @author Matt
 */
public class ParameterNodeConstructor extends BorderPane implements Constructor<ParameterNode> {
	private final ListView<ParameterNode> view;
	private final TextField name;
	private final AccessButton access;

	public ParameterNodeConstructor(MethodNode method, ListView<ParameterNode> view) {
		this.view = view;
		HBox menuPane = new HBox();
		access = new AccessButton(AccessFlag.Type.PARAM);
		name = new TextField();
		name.setTooltip(new Tooltip(Lang.get("ui.bean.method.localvariable.name.tooltip")));
		menuPane.getChildren().addAll(access, name);
		setCenter(menuPane);
	}

	public ParameterNodeConstructor(MethodNode method, ListView<ParameterNode> view, ParameterNode variable) {
		this(method, view);
		access.setAccess(variable.access);
		access.setUpdateTask(a -> variable.access = a);
		name.setText(variable.name);
		name.setOnKeyReleased(e -> variable.name = name.getText());
	}

	@Override
	public ParameterNode get() {
		return new ParameterNode(name.getText(), access.getAccess());
	}

	@Override
	public void reset() {
		access.setAccess(0);
		name.setText("");
	}

	@Override
	public void finish() {
		view.getItems().add(get());
	}
}