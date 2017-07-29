package me.coley.recaf.ui.component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Program;
import me.coley.recaf.ui.Gui;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.internalframe.AccessBox;
import me.coley.recaf.ui.component.list.MemberNodeClickListener;
import me.coley.recaf.ui.component.list.MemberNodeRenderer;
import me.coley.recaf.util.Misc;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
import java.awt.BorderLayout;
import javax.swing.JInternalFrame;
import javax.swing.JList;

@SuppressWarnings("serial")
public class ClassDisplayPanel extends JPanel {
	private final JDesktopPane desktopPane = new JDesktopPane();
	private final ClassNode node;
	private final Program callback;
	private final Gui gui;

	public ClassDisplayPanel(Program callback, ClassNode node) {
		this.callback = callback;
		this.gui = callback.window;
		this.node = node;
		setLayout(new BorderLayout(0, 0));
		// Class
		JInternalFrame frameClass = setupClassFrame();
		addWindow(frameClass);
		// Fields
		if (node.fields.size() > 0) {
			JInternalFrame frameFields = setupFieldsFrame();
			addWindow(frameFields);
		}
		// Methods
		if (node.methods.size() > 0) {
			JInternalFrame frameMethods = setupMethodsFrame();
			addWindow(frameMethods);
		}
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
		Misc.addAll(frameClass,
			new LabeledComponent("SourceFile:", new ActionTextField(node.sourceFile, s -> node.sourceFile = s)),
			new LabeledComponent("SourceDebug:", new ActionTextField(node.sourceDebug, s -> node.sourceDebug = s)),
			new LabeledComponent("Version:", new ActionTextField(node.version, s -> {
				if (Misc.isInt(s)) {
					node.version = Integer.parseInt(s);
				}
			})),
			new LabeledComponent("Access:", new ActionButton("Edit Access",() -> {
				try {					
					addWindow(new AccessBox(node));
				} catch (Exception e) {
					exception(e);
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
		fields.setCellRenderer(new MemberNodeRenderer());
		fields.addMouseListener(new MemberNodeClickListener(callback, this, node, fields));
		DefaultListModel<FieldNode> model = new DefaultListModel<>();
		for (FieldNode fn : node.fields) {
			model.addElement(fn);
		}
		fields.setModel(model);
		frameFields.add(new JScrollPane(fields), BorderLayout.CENTER);
		frameFields.pack();
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
		methods.setCellRenderer(new MemberNodeRenderer());
		methods.addMouseListener(new MemberNodeClickListener(callback, this, node, methods));
		DefaultListModel<MethodNode> model = new DefaultListModel<>();
		for (MethodNode mn : node.methods) {
			model.addElement(mn);
		}
		methods.setModel(model);
		frameMethods.add(new JScrollPane(methods), BorderLayout.CENTER);
		frameMethods.pack();
		return frameMethods;
	}

	public void addWindow(JInternalFrame frame) {
		desktopPane.add(frame);
		desktopPane.moveToFront(frame);
	}

	public void exception(Exception e) {
		gui.displayError(e);
	}
}
