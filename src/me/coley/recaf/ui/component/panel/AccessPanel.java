package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.asm.Access;

@SuppressWarnings("serial")
public class AccessPanel extends JPanel {
	public final static String TITLE_CLASS = "Class Access";
	public final static String TITLE_FIELD = "Field Access";
	public final static String TITLE_METHOD = "Method Access";
	public final static String TITLE_PARAMETER = "Parameter Access";
	private final Map<JCheckBox, Integer> compToAccess = new HashMap<>();
	private final Consumer<Integer> action;
	private final String title;

	public AccessPanel(ClassNode clazz, JComponent owner) {
		this(AccessPanel.TITLE_CLASS + ": " + clazz.name, clazz.access, acc -> clazz.access = acc, owner);
	}

	public AccessPanel(FieldNode field, JComponent owner) {
		this(AccessPanel.TITLE_FIELD + ": " + field.name, field.access, acc -> field.access = acc, owner);
	}

	public AccessPanel(MethodNode method, JComponent owner) {
		this(AccessPanel.TITLE_METHOD + ": " + method.name, method.access, acc -> method.access = acc, owner);
	}

	private AccessPanel(String title, int init, Consumer<Integer> action, JComponent owner) {
		setBorder(BorderFactory.createTitledBorder("Modifiers"));
		this.title = title;
		this.action = action;
		this.setLayout(new GridLayout(0, 3));
		for (Entry<String, Integer> entry : Access.accessMap.entrySet()) {
			String accName = entry.getKey();
			int accValue = entry.getValue();
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
			String accNameFmt = accName.substring(0, 1) + accName.toLowerCase().substring(1);
			JCheckBox check = new JCheckBox(accNameFmt);
			if (Access.hasAccess(init, accValue)) {
				check.setSelected(true);
			}
			compToAccess.put(check, accValue);
			check.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (owner != null) {
						owner.repaint();
					}
					onUpdate();
				}
			});
			add(check);
		}
	}

	private void onUpdate() {
		// Create new access
		int access = 0;
		for (Entry<JCheckBox, Integer> entry : compToAccess.entrySet()) {
			if (entry.getKey().isSelected()) {
				access |= entry.getValue().intValue();
			}
		}
		this.action.accept(access);
	}

	public String getTitle() {
		return title;
	}
}