package me.coley.recaf.ui.component;

import javafx.scene.control.TextField;
import org.controlsfx.property.editor.PropertyEditor;

/**
 * ControlsFX PropertyEditor based text field.
 * 
 * @author Matt
 *
 * @param <T> Type of represented value.
 */
public abstract class PropertyTextField<T> extends TextField {
	public PropertyTextField(PropertyEditor<T> editor) {
		setText(editor);
		textProperty().addListener((ob, old, curr) -> {
			T converted = convertTextToValue(curr);
			if (converted != null) {
				editor.setValue(converted);
			}
		});
	}

	/**
	 * Set text-field text from property-editor's value.
	 * 
	 * @param editor
	 */
	protected void setText(PropertyEditor<T> editor) {
		this.setText(convertValueToText(editor.getValue()));
	}

	/**
	 * @param text
	 * @return Value from text.
	 */
	protected abstract T convertTextToValue(String text);

	/**
	 * @param value
	 * @return Text from value.
	 */
	protected abstract String convertValueToText(T value);
}
