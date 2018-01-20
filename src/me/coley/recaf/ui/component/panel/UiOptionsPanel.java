package me.coley.recaf.ui.component.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.impl.ConfUI;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.RadioGroup;
import me.coley.recaf.ui.component.action.ActionCheckBox;
import me.coley.recaf.ui.component.action.ActionRadioButton;
import me.coley.recaf.ui.component.action.ActionTextField;

/**
 * Panel for selecting ASM flags.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class UiOptionsPanel extends JPanel {
	public UiOptionsPanel() {
		//@formatter:off
		ConfUI options = Recaf.INSTANCE.configs.ui;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder(Lang.get("option.ui.group.edit")));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.jumphint"), options.opcodeShowJumpHelp,b -> options.opcodeShowJumpHelp = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.simplify"), options.opcodeSimplifyDescriptors,b -> options.opcodeSimplifyDescriptors = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.advancedvartable"), options.showVariableSignatureInTable,b -> options.showVariableSignatureInTable = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.confirmdelete"), options.confirmDeletions,b -> options.confirmDeletions = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.showempty"), options.showEmptyMemberWindows,b -> options.showEmptyMemberWindows = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.showuncommon"), options.showUncommonAttributes,b -> options.showUncommonAttributes = b));
		p1.add(new LabeledComponent(Lang.get("option.ui.group.edit.lang"), new ActionTextField(options.language, s -> options.language = s)));
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder(Lang.get("option.ui.group.look")));
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
		add(p1);
		add(p2);
		//@formatter:on
	}
}