package me.coley.recaf.ui.component.action;

import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * CheckBox with associated runnable action called when the box is changed.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class ActionCheckBox extends JCheckBox {
	public ActionCheckBox(String text, boolean defaultValue, Consumer<Boolean> r) {
		super(text, defaultValue);
		addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				r.accept(isSelected());
			}
		});
	}
}
