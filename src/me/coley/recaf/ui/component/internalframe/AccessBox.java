package me.coley.recaf.ui.component.internalframe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.JCheckBox;

import me.coley.recaf.asm.Access;

@SuppressWarnings("serial")
public class AccessBox extends BasicFrame {
	public final static String TITLE_CLASS = "Class Access";
	public final static String TITLE_FIELD = "Field Access";
	public final static String TITLE_METHOD = "Method Access";
	public final static String TITLE_PARAMETER = "Parameter Access";
	private final Consumer<Integer> action;
	private final Map<JCheckBox, Integer> compToAccess = new HashMap<>();

	public AccessBox(String title, int init, Consumer<Integer> action) throws Exception {
		super(title);
		this.action = action;
		this.setLayout(new GridLayout(0, 3));
		// this.add(comp)
		for (Field acc : Access.class.getDeclaredFields()) {
			acc.setAccessible(true);
			String name = acc.getName();
			// Skip non-modifier value fields
			if (name.contains("_")) {
				continue;
			}
			int accValue = acc.getInt(null);
			// Skip modifiers that don't apply to the given access
			if (title.contains(TITLE_CLASS)) {
				// Classes
				if (!Access.hasAccess(Access.CLASS_MODIFIERS, accValue)) {
					continue;
				}
			} else if (title.contains(TITLE_FIELD)) {
				// fields
				if (!Access.hasAccess(Access.FIELD_MODIFIERS, accValue)) {
					continue;
				}
			} else if (title.contains(TITLE_METHOD)) {
				if (title.contains("<c")) {
					// Do not let people edit the static block
					continue;
				} else if (title.contains("<i")) {
					// constructor
					if (!Access.hasAccess(Access.CONSTRUCTOR_MODIFIERS, accValue)) {
						continue;
					}
				} else if (!Access.hasAccess(Access.METHOD_MODIFIERS, accValue)) {
					// Normal method
					continue;
				}
			} else if (title.contains(TITLE_PARAMETER)) {
				// Params only can be final
				if (!Access.hasAccess(Access.FINAL, accValue)) {
					continue;
				}
			}
			// Create checkbox and add to map
			String accName = name.substring(0, 1) + name.toLowerCase().substring(1);
			JCheckBox check = new JCheckBox(accName);
			if (Access.hasAccess(init, accValue)) {
				check.setSelected(true);
			}
			compToAccess.put(check, accValue);
			check.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onUpdate();
				}
			});
			add(check);
		}
		setVisible(true);
	}

	public void onUpdate() {
		// Create new access
		int access = 0;
		for (Entry<JCheckBox, Integer> entry : compToAccess.entrySet()) {
			if (entry.getKey().isSelected()) {
				access |= entry.getValue().intValue();
			}
		}
		this.action.accept(access);
	}
}
