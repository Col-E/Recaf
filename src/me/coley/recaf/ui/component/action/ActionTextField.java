package me.coley.recaf.ui.component.action;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.JTextField;

import me.coley.recaf.ui.FontUtil;

/**
 * Button with associated runnable action called when the text is updated.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class ActionTextField extends JTextField {
	public ActionTextField(Object content, Consumer<String> textAction) {
		this(content.toString(), textAction);
	}

	public ActionTextField(String content, Consumer<String> textAction) {
		super(ensureNonNull(content));
		setFont(FontUtil.monospace);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				textAction.accept(getText());
			}
		});
	}

	/**
	 * Used to ensure the text-field's initial value is not null.
	 * 
	 * @param content
	 *            Value to set.
	 * @return content, or an empty string if content is null.
	 */
	private static String ensureNonNull(String content) {
		if (content == null) {
			return "";
		}
		return content;
	}
}
