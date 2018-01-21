package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class InnerClassBox extends BasicFrame {
	private static final Color bg = new Color(166, 166, 166);
	private final JScrollPane scroll = new JScrollPane();

	public InnerClassBox(ClassNode cn) {
		super(Lang.get("window.innerclasses.prefix") + cn.name);
		setBackground(bg);
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);

		update(cn);
		int k = 162;
		int s = Math.min(k * 2, k * (cn.innerClasses.size() + 1));
		scroll.setPreferredSize(new Dimension(350, s));
		setVisible(true);
	}

	private void update(ClassNode cn) {
		JPanel content = new JPanel();
		content.setLayout(new GridLayout(0, 1));
		for (int i = 0; i <= cn.innerClasses.size(); i++) {
			content.add(make(i, cn));
		}
		scroll.setViewportView(content);
	}

	private JPanel make(final int i, ClassNode cn) {
		boolean isNew = i >= cn.innerClasses.size();
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEtchedBorder());
		InnerClassNode node;
		if (isNew) {
			node = new InnerClassNode(null, null, null, 0);
		} else {
			node = cn.innerClasses.get(i);
		}
		List<JComponent> comps = new ArrayList<>();
		comps.add(new LabeledComponent(Lang.get("window.method.inner.name"), new ActionTextField(node.name, l -> node.name = l)));
		comps.add(new LabeledComponent(Lang.get("window.method.inner.outname"), new ActionTextField( node.outerName, l -> node.outerName = l)));
		comps.add(new LabeledComponent(Lang.get("window.method.inner.inname"), new ActionTextField( node.innerName, l -> node.innerName = l)));
		if (isNew) {
			comps.add(new ActionButton(Lang.get("misc.add"), () -> {
				// Outer / Inner names only needed for non-anonymous classes
				if (node.name == null) {
					return;
				} else {
					Recaf.INSTANCE.ui.setTempTile(Lang.get("misc.warn.fill.single") + "'Name'", 1000);
				}
				cn.innerClasses.add(node);
				update(cn);
			}));
		} else {
			comps.add(new ActionButton(Lang.get("misc.delete"), () -> {
				if (Recaf.INSTANCE.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, Lang.get("misc.warn.inner"), Lang.get("misc.warn.title"),
							JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
				cn.innerClasses.remove(i);
				update(cn);
			}));
		}
		for (JComponent c : comps) {
			c.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			panel.add(c);
		}
		return panel;
	}
}