package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.UiConfig;
import me.coley.recaf.ui.component.RadioGroup;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionRadioButton;

/**
 * Panel for selecting ASM flags.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class UiOptionsPanel extends JPanel {
	private final UiConfig options = Recaf.INSTANCE.confUI;

	public UiOptionsPanel() {
		//@formatter:off
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder("Opcodes and Methods"));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(new ActionCheckBox("Show jump hints", options.opcodeShowJumpHelp,b -> options.opcodeShowJumpHelp = b));
		p1.add(new ActionCheckBox("Simplify type descriptors", options.opcodeSimplifyDescriptors,b -> options.opcodeSimplifyDescriptors = b));
		p1.add(new ActionCheckBox("Advanced Variable Table", options.showVariableSignatureInTable,b -> options.showVariableSignatureInTable = b));
		p1.add(new ActionCheckBox("Confirm deletions", options.confirmDeletions,b -> options.confirmDeletions = b));
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder("Look and feel"));
		p2.setLayout(new GridLayout(0, 2));
		RadioGroup radios = new RadioGroup(0,1);
		for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
			radios.add(new ActionRadioButton(
				laf.getName(), 
				laf.getClassName().equals(options.getLookAndFeel()), 
				b -> {
					if (b) {
						options.setLookAndFeel(laf.getClassName());
					}
				}));
		}
		p2.add(radios);
		//
		add(p1);
		add(p2);
		//@formatter:on
	}
}
