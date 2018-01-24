package me.coley.recaf.ui.component.list;

import java.util.Collection;
import java.util.function.Consumer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;

import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.action.ActionButton;

/**
 * Orderable list with directional buttons <i>(For moving current
 * selection)</i>.
 * 
 * @author Matt
 *
 * @param <T>
 *            Type of data in the list.
 */
@SuppressWarnings("serial")
public class ReorderList<T> extends JPanel {
	private final JList<T> list = new JList<>();
	private final ReorderableListModel model;
	private final JButton up, down;

	public ReorderList(Collection<T> initial) {
		this(initial, null);
	}

	public ReorderList(Collection<T> initial, Consumer<ReorderableListModel> action) {
		list.setModel(model = new ReorderableListModel());
		list.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new LangRenderer());
		for (T element : initial) {
			model.addElement(element);
		}
		up = new ActionButton("\u2191", () -> {
			int index = list.getSelectedIndex();
			model.moveUp(index);
			int pos = index - 1;
			int max = model.getSize() - 1;
			if (pos > max) {
				pos = max;
			}
			list.setSelectedIndex(pos);
			if (action != null) {
				action.accept(model);
			}
		});
		down = new ActionButton("\u2193", () -> {
			int index = list.getSelectedIndex();
			model.movedown(index);
			int pos = index + 1;
			if (pos < 0) {
				pos = 0;
			}
			list.setSelectedIndex(pos);
			if (action != null) {
				action.accept(model);
			}
		});
		// Adding/formatting the components
		setLayout(new BorderLayout());
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 1;
		c.gridwidth = 1;
		buttons.add(up, c);
		c.gridx = 1;
		buttons.add(down, c);
		add(list, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
	}
	
	public class ReorderableListModel extends DefaultListModel<T> {
		public void moveUp(int index) {
			if (index > 0 && index <= getSize()) {
				T item = getElementAt(index);
				removeElementAt(index);
				insertElementAt(item, index - 1);
			}
			fireContentsChanged(this, index - 1, index);
		}

		public void movedown(int index) {
			if (index < getSize() - 1) {
				T item = getElementAt(index);
				removeElementAt(index);
				insertElementAt(item, index + 1);
			}
			fireContentsChanged(this, index, index + 1);
		}
	}
	

	private class LangRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (value instanceof String) {
				value = Lang.get((String) value);
			}
			return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		}
	}
}
