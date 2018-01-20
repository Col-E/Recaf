package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.panel.AccessPanel;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;
import me.coley.recaf.util.Parse;

/**
 * Window for editing the definitions of members <i>(Fields / Methods)</i>.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class DefinitionBox extends BasicFrame {

	public DefinitionBox(FieldNode fn, JList<?> list) {
		super(Lang.get("window.define.prefix") + fn.name);
		setMaximumSize(new Dimension(700, 700));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(c, new JLabel(Lang.get("window.define.name")), new ActionTextField(fn.name, n -> {
			fn.name = n;
			list.repaint();
		}));
		add(c, new JLabel(Lang.get("window.define.desc")), new ActionTextField(fn.desc, d -> {
			fn.desc = d;
			list.repaint();
		}));
		add(c, new JLabel(Lang.get("window.define.sig")), new ActionTextField(fn.signature == null ? "" : fn.signature, s -> {
			if (s.isEmpty()) {
				fn.signature = null;
			} else {
				fn.signature = s;
			}
		}));
		if (fn.desc.length() == 1 || fn.desc.equals("Ljava/lang/String;")) {
			String value = fn.value == null ? "" : fn.value.toString();
			add(c, new JLabel("DefaultValue:"), new ActionTextField(value, n -> {
				switch (Type.getType(fn.desc).getDescriptor()) {
				case "B":
				case "C":
				case "I":
					fn.value = Integer.parseInt(n);
					break;
				case "J":
					fn.value = Long.parseLong(n);
					break;
				case "F":
					fn.value = Float.parseFloat(n);
					break;
				case "D":
					fn.value = Double.parseDouble(n);
					break;
				case "Ljava/lang/String;":
					fn.value = n;
					break;
				default:
					fn.value = null;
				}
			}));
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
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(c, new JLabel(Lang.get("window.define.name")), new ActionTextField(mn.name, n -> {
			mn.name = n;
			list.repaint();
		}));
		add(c, new JLabel(Lang.get("window.define.desc")), new ActionTextField(mn.desc, d -> {
			mn.desc = d;
			list.repaint();
		}));
		add(c, new JLabel(Lang.get("window.define.sig")), new ActionTextField(mn.signature == null ? "" : mn.signature, s -> {
			if (s.isEmpty()) {
				mn.signature = null;
			} else {
				mn.signature = s;
			}
		}));
		add(c, new AccessPanel(mn, null));
		// Exceptions
		JPanel exceptions = new JPanel();
		exceptions.setBorder(BorderFactory.createTitledBorder(Lang.get("window.define.exceptions")));
		exceptions.setLayout(new BoxLayout(exceptions, BoxLayout.Y_AXIS));
		update(exceptions, mn);
		add(c, exceptions);
		setVisible(true);
	}

	public DefinitionBox(ClassNode cn, ClassDisplayPanel cdp) {
		super(Lang.get("window.class.prefix") + cn.name);
		boolean u = Recaf.INSTANCE.configs.ui.showUncommonAttributes;
		setMaximumSize(new Dimension(700, 700));
		setLayout(new GridBagLayout());
		setClosable(false);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(c, new JLabel(Lang.get("window.class.name")), new ActionTextField(cn.name, n -> {
			cn.name = n;
		}));
		add(c, new JLabel(Lang.get("window.class.super")), new ActionTextField(cn.superName == null ? "" : cn.superName, s -> {
			if (s.isEmpty()) {
				cn.superName = null;
			} else {
				cn.superName = s;
			}
		}));
		add(c, new JLabel(Lang.get("window.class.source")), new ActionTextField(cn.sourceFile == null ? "" : cn.sourceFile, s -> {
			if (s.isEmpty()) {
				cn.sourceFile = null;
			} else {
				cn.sourceFile = s;
			}
		}));
		if (u) {
			add(c, new JLabel(Lang.get("window.class.debug")), new ActionTextField(cn.sourceDebug == null ? "" : cn.sourceDebug,
					s -> {
						if (s.isEmpty()) {
							cn.sourceDebug = null;
						} else {
							cn.sourceDebug = s;
						}
					}));
		}
		add(c, new JLabel(Lang.get("window.class.version")), new ActionTextField(cn.version, s -> {
			if (Parse.isInt(s)) {
				cn.version = Integer.parseInt(s);
			}
		}));

		add(c, new JLabel(Lang.get("window.class.sig")), new ActionTextField(cn.signature == null ? "" : cn.signature, s -> {
			if (s.isEmpty()) {
				cn.signature = null;
			} else {
				cn.signature = s;
			}
		}));
		if (u) {
			add(c, new JLabel(Lang.get("window.class.out.class")), new ActionTextField(cn.outerClass == null ? "" : cn.outerClass,
					s -> {
						if (s.isEmpty()) {
							cn.outerClass = null;
						} else {
							cn.outerClass = s;
						}
					}));
			add(c, new JLabel(Lang.get("window.class.out.name")), new ActionTextField(cn.outerMethod == null ? ""
					: cn.outerMethod, s -> {
						if (s.isEmpty()) {
							cn.outerMethod = null;
						} else {
							cn.outerMethod = s;
						}
					}));

			add(c, new JLabel(Lang.get("window.class.out.desc")), new ActionTextField(cn.outerMethodDesc == null ? ""
					: cn.outerMethodDesc, s -> {
						if (s.isEmpty()) {
							cn.outerMethodDesc = null;
						} else {
							cn.outerMethodDesc = s;
						}
					}));
		}
		add(c, new ActionButton(Lang.get("window.access"), () -> {
			try {
				cdp.addWindow(new AccessBox(cn, null));
			} catch (Exception e) {
				cdp.exception(e);
			}
		}));
		add(c, new ActionButton(Lang.get("window.decompile"), () -> {
			try {
				cdp.decompile(cn, null);
			} catch (Exception e) {
				cdp.exception(e);
			}
		}));
		if (u) {
			add(c, new ActionButton(Lang.get("window.innerclasses"), () -> {
				try {
					cdp.addWindow(new InnerClassBox(cn));
				} catch (Exception e) {
					cdp.exception(e);
				}
			}));
		}
		c.gridy++;
		/*
		 * c.fill = GridBagConstraints.HORIZONTAL; c.gridy++; c.gridx = 0;
		 * c.gridwidth = 3; add(new AccessPanel(cn, null), c); c.fill =
		 * GridBagConstraints.BOTH; c.weighty = 1.0; c.gridy++;
		 */
		// Interfaces
		JPanel interfaces = new JPanel();
		interfaces.setBorder(BorderFactory.createTitledBorder(Lang.get("window.class.interfaces")));
		interfaces.setLayout(new BoxLayout(interfaces, BoxLayout.Y_AXIS));
		update(interfaces, cn);
		add(c, interfaces);
		setVisible(true);
	}

	private void add(GridBagConstraints c, JComponent c1) {
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		add(c1, c);
	}

	private void add(GridBagConstraints c, JComponent c1, JComponent c2) {
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add(c1, c);
		c.gridx = 1;
		c.gridwidth = 2;
		add(c2, c);
	}

	private void update(JPanel content, MethodNode mn) {
		content.removeAll();
		for (int i = 0; i < mn.exceptions.size(); i++) {
			final int j = i;
			String ex = mn.exceptions.get(i);
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(new ActionButton(Lang.get("misc.delete"), () -> {
				if (Recaf.INSTANCE.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, Lang.get("misc.warn.exception"), Lang.get("misc.warn.title"),
							JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
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
			panel.add(new ActionButton(Lang.get("misc.add"), () -> {
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
			panel.add(new ActionButton(Lang.get("misc.delete"), () -> {
				if (Recaf.INSTANCE.configs.ui.confirmDeletions) {
					int dialogResult = JOptionPane.showConfirmDialog(null, Lang.get("misc.warn.interface"), Lang.get("misc.warn.title"),
							JOptionPane.YES_NO_OPTION);
					if (dialogResult != JOptionPane.YES_OPTION) {
						return;
					}
				}
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
			panel.add(new ActionButton(Lang.get("misc.add"), () -> {
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