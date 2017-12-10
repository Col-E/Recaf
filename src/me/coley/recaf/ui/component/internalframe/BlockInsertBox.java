package me.coley.recaf.ui.component.internalframe;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.list.OpcodeList;

@SuppressWarnings("serial")
public class BlockInsertBox extends BasicFrame {
	private static final String BEFORE = "Before", AFTER = "After"; 
	private String insertPoint, blockKey;

	public BlockInsertBox(InsnList insns, OpcodeList list) {
		super("Insert block");
		setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		JComboBox<String> comboInsert = new JComboBox<>(new String[] { BEFORE, AFTER });
		comboInsert.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				String item = (String) evt.getItem();
				insertPoint = item;
			}
		});
		JComboBox<String> comboBlock = new JComboBox<>(from(Recaf.INSTANCE.configs.blocks.blocks));
		comboBlock.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				String item = (String) evt.getItem();
				blockKey = item;
			}
		});
		// Initial values
		insertPoint = comboInsert.getItemAt(0);
		blockKey = comboBlock.getItemAt(0);
		add(new LabeledComponent("Insertion point: ", comboInsert));
		add(new LabeledComponent("Block to insert: ", comboBlock));
		add(new ActionButton("Insert", () -> {
			List<AbstractInsnNode> selected = list.getSelectedValuesList();
			List<AbstractInsnNode> block = Recaf.INSTANCE.configs.blocks.getClone(blockKey);
			InsnList insertList = new InsnList();
			int start = -1;
			// Calculate start index for insertion into model.
			if (isBefore()) {
				start = insns.indexOf(selected.get(0));
			} else {
				start = insns.indexOf(selected.get(selected.size() - 1)) + 1;
			}
			// Populate model and insertList with content of block.
			DefaultListModel<AbstractInsnNode> model = (DefaultListModel<AbstractInsnNode>) list.getModel();
			for (int i = 0; i < block.size(); i++) {
				AbstractInsnNode clone = block.get(i);
				insertList.add(clone);
				model.insertElementAt(clone, start);
				start++;
			}
			// Insert to InsnList
			if (isBefore()) {
				insns.insertBefore(selected.get(0), insertList);
			} else {
				insns.insert(selected.get(selected.size() - 1), insertList);
			}
			dispose();
		}));
		setVisible(true);
	}

	private boolean isBefore() {
		return insertPoint.equals(BEFORE);
	}

	private String[] from(Map<String, List<AbstractInsnNode>> blocks) {
		String[] a = new String[blocks.size()];
		int i = 0;
		for (String s : blocks.keySet()) {
			a[i] = s;
			i++;
		}
		return a;
	}
}