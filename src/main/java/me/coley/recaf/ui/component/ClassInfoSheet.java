package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import javafx.scene.layout.*;
import me.coley.recaf.util.*;
import org.controlsfx.control.*;
import org.objectweb.asm.tree.*;

public class ClassInfoSheet extends BorderPane {
	public ClassInfoSheet(ClassNode node) {
		PropertySheet propertySheet = new ReflectivePropertySheet(node) {
			@Override
			protected void setupItems(Object instance) {
				for (Field field : Reflect.fields(instance.getClass())) {
					// Skip fields/methods/attrs
					String name = field.getName();
					// skip experimental values
					if (name.toLowerCase().contains("exper")) {
						continue;
					}
					// skip, we have separate tabs for these
					if (name.equals("fields") || name.equals("methods")) {
						continue;
					}
					// TODO: Implement nesting
					// - increase 0.75 -> 0.804 for logging panel
					// split size to fit this.
					if (name.contains("nest")) {
						continue;
					}
					// TODO: Implement module
					if (name.equals("module")) {
						continue;
					}
					// TODO: figure out kinds of data allowed in
					// attrs
					if (name.equals("attrs")) {
						continue;
					}
					// Set accessible and check determine if field
					// type is simply represented.
					field.setAccessible(true);
					// Setup item & add to list
					getItems().add(new ReflectiveClassNodeItem(instance, field, "ui.bean.class", name.toLowerCase()));
				}
			}
		};
		propertySheet.setModeSwitcherVisible(false);
		propertySheet.setSearchBoxVisible(false);
		propertySheet.getItems().add(new DecompileItem(node));
		VBox.setVgrow(propertySheet, Priority.ALWAYS);
		setCenter(propertySheet);
	}
}