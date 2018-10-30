package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import javafx.scene.layout.*;
import me.coley.recaf.event.*;
import me.coley.recaf.util.*;
import org.controlsfx.control.*;
import org.objectweb.asm.tree.*;

/**
 * Editor pane for some MethodNode.
 * 
 * @author Matt
 */
public class MethodEditor extends BorderPane {
	public MethodEditor(MethodOpenEvent event) {
		PropertySheet propertySheet = new ReflectivePropertySheet(event.getMethod()) {
			@Override
			protected void setupItems(Object instance) {
				for (Field field : Reflect.fields(instance.getClass())) {
					String name = field.getName();
					// Skip attrs
					if (name.equals("attrs") || name.equals("visited")) {
						continue;
					}
					// Stack info is recalculated on-exit, so this is
					// 'useless' unless you need to manually insert some
					// additional values yourself. The verifier will
					// relies on this information, but since it is only
					// calculated on-export we need to be able to edit
					// it.
					/*
					 * if (ConfASM.instance().ignoreMaxs() &&
					 * (name.contains("max"))) { continue; }
					 */
					String group = "ui.bean.method";
					field.setAccessible(true);
					// TODO: Further annotation support.
					if (field.getType().isArray() || name.contains("LocalVariableAnnotations") || name.contains(
							"annotationDefault")) {
						continue;
					}
					// Setup item & add to list
					getItems().add(new ReflectiveMethodNodeItem(event.getOwner(), (MethodNode) instance, field, group, name
							.toLowerCase()));
				}
			}
		};
		propertySheet.setModeSwitcherVisible(false);
		propertySheet.setSearchBoxVisible(false);
		VBox.setVgrow(propertySheet, Priority.ALWAYS);
		setCenter(propertySheet);
	}
}