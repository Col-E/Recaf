package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import me.coley.recaf.Options;
import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Access;
import me.coley.recaf.ui.component.action.ActionCheckBox;

/**
 * Panel for selecting ASM flags.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class AsmFlagsPanel extends JPanel {
	private final Options options = Recaf.getInstance().options;
	private ActionCheckBox inE, inD, inF, inC, outF, outM;

	public AsmFlagsPanel() {
		//@formatter:off
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder("Input Flags"));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(inE = new ActionCheckBox("Expand Frames", Access.hasAccess(options.classFlagsInput, ClassReader.EXPAND_FRAMES), (b) -> update()));
		p1.add(inD = new ActionCheckBox("Skip Debug", Access.hasAccess(options.classFlagsInput, ClassReader.SKIP_DEBUG), (b) -> update()));
		p1.add(inF = new ActionCheckBox("Skip Frames", Access.hasAccess(options.classFlagsInput, ClassReader.SKIP_FRAMES), (b) -> update()));
		p1.add(inC = new ActionCheckBox("Skip Code", Access.hasAccess(options.classFlagsInput, ClassReader.SKIP_CODE), (b) -> update()));
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder("Output Flags"));
		p2.setLayout(new GridLayout(0, 2));
		p2.add(outF = new ActionCheckBox("Compute Frames", Access.hasAccess(options.classFlagsOutput, ClassWriter.COMPUTE_FRAMES), (b) -> update()));
		p2.add(outM = new ActionCheckBox("Compute Maxs", Access.hasAccess(options.classFlagsOutput, ClassWriter.COMPUTE_MAXS), (b) -> update()));
		add(p1);
		add(p2);
		//@formatter:on
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
		options.classFlagsInput = in;
		options.classFlagsOutput = out;
	}
}
