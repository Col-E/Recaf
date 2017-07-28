package me.coley.recaf.ui.component.action;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class ActionTextField extends JTextField {
	public ActionTextField(Object content, Consumer<String> textAction) {
		this(content.toString(), textAction);
	}

	public ActionTextField(String content, Consumer<String> textAction) {
		super(resolve(content));
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				textAction.accept(getText());
			}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {}
		});
	}

	private static String resolve(String content) {
		if (content == null) {
			return "";
		}
		return content;
	}
}
