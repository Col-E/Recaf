package me.coley.recaf.ui.component.editor;

import com.github.javaparser.utils.StringEscapeUtils;
import javafx.scene.Node;
import me.coley.recaf.ui.component.ReflectivePropertySheet.CustomEditor;
import me.coley.recaf.ui.component.PropertyTextField;
import org.controlsfx.control.PropertySheet.Item;

import java.lang.reflect.Field;

/**
 * Editor for strings.
 * 
 * @author Matt
 */
public class StringEditor extends CustomEditor<String> {
	public StringEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		return new PropertyTextField<String>(StringEditor.this) {
			@Override
			protected String convertTextToValue(String text) {
				return StringEscapeUtils.unescapeJava(text);
			}

			@Override
			protected String convertValueToText(String value) {
				return StringEscapeUtils.escapeJava(value);
			}
		};
	}
}