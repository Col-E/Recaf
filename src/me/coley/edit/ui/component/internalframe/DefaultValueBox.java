package me.coley.edit.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.JInternalFrame;

import me.coley.edit.ui.component.LabeledComponent;
import me.coley.edit.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class DefaultValueBox extends JInternalFrame {
	public DefaultValueBox(String fieldName, Object init, Consumer<String> action) throws Exception {
		super("Default Value: " + fieldName);
		int padding = 12;
		setMaximumSize(new Dimension(300, 300));
		setResizable(true);
		setIconifiable(true);
		setClosable(true);
		setVisible(true);

		setLayout(new BorderLayout());
		String value = init == null ? "" : init.toString();
		add(new LabeledComponent("Default Value:", new ActionTextField(value, action)), BorderLayout.CENTER);

		pack();
		setSize(getWidth() + padding, getHeight() + padding);
	}
}
