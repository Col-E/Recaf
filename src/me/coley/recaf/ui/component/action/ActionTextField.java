package me.coley.recaf.ui.component.action;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;
import javax.swing.JTextField;

import me.coley.recaf.ui.FontUtil;

@SuppressWarnings("serial")
public class ActionTextField extends JTextField {
	public ActionTextField(Object content, Consumer<String> textAction) {
		this(content.toString(), textAction);
	}

	public ActionTextField(String content, Consumer<String> textAction) {
		super(resolve(content));
		setFont(FontUtil.monospace);
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				textAction.accept(getText());
			}
		});
	}

	private static String resolve(String content) {
		if (content == null) {
			return "";
		}
		return content;
	}
}
