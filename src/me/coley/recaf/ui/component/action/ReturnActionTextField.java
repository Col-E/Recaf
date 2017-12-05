package me.coley.recaf.ui.component.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import javax.swing.JTextField;

import me.coley.recaf.ui.FontUtil;

/**
 * Text field with associated runnable action called when enter is pressed.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class ReturnActionTextField extends JTextField {
	public ReturnActionTextField(Object content, Consumer<String> textAction) {
		this(content.toString(), textAction);
	}

	public ReturnActionTextField(String content, Consumer<String> textAction) {
		super(ensureNonNull(content));
		setFont(FontUtil.monospace);
		// TIL that hitting enter calls a textfield's action listeners.
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
