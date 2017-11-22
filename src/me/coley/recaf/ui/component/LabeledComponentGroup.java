package me.coley.recaf.ui.component;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;

/**
 * A group of radio-buttons that handles singular radio selection.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class LabeledComponentGroup extends JPanel {
	private final GridBagConstraints c = new GridBagConstraints();

	public LabeledComponentGroup(LabeledComponent... components) {
		setLayout(new GridBagLayout());
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.weighty = 1.0;
		for (LabeledComponent comp : components) {
			add(comp);
			c.gridy += 1;
		}
	}

	/**
	 * Overridden to prevent adding components the default way.
	 */
	@Override
	public Component add(Component comp) throws RuntimeException {
		if (comp instanceof LabeledComponent) {
			LabeledComponent lc = (LabeledComponent) comp;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridwidth = 1;
			super.add(lc.getLabel(), c);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridwidth = 1;
			super.add(lc.getComponent(), c);
			return comp;
		} else throw new RuntimeException("Non-LabeledComponent are not supported!!");
	}
}
