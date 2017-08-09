package me.coley.recaf.ui.component.internalframe;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

// Not extending BasicFrame is intentional.
@SuppressWarnings("serial")
public class EditBox extends JInternalFrame {
	private boolean hasContent;

	public EditBox(String title) {
		super(title);
		setResizable(true);
		setIconifiable(true);
		setClosable(true);
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			pack();
			// setMinimumSize(getSize());
		}
	}

	@Override
	public Component add(Component comp) {
		// Don't count internal swing components
		if (!(comp instanceof BasicInternalFrameTitlePane)) {
			hasContent = true;
		}
		return super.add(comp);
	}

	public boolean hasContent() {
		return hasContent;
	}

}