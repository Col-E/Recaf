package me.coley.recaf.ui.component.editor;

import java.util.Arrays;
import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.Opcodes;

import javafx.scene.Node;
import me.coley.recaf.ui.component.ReflectiveCombo;
import me.coley.recaf.ui.component.ReflectivePropertySheet.CustomEditor;

/**
 * Editor for Handle tags.
 * 
 * @author Matt
 */
public class TagEditor extends CustomEditor<Integer> {
	public TagEditor(Item item) {
		super(item);
	}

	@Override
	public Node getEditor() {
		//@formatter:off
		List<Integer> opts = Arrays.asList(
			Opcodes.H_GETFIELD,
			Opcodes.H_GETSTATIC, 
			Opcodes.H_PUTFIELD,
			Opcodes.H_PUTSTATIC, 
			Opcodes.H_INVOKEINTERFACE, 
			Opcodes.H_INVOKESPECIAL,
			Opcodes.H_INVOKESTATIC,
			Opcodes.H_INVOKEVIRTUAL);
		List<String> optStrs = Arrays.asList(
			"H_GETFIELD", 
			"H_GETSTATIC", 
			"H_PUTFIELD", 
			"H_PUTSTATIC",
			"H_INVOKEINTERFACE", 
			"H_INVOKESPECIAL", 
			"H_INVOKESTATIC", 
			"H_INVOKEVIRTUAL");
		return new ReflectiveCombo<>(item.getOwner(), item.getField(), opts, optStrs);
		//@formatter:on
	}
}