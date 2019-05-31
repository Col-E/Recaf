package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import me.coley.recaf.ui.component.ReflectivePropertySheet.ReflectiveItem;
import me.coley.recaf.ui.component.editor.LanguageEditor;
import me.coley.recaf.ui.component.editor.StyleEditor;

public class ReflectiveConfigItem extends ReflectiveItem {
	public ReflectiveConfigItem(Object owner, Field field, String categoryKey, String translationKey) {
		super(owner, field, categoryKey, translationKey);
	}

	@Override
	protected Class<?> getEditorType() {
		String name = getField().getName();
		if (name.equals("language")) {
			return LanguageEditor.class;
		} else if (name.equals("style")) {
			return StyleEditor.class;
		}
		return super.getEditorType();
	}
}
