package me.coley.recaf.ui.component;

import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JPanel;

/**
 * A group of radio-buttons that handles singular radio selection.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class LabeledComponentGroup extends JPanel {
	public LabeledComponentGroup(LabeledComponent... components) {
		setLayout(new GridLayout(0, 2));
		for (LabeledComponent comp : components) {
			add(comp);
		}
	}

	/**
	 * Overridden to prevent adding components the default way.
	 */
	@Override
	public Component add(Component c) throws RuntimeException {
		if (c instanceof LabeledComponent) {
			LabeledComponent comp = (LabeledComponent) c;
			super.add(comp.getLabel());
			return super.add(comp.getComponent());
		} else throw new RuntimeException("Non-LabeledComponent are not supported!!");
	}
}
