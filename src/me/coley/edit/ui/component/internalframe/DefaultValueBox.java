package me.coley.edit.ui.component.internalframe;

import java.awt.BorderLayout;
import java.util.function.Consumer;

import me.coley.edit.ui.component.LabeledComponent;
import me.coley.edit.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class DefaultValueBox extends BasicFrame {
	public DefaultValueBox(String fieldName, Object init, Consumer<String> action) throws Exception {
		super("Default Value: " + fieldName);
		setLayout(new BorderLayout());
		String value = init == null ? "" : init.toString();
		add(new LabeledComponent("Default Value:", new ActionTextField(value, action)), BorderLayout.CENTER);
		setVisible(true);
	}
}
