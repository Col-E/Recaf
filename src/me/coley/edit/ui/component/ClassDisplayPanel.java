package me.coley.edit.ui.component;

import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.edit.ui.Gui;
import me.coley.edit.ui.component.action.ActionButton;
import me.coley.edit.ui.component.action.ActionTextField;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;

@SuppressWarnings("serial")
public class ClassDisplayPanel extends JPanel {
	private final JDesktopPane desktopPane = new JDesktopPane();
	private final ClassNode node;
	private final Gui gui;

	public ClassDisplayPanel(Gui gui, ClassNode node) {
		this.gui = gui;
		this.node = node;
		setLayout(new BorderLayout(0, 0));
		JInternalFrame frameClass = setupClassFrame();
		JInternalFrame frameFields = setupFieldsFrame();
		JInternalFrame frameMethods = setupMethodsFrame();
		desktopPane.add(frameClass);
		desktopPane.add(frameFields);
		desktopPane.add(frameMethods);
		add(desktopPane);
	}

	private JInternalFrame setupClassFrame() {
		JInternalFrame frameClass = new JInternalFrame("Class Data");
		frameClass.setResizable(true);
		frameClass.setIconifiable(true);
		frameClass.setBounds(10, 11, 240, 140);
		frameClass.setVisible(true);
		frameClass.setLayout(new BoxLayout(frameClass.getContentPane(), BoxLayout.Y_AXIS));
		//@formatter:off
		addAll(frameClass,
			new LabeledComponent("SourceFile:", new ActionTextField(node.sourceFile, s -> node.sourceFile = s)),
			new LabeledComponent("SourceDebug:", new ActionTextField(node.sourceDebug, s -> node.sourceDebug = s)),
			new LabeledComponent("Version:", new ActionTextField(node.version, s -> {
				if (isInt(s)) {
					node.version = Integer.parseInt(s);
				}
			})),
			new LabeledComponent("Access:", new ActionButton("Edit Access",() -> {
				try {
					AccessBox box = new AccessBox("Class Access", node.access, acc -> node.access = acc);
					box.setMaximumSize(new Dimension(300, 300));
					box.setPreferredSize(new Dimension(300, 165));
					box.setResizable(true);
					box.setIconifiable(true);
					box.setClosable(true);
					box.pack();
					box.setVisible(true);
					desktopPane.add(box);
				} catch (Exception e) {
					gui.displayError(e);
				}
			}))
		);
		//@formatter:on
		return frameClass;
	}

	private JInternalFrame setupFieldsFrame() {
		JInternalFrame frameFields = new JInternalFrame("Fields");
		frameFields.setResizable(true);
		frameFields.setIconifiable(true);
		frameFields.setBounds(260, 11, 180, 120);
		frameFields.setVisible(true);
		frameFields.setLayout(new BorderLayout());
		JList<FieldNode> fields = new JList<>();
		fields.setCellRenderer(new ListCellRenderer<FieldNode>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends FieldNode> list, FieldNode value, int index,
					boolean isSelected, boolean cellHasFocus) {
				return new JLabel(value.name + " " + value.desc);
			}
		});
		DefaultListModel<FieldNode> model = new DefaultListModel<>();
		for (FieldNode fn : node.fields) {
			model.addElement(fn);
		}
		fields.setModel(model);
		frameFields.add(fields, BorderLayout.CENTER);
		return frameFields;
	}

	private JInternalFrame setupMethodsFrame() {
		JInternalFrame frameMethods = new JInternalFrame("Methods");
		frameMethods.setResizable(true);
		frameMethods.setIconifiable(true);
		frameMethods.setBounds(445, 11, 180, 120);
		frameMethods.setVisible(true);
		frameMethods.setLayout(new BorderLayout());
		JList<MethodNode> methods = new JList<>();
		methods.setCellRenderer(new ListCellRenderer<MethodNode>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends MethodNode> list, MethodNode value, int index,
					boolean isSelected, boolean cellHasFocus) {
				return new JLabel(value.name + value.desc);
			}
		});
		DefaultListModel<MethodNode> model = new DefaultListModel<>();
		for (MethodNode mn : node.methods) {
			model.addElement(mn);
		}
		methods.setModel(model);
		frameMethods.add(methods, BorderLayout.CENTER);
		return frameMethods;
	}

	private static void addAll(JInternalFrame owner, JPanel... components) {
		for (JPanel component : components) {
			owner.add(component);
		}
	}

	private static boolean isInt(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
