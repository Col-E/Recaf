package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Access;
import me.coley.recaf.config.impl.ConfAsm;
import me.coley.recaf.ui.Lang;
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
		//@formatter:off
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder(Lang.get("option.asm.group.inflags")));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(inE = new ActionCheckBox(Lang.get("option.asm.group.inflags.expand"), Access.hasAccess(getOptions().classFlagsInput, ClassReader.EXPAND_FRAMES), (b) -> update()));
		p1.add(inD = new ActionCheckBox(Lang.get("option.asm.group.inflags.skipdebug"), Access.hasAccess(getOptions().classFlagsInput, ClassReader.SKIP_DEBUG), (b) -> update()));
		p1.add(inF = new ActionCheckBox(Lang.get("option.asm.group.inflags.skipframes"), Access.hasAccess(getOptions().classFlagsInput, ClassReader.SKIP_FRAMES), (b) -> update()));
		p1.add(inC = new ActionCheckBox(Lang.get("option.asm.group.inflags.skipcode"), Access.hasAccess(getOptions().classFlagsInput, ClassReader.SKIP_CODE), (b) -> update()));
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder(Lang.get("option.asm.group.outflags")));
		p2.setLayout(new GridLayout(0, 2));
		p2.add(outF = new ActionCheckBox(Lang.get("option.asm.group.outflags.frames"), Access.hasAccess(getOptions().classFlagsOutput, ClassWriter.COMPUTE_FRAMES), (b) -> update()));
		p2.add(outM = new ActionCheckBox(Lang.get("option.asm.group.outflags.maxs"), Access.hasAccess(getOptions().classFlagsOutput, ClassWriter.COMPUTE_MAXS), (b) -> update()));
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
		getOptions().classFlagsInput = in;
		getOptions().classFlagsOutput = out;
	}
	
	private static ConfAsm getOptions() {
		return Recaf.INSTANCE.configs.asm;
	}
}