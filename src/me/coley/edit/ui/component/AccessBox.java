package me.coley.edit.ui.component;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;

import me.coley.edit.asm.Access;

@SuppressWarnings("serial")
public class AccessBox extends JInternalFrame {
	private Consumer<Integer> action;
	private Map<JCheckBox, Integer> compToAccess = new HashMap<>();

	public AccessBox(String title, int init, Consumer<Integer> action) throws Exception {
		super(title);
		this.action = action;
		this.setLayout(new GridLayout(0,3));
		//this.add(comp)
		for (Field acc : Access.class.getDeclaredFields()) {
			acc.setAccessible(true);
			String accName = acc.getName().substring(0,1) + acc.getName().toLowerCase().substring(1);
			int accValue = acc.getInt(null);
			JCheckBox check = new JCheckBox(accName);
			if (Access.hasAccess(init, accValue)) {
				check.setSelected(true);
			}
			compToAccess.put(check, accValue);
			add(check);
		}
	}
	
	public void onUpdate() {
		int access = 0;
		for (Entry<JCheckBox, Integer> entry : compToAccess.entrySet()) {
			if (entry.getKey().isSelected()) {
				access |= entry.getValue().intValue();
			}
		}
		this.action.accept(access);
	}
}
