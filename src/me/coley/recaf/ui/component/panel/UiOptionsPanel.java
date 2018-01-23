package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import me.coley.recaf.ui.component.list.ReorderList;

/**
 * Panel for selecting ASM flags.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class UiOptionsPanel extends JPanel {	
	public UiOptionsPanel() {
		//@formatter:off
		setLayout(new BorderLayout());
		ConfUI options = Recaf.INSTANCE.configs.ui;
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder(Lang.get("option.ui.group.edit")));
		p1.setLayout(new GridLayout(0, 2));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.jumphint"), options.opcodeShowJumpHelp,b -> options.opcodeShowJumpHelp = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.simplify"), options.opcodeSimplifyDescriptors,b -> options.opcodeSimplifyDescriptors = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.advancedvartable"), options.showVariableSignatureInTable,b -> options.showVariableSignatureInTable = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.confirmdelete"), options.confirmDeletions,b -> options.confirmDeletions = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.showempty"), options.showEmptyMemberWindows,b -> options.showEmptyMemberWindows = b));
		p1.add(new ActionCheckBox(Lang.get("option.ui.group.edit.showuncommon"), options.showUncommonAttributes,b -> options.showUncommonAttributes = b));
		p1.add(new LabeledComponent(Lang.get("option.ui.group.edit.defaultaction.opcodes"), new ActionTextField(options.menuOpcodesDefaultAction, b -> options.menuOpcodesDefaultAction = b)));
		p1.add(new LabeledComponent(Lang.get("option.ui.group.edit.defaultaction.method"), new ActionTextField(options.menuMethodDefaultAction, b -> options.menuMethodDefaultAction = b)));
		p1.add(new LabeledComponent(Lang.get("option.ui.group.edit.defaultaction.field"), new ActionTextField(options.menuFieldDefaultAction, b -> options.menuFieldDefaultAction = b)));
		p1.add(new LabeledComponent(Lang.get("option.ui.group.edit.lang"), new ActionTextField(options.language, s -> options.language = s)));
		JPanel p3 = new JPanel();
		p3.setBorder(BorderFactory.createTitledBorder(Lang.get("option.ui.group.menu")));
		p3.setLayout(new GridLayout(0, 2));
		p3.add(new LabeledComponent(Lang.get("option.ui.group.edit.menuorder.opcode"), new ReorderList<String>(options.menuOrderOpcodes, m -> options.menuOrderOpcodes = Collections.list(m.elements()))));
		p3.add(new LabeledComponent(Lang.get("option.ui.group.edit.menuorder.member"), new ReorderList<String>(options.menuOrderMember, m -> options.menuOrderOpcodes = Collections.list(m.elements()))));
		
		
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
		JPanel cont = new JPanel();
		cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
		cont.add(p1);
		cont.add(p2);
		cont.add(p3);
		JScrollPane wrap = new JScrollPane(cont);
		wrap.getVerticalScrollBar().setUnitIncrement(16);

		add(wrap, BorderLayout.CENTER);
		//@formatter:on
	}
}