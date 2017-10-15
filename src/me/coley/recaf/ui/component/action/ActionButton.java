package me.coley.recaf.ui.component.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * Button with associated runnable action called when the button is pressed.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class ActionButton extends JButton {
	public ActionButton(String text, Runnable r) {
		super(text);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				r.run();
			}
		});
	}
}
