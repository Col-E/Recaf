package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import me.coley.recaf.Program;
import me.coley.recaf.ui.component.action.ActionCheckBox;

/**
 * Panel for selecting ASM flags.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class AsmFlagsPanel extends JPanel {
	private ActionCheckBox inE, inD, inF, inC, outF, outM;

	public AsmFlagsPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder("Input Flags"));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(inE = new ActionCheckBox("Expand Frames", true, (b) -> update()));
		p1.add(inD = new ActionCheckBox("Skip Debug", false, (b) -> update()));
		p1.add(inF = new ActionCheckBox("Skip Frames", false, (b) -> update()));
		p1.add(inC = new ActionCheckBox("Skip Code", false, (b) -> update()));
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder("Output Flags"));
		p2.setLayout(new GridLayout(0, 2));
		p2.add(outF = new ActionCheckBox("Compute Frames", true, (b) -> update()));
		p2.add(outM = new ActionCheckBox("Compute Maxs", false, (b) -> update()));
		add(p1);
		add(p2);
	}

	private void update() {
		int in = 0;
		int out = 0;
		if (inE.isSelected()) {
			in |= ClassReader.EXPAND_FRAMES;
		}
		if (inD.isSelected()) {
			in |= ClassReader.SKIP_DEBUG;
		}
		if (inF.isSelected()) {
			in |= ClassReader.SKIP_FRAMES;
		}
		if (inC.isSelected()) {
			in |= ClassReader.SKIP_CODE;
		}
		if (outF.isSelected()) {
			out |= ClassWriter.COMPUTE_FRAMES;
		}
		if (outM.isSelected()) {
			out |= ClassWriter.COMPUTE_MAXS;
		}
		Program.getInstance().options.classFlagsInput = in;
		Program.getInstance().options.classFlagsInput = out;
	}
}
