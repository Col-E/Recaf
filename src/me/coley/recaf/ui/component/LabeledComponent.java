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
	public LabeledComponent(String label, JComponent component) {
		setLayout(new BorderLayout());
		add(new JLabel(label), BorderLayout.WEST);
		add(component, BorderLayout.CENTER);
	}
}
