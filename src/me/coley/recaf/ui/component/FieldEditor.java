package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import javafx.scene.layout.*;
import me.coley.recaf.event.*;
import me.coley.recaf.util.*;
import org.controlsfx.control.*;
import org.objectweb.asm.tree.*;

/**
 * Editor pane for some FieldNode.
 * 
 * @author Matt
 */
public class FieldEditor extends BorderPane {
	public FieldEditor(FieldOpenEvent event) {
		PropertySheet propertySheet = new ReflectivePropertySheet(event.getNode()) {
			@Override
			protected void setupItems(Object instance) {
				for (Field field : Reflect.fields(instance.getClass())) {
					String name = field.getName();
					// Skip attrs
					if (name.equals("attrs")) {
						continue;
					}
					String group = "ui.bean.field";
					field.setAccessible(true);
					// Setup item & add to list
					getItems().add(new ReflectiveFieldNodeItem(event.getOwner(), (FieldNode) instance, field, group, field
							.getName().toLowerCase()));
				}
			}
		};
		propertySheet.setModeSwitcherVisible(false);
		propertySheet.setSearchBoxVisible(false);
		VBox.setVgrow(propertySheet, Priority.ALWAYS);
		setCenter(propertySheet);
	}
}