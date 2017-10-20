package me.coley.recaf.ui.component.panel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.Gui;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.LabeledComponentGroup;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.internalframe.AccessBox;
import me.coley.recaf.ui.component.internalframe.DecompileBox;
import me.coley.recaf.ui.component.internalframe.DefaultValueBox;
import me.coley.recaf.ui.component.internalframe.ExceptionsListBox;
import me.coley.recaf.ui.component.internalframe.OpcodeListBox;
import me.coley.recaf.ui.component.internalframe.TryCatchBox;
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
	private final Recaf recaf = Recaf.INSTANCE;
	private final Gui gui = recaf.gui;
	private final JDesktopPane desktopPane = new JDesktopPane();
	private final ClassNode node;

	public ClassDisplayPanel(ClassNode node) {
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
		frameClass.setBounds(10, 11, 248, 284);
		frameClass.setVisible(true);
		frameClass.setLayout(new BoxLayout(frameClass.getContentPane(), BoxLayout.Y_AXIS));
		//@formatter:off
		frameClass.add(new LabeledComponentGroup(
		new LabeledComponent("Version: ", new ActionTextField(node.version, s -> {
			if (Misc.isInt(s)) {
				node.version = Integer.parseInt(s);
			}
		})),
		new LabeledComponent("Source File: ", new ActionTextField(node.sourceFile, s -> {
			if (s.isEmpty()) {
				node.sourceFile = null;
			} else {
				node.sourceFile = s;
			}
		})),
		new LabeledComponent("Source Debug: ", new ActionTextField(node.sourceDebug, s -> {
			if (s.isEmpty()) {
				node.sourceDebug = null;
			} else {
				node.sourceDebug = s;
			}
		})),
		new LabeledComponent("Signature: ", new ActionTextField(node.signature == null ? "" : node.signature, s -> {
			if (s.isEmpty()) {
				node.signature = null;
			} else {
				node.signature = s;
			}
		})),
		new LabeledComponent("Outer Class: ", new ActionTextField(node.outerClass == null ? "" : node.outerClass, s -> {
			if (s.isEmpty()) {
				node.outerClass = null;
			} else {
				node.outerClass = s;
			}
		})),
		new LabeledComponent("Outer Method Name: ", new ActionTextField(node.outerMethod == null ? "" : node.outerMethod, s -> {
			if (s.isEmpty()) {
				node.outerMethod = null;
			} else {
				node.outerMethod = s;
			}
		})),
		new LabeledComponent("Outer Method Desc: ", new ActionTextField(node.outerMethodDesc == null ? "" : node.outerMethodDesc, s -> {
			if (s.isEmpty()) {
				node.outerMethodDesc = null;
			} else {
				node.outerMethodDesc = s;
			}
		})),
		new LabeledComponent("", new ActionButton("Edit Access",() -> {
			try {
				addWindow(new AccessBox(node, null));
			} catch (Exception e) {
				exception(e);
			}
		})),
		new LabeledComponent("", new ActionButton("Decompile", () ->  {
			try {
				addWindow(new DecompileBox(new DecompilePanel(node)));
			} catch (Exception e) {
				exception(e);
			}
		}))
		 ));
		//@formatter:on
		return frameClass;
	}

	private JInternalFrame setupFieldsFrame() {
		JInternalFrame frameFields = new JInternalFrame("Fields");
		frameFields.setResizable(true);
		frameFields.setIconifiable(true);
		frameFields.setBounds(266, 11, 180, 140);
		frameFields.setVisible(true);
		frameFields.setLayout(new BorderLayout());
		JList<FieldNode> fields = new JList<>();
		fields.setCellRenderer(new MemberNodeRenderer(recaf.confUI));
		fields.addMouseListener(new MemberNodeClickListener(this, node, fields));
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
		methods.setCellRenderer(new MemberNodeRenderer(recaf.confUI));
		methods.addMouseListener(new MemberNodeClickListener(this, node, methods));
		DefaultListModel<MethodNode> model = new DefaultListModel<>();
		for (MethodNode mn : node.methods) {
			model.addElement(mn);
		}
		methods.setModel(model);
		frameMethods.add(new JScrollPane(methods), BorderLayout.CENTER);
		// TODO: Switch to table
		// frameMethods.add(new JScrollPane(MemberTable.create(node.methods)),
		// BorderLayout.CENTER);
		frameMethods.pack();
		return frameMethods;
	}

	/**
	 * Opens a window showing the decompiled method belonging to the given
	 * class.
	 *
	 * @param cn
	 * @param mn
	 */
	public DecompileBox decompile(ClassNode cn, MethodNode mn) {
		DecompileBox box = null;
		try {
			addWindow(box = new DecompileBox(new DecompilePanel(cn, mn)));
		} catch (Exception e) {
			exception(e);
		}
		return box;
	}

	/**
	 * Open window for modifying default value of a field.
	 *
	 * @param field
	 */
	public DefaultValueBox openDefaultValue(FieldNode field) {
		DefaultValueBox box = null;
		try {
			addWindow(box = new DefaultValueBox(field.name, field.value, value -> {
				if (field.desc.length() == 1) {
					// Convert string value to int.
					if (Misc.isInt(value)) {
						field.value = Integer.parseInt(value);
					}
				} else {
					// Just set value as string
					field.value = value;
				}
			}));
		} catch (Exception e) {
			exception(e);
		}
		return box;
	}

	/**
	 * Open window for modifying method opcodes.
	 *
	 * @param method
	 */
	public OpcodeListBox openOpcodes(MethodNode method) {
		OpcodeListBox box = null;
		try {
			addWindow(box = new OpcodeListBox(this, method));

		} catch (Exception e) {
			exception(e);
		}
		return box;
	}

	/**
	 * Open window for modifying method exceptions.
	 *
	 * @param method
	 */
	public ExceptionsListBox openExceptions(MethodNode method) {
		ExceptionsListBox box = null;
		try {
			addWindow(box = new ExceptionsListBox(method));
		} catch (Exception e) {
			exception(e);
		}
		return box;
	}

	/**
	 * Open window for modifying method try-catch blocks.
	 *
	 * @param method
	 */
	public TryCatchBox openTryCatchBlocks(MethodNode method) {
		TryCatchBox box = null;
		try {
			addWindow(box = new TryCatchBox(method));
		} catch (Exception e) {
			exception(e);
		}
		return box;
	}

	/**
	 * Adds a window to the page.
	 * 
	 * @param frame
	 *            Window to add.
	 */
	public void addWindow(JInternalFrame frame) {
		desktopPane.add(frame);
		desktopPane.moveToFront(frame);
	}

	/**
	 * Opens an exception in a new tab.
	 * 
	 * @param e
	 */
	public void exception(Exception e) {
		gui.displayError(e);
	}
}
