package me.coley.recaf.ui.component;

import org.controlsfx.control.PropertySheet;
import org.objectweb.asm.tree.*;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class InsnEditor extends BorderPane {
	public InsnEditor(InsnListEditor list, AbstractInsnNode ain) {
		PropertySheet propertySheet = new ReflectiveOpcodeSheet(list, ain);
		VBox.setVgrow(propertySheet, Priority.ALWAYS);
		setCenter(propertySheet);
	}
}
