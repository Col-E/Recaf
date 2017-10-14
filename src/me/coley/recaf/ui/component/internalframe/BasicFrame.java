package me.coley.recaf.ui.component.internalframe;

import java.awt.Dimension;

import javax.swing.JInternalFrame;

@SuppressWarnings("serial")
public class BasicFrame extends JInternalFrame {
	protected int padding = 12;

	public BasicFrame() {
		this("");
	}

	public BasicFrame(String title) {
		super(title);
		setMaximumSize(new Dimension(400, 400));
		setResizable(true);
		setIconifiable(true);
		setClosable(true);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		pack();
		setSize(getWidth() + padding, getHeight() + padding);
	}
}
