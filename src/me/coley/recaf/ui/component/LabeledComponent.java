package me.coley.recaf.ui.component;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Basic wrapper for:
 *
 * <pre>
 * Label[text] : Component
 * </pre>
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class LabeledComponent extends JPanel {
	private final JLabel label;
	private final JComponent component;

	public LabeledComponent(String label, JComponent component) {
		setLayout(new BorderLayout());
		add(this.label = new JLabel(label), BorderLayout.WEST);
		add(this.component = component, BorderLayout.CENTER);
	}

	/**
	 * @return The label.
	 */
	public JLabel getLabel() {
		return label;
	}

	/**
	 * @return The component.
	 */
	public JComponent getComponent() {
		return component;
	}
}
