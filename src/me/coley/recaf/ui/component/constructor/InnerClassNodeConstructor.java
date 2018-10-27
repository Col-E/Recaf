package me.coley.recaf.ui.component.constructor;

import org.objectweb.asm.tree.InnerClassNode;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.ui.component.AccessButton;
import me.coley.recaf.util.Lang;

public class InnerClassNodeConstructor extends HBox implements Constructor<InnerClassNode> {
	TextField name = new TextField();
	TextField innerName = new TextField();
	TextField outerName = new TextField();
	AccessButton access = new AccessButton(AccessFlag.Type.INNER_CLASS);

	public InnerClassNodeConstructor() {
		name.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.name.tooltip")));
		innerName.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.inner.tooltip")));
		outerName.setTooltip(new Tooltip(Lang.get("ui.bean.class.innerclasses.outer.tooltip")));
		name.setOnAction((e) -> innerName.selectAll());
		innerName.setOnAction((e) -> outerName.selectAll());
		// outerName.setOnAction((e) -> add(access, name, innerName,
		// outerName, view));
		getChildren().addAll(access, name, innerName, outerName);
	}

	@Override
	public InnerClassNode get() {
		return new InnerClassNode(name.getText(), outerName.getText(), innerName.getText(), access.getAccess());
	}

	@Override
	public void reset() {
		access.setAccess(0);
		name.textProperty().setValue("");
		innerName.textProperty().setValue("");
		outerName.textProperty().setValue("");
	}
}