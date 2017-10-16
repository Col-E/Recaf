package me.coley.recaf.ui.component;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * A group of radio-buttons that handles singular radio selection.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class RadioGroup extends JPanel {
	private final Set<JRadioButton> radios = new HashSet<>();

	public RadioGroup(int rows, int cols) {
		setLayout(new GridLayout(rows, cols));
	}

	/**
	 * Overridden to prevent adding components the default way.
	 */
	@Override
	public Component add(Component c) throws RuntimeException {
		if (c instanceof JRadioButton) {
			JRadioButton radio = (JRadioButton) c;
			radio.addActionListener(new RadioListener(radio));
			radios.add(radio);
			return super.add(radio);
		} else throw new RuntimeException("Non-RadioButtons are not supported!!");
	}

	/**
	 * Listener for disabling other radio buttons.
	 *
	 * @author Matt
	 */
	private class RadioListener implements ActionListener {
		private final JRadioButton button;

		public RadioListener(JRadioButton button) {
			this.button = button;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (JRadioButton radio : radios) {
				if (radio != button && radio.isSelected()) {
					radio.setSelected(false);
				} else if (radio == button && !radio.isSelected()) {
					radio.setSelected(true);
				}
			}
		}
	}
}
