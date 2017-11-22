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

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.panel.AccessPanel;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.util.Misc;

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
public class DefinitionBox extends BasicFrame {
	public DefinitionBox(FieldNode fn, JList<?> list) {
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

	public DefinitionBox(MethodNode mn, JList<?> list) {
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

	
	public DefinitionBox(ClassNode cn,ClassDisplayPanel cdp) {
		super("Class: " + cn.name);
		setMaximumSize(new Dimension(700, 700));
		setLayout(new GridBagLayout());
		setClosable(false);
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
		add(new ActionTextField(cn.name, n -> {
			cn.name = n;
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Super Name:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.superName == null ? "" : cn.superName, s -> {
			if (s.isEmpty()) {
				cn.superName = null;
			} else {
				cn.superName = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Source File:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.sourceFile == null ? "" : cn.sourceFile, s -> {
			if (s.isEmpty()) {
				cn.sourceFile = null;
			} else {
				cn.sourceFile = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Version:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.version, s -> {
			if (Misc.isInt(s)) {
				cn.version = Integer.parseInt(s);
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Source Debug:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.sourceDebug == null ? "" : cn.sourceDebug, s -> {
			if (s.isEmpty()) {
				cn.sourceDebug = null;
			} else {
				cn.sourceDebug = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Signature:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.signature == null ? "" : cn.signature, s -> {
			if (s.isEmpty()) {
				cn.signature = null;
			} else {
				cn.signature = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Outer Class:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.outerClass == null ? "" : cn.outerClass, s -> {
			if (s.isEmpty()) {
				cn.outerClass = null;
			} else {
				cn.outerClass = s;
			}
		}), c);		
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Outer Method Name:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.outerMethod == null ? "" : cn.outerMethod, s -> {
			if (s.isEmpty()) {
				cn.outerMethod = null;
			} else {
				cn.outerMethod = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Outer Method Desc:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionTextField(cn.outerMethodDesc == null ? "" : cn.outerMethodDesc, s -> {
			if (s.isEmpty()) {
				cn.outerMethodDesc = null;
			} else {
				cn.outerMethodDesc = s;
			}
		}), c);
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(new JLabel("Acc:"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridwidth = 2;
		add(new ActionButton("Edit Access",() -> {
			try {
				cdp.addWindow(new AccessBox(cn, null));
			} catch (Exception e) {
				cdp.exception(e);
			}
		}),c);
		c.gridy++;
		/*
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		add(new AccessPanel(cn, null), c);
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.0;
		c.gridy++;
		*/
		// Interfaces
		JPanel interfaces = new JPanel();
		interfaces.setBorder(BorderFactory.createTitledBorder("Interfaces"));
		interfaces.setLayout(new BoxLayout(interfaces, BoxLayout.Y_AXIS));
		update(interfaces, cn);
		add(interfaces, c);
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
	
	private void update(JPanel content, ClassNode cn) {
		content.removeAll();
		for (int i = 0; i < cn.interfaces.size(); i++) {
			final int j = i;
			String ex = cn.interfaces.get(i);
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton("Delete", () -> {
				cn.interfaces.remove(j);
				update(content, cn);
			}), BorderLayout.WEST);
			panel.add(new ActionTextField(ex, s -> cn.interfaces.set(j, s)), BorderLayout.CENTER);
			content.add(panel);
		}
		JPanel panel = new JPanel();
		{
			final JTextField text = new JTextField();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton("Add", () -> {
				cn.interfaces.add(text.getText());
				// Bump window size up to fit new button
				setSize(getWidth(), getHeight() + 44);
				update(content, cn);
			}), BorderLayout.WEST);
			panel.add(text, BorderLayout.CENTER);
			content.add(panel);
		}
		content.repaint();
		content.validate();
	}
}
