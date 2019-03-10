package me.coley.recaf.ui.component.editor;

import java.lang.reflect.Field;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.*;
import javafx.scene.Node;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

/**
 * Editor for Indy handles.
 * 
 * @author Matt
 */
public class HandleEditor extends StagedCustomEditor<Handle> {
	public HandleEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		return new ActionButton(Lang.get("misc.edit"), () -> open(this));

	}

	private void open(HandleEditor handleEditor) {
		if (staged()) {
			return;
		}
		Handle h = (Handle) item.getValue();
		ReflectivePropertySheet ps = new ReflectivePropertySheet(h) {
			protected void setupItems(Object instance) {
				for (Field field : Reflect.fields(instance.getClass())) {
					field.setAccessible(true);
					String name = field.getName();
					String group = "ui.bean.handle";
					// Setup item & add to list
					if (name.equals("tag")) {
						getItems().add(new ReflectiveItem(instance, field, group, name) {
							@Override
							protected Class<?> getEditorType() {
								return TagEditor.class;
							}
						});
					} else {
						getItems().add(new ReflectiveItem(instance, field, group, name));
					}
				}
			}
		};
		ps.setModeSwitcherVisible(false);
		ps.setSearchBoxVisible(false);
		setStage("ui.bean.opcode.bsm.name", ps, 300, 300);
	}
}