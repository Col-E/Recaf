package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.panel.AccessPanel;

/**
 * Window for editing the definitions of members <i>(Fields / Methods)</i>.
 * <hr>
 * This is proof that swing was invented as a joke. But for real, this is
 * ungodly hideous, but it behaves like I want it to more than other layouts
 * I've tried.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class MemberDefinitionBox extends BasicFrame {
	public MemberDefinitionBox(FieldNode fn, JList<?> list) {
		super("Definition: " + fn.name);
		// Forgive me...
		setMaximumSize(new Dimension(700, 700));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Name:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(fn.name, n -> {
			fn.name = n;
			list.repaint();
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Descriptor:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(fn.desc, d -> {
			fn.desc = d;
			list.repaint();
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Signature:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(fn.signature == null ? "" : fn.signature, s -> {
			if (s.isEmpty()) {
				fn.signature = null;
			} else {
				fn.signature = s;
			}
		}), c);
		if (fn.desc.length() == 1 || fn.desc.equals("Ljava/lang/String;")) {
			c.fill = GridBagConstraints.NONE;
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 1;
			add(new JLabel("DefaultValue:"), c);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridwidth = 2;
			String value = fn.value == null ? "" : fn.value.toString();
			add(new ActionTextField(value, n -> {
				fn.value = n;
			}), c);
		}
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		add(new AccessPanel(fn, null), c);
		setVisible(true);
	}

	public MemberDefinitionBox(MethodNode mn, JList<?> list) {
		super("Definition: " + mn.name);
		setMaximumSize(new Dimension(700, 700));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Name:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(mn.name, n -> {
			mn.name = n;
			list.repaint();
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Descriptor:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(mn.desc, d -> {
			mn.desc = d;
			list.repaint();
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Signature:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(mn.signature == null ? "" : mn.signature, s -> {
			if (s.isEmpty()) {
				mn.signature = null;
			} else {
				mn.signature = s;
			}
		}), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		add(new AccessPanel(mn, null), c);
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridy++;
		// Exceptions
		JPanel exceptions = new JPanel();
		exceptions.setBorder(BorderFactory.createTitledBorder("Exceptions"));
		exceptions.setLayout(new BoxLayout(exceptions, BoxLayout.Y_AXIS));
		update(exceptions, mn);
		add(exceptions, c);
		setVisible(true);
	}

	private void update(JPanel content, MethodNode mn) {
		content.removeAll();
		for (int i = 0; i < mn.exceptions.size(); i++) {
			final int j = i;
			String ex = mn.exceptions.get(i);
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton("Delete", () -> {
				mn.exceptions.remove(j);
				update(content, mn);
			}), BorderLayout.WEST);
			panel.add(new ActionTextField(ex, s -> mn.exceptions.set(j, s)), BorderLayout.CENTER);
			content.add(panel);
		}
		JPanel panel = new JPanel();
		{
			final JTextField text = new JTextField();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton("Add", () -> {
				mn.exceptions.add(text.getText());
				// Bump window size up to fit new button
				setSize(getWidth(), getHeight() + 27);
				update(content, mn);
			}), BorderLayout.WEST);
			panel.add(text, BorderLayout.CENTER);
			content.add(panel);
		}
		content.repaint();
		content.validate();
	}
}
