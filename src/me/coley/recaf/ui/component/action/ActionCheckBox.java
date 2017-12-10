package me.coley.recaf.ui.component.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.JCheckBox;

/**
 * CheckBox with associated runnable action called when the box is changed.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class ActionCheckBox extends JCheckBox {
	public ActionCheckBox(String text, boolean defaultValue, Consumer<Boolean> r) {
		super(text, defaultValue);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				r.accept(isSelected());
			}
		});
	}
}