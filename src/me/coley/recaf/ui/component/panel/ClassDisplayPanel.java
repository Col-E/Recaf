package me.coley.recaf.ui.component.panel;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.ui.Gui;
import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.LabeledComponentGroup;
import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.ui.component.action.ActionTextField;
import me.coley.recaf.ui.component.internalframe.AccessBox;
import me.coley.recaf.ui.component.internalframe.DecompileBox;
import me.coley.recaf.ui.component.internalframe.DefinitionBox;
import me.coley.recaf.ui.component.internalframe.OpcodeListBox;
import me.coley.recaf.ui.component.internalframe.TryCatchBox;
import me.coley.recaf.ui.component.list.MemberNodeClickListener;
import me.coley.recaf.ui.component.list.MemberNodeRenderer;
import me.coley.recaf.util.Misc;

import javax.swing.DefaultListModel;
import javax.swing.JDesktopPane;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JInternalFrame;
import javax.swing.JList;

@SuppressWarnings("serial")
public class ClassDisplayPanel extends JPanel {
	private final Recaf recaf = Recaf.INSTANCE;
	private final Gui gui = recaf.gui;
	private final JDesktopPane desktopPane = new JDesktopPane();
	private final ClassNode node;
	private JInternalFrame frameClass, frameMethods, frameFields;
	private JList<MethodNode> methods;
	private JList<FieldNode> fields;

	public ClassDisplayPanel(ClassNode node) {
		this.node = node;
		setLayout(new BorderLayout(0, 0));
		// Class
		setupClassFrame();
		addWindow(frameClass);
		// Fields
		if (node.fields.size() > 0) {
			setupFieldsFrame();
			addWindow(frameFields);
		}
		// Methods
		if (node.methods.size() > 0) {
			setupMethodsFrame();
			addWindow(frameMethods);
		}
		add(desktopPane);
		// Context menu
		desktopPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu popup = new JPopupMenu();
					popup.add(new ActionMenuItem("Tile windows", () -> Misc.tile(desktopPane)));
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	private void setupClassFrame() {
		frameClass = new DefinitionBox(node, this);
		frameClass.setBounds(10, 11, frameClass.getWidth(), frameClass.getHeight());
		/*
		JInternalFrame frameClass = new JInternalFrame("Class Data");
		frameClass.setResizable(true);
		frameClass.setIconifiable(true);
		frameClass.setBounds(10, 11, 248, 284);
		frameClass.setVisible(true);
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
		*/
	}

	private void setupFieldsFrame() {
		frameFields = new JInternalFrame("Fields");
		frameFields.setResizable(true);
		frameFields.setIconifiable(true);
		frameFields.setBounds(frameClass.getWidth() + 11, 11, 180, 140);
		frameFields.setVisible(true);
		frameFields.setLayout(new BorderLayout());
		fields = new JList<>();
		fields.setCellRenderer(new MemberNodeRenderer(recaf.confUI));
		fields.addMouseListener(new MemberNodeClickListener(this, node, fields));
		DefaultListModel<FieldNode> model = new DefaultListModel<>();
		for (FieldNode fn : node.fields) {
			model.addElement(fn);
		}
		fields.setModel(model);
		frameFields.add(new JScrollPane(fields), BorderLayout.CENTER);
		frameFields.pack();
	}

	private void setupMethodsFrame() {
		frameMethods = new JInternalFrame("Methods");
		frameMethods.setResizable(true);
		frameMethods.setIconifiable(true);
		int fw = frameFields == null ? 0 : frameFields.getWidth();
		frameMethods.setBounds(fw + frameClass.getWidth() + 11, 11, 180, 120);
		frameMethods.setVisible(true);
		frameMethods.setLayout(new BorderLayout());

		methods = new JList<>();
		methods.setCellRenderer(new MemberNodeRenderer(recaf.confUI));
		methods.addMouseListener(new MemberNodeClickListener(this, node, methods));
		DefaultListModel<MethodNode> model = new DefaultListModel<>();
		for (MethodNode mn : node.methods) {
			model.addElement(mn);
		}
		methods.setModel(model);
		frameMethods.add(new JScrollPane(methods), BorderLayout.CENTER);
		// TODO: Switch to table. A table may be bigger but allows for sorting
		// of members.
		//
		// frameMethods.add(new JScrollPane(MemberTable.create(node.methods)),
		// BorderLayout.CENTER);
		frameMethods.pack();
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
	 * Opens a window showing the definition of the given member.
	 * 
	 * @param member
	 *            Either an instance of FieldNode or MethodNode.
	 */
	public void openDefinition(Object member) {
		try {
			if (member instanceof FieldNode) {
				FieldNode fn = (FieldNode) member;
				addWindow(new DefinitionBox(fn, fields));
			} else if (member instanceof MethodNode) {
				MethodNode mn = (MethodNode) member;
				addWindow(new DefinitionBox(mn, methods));
			}
		} catch (Exception e) {
			exception(e);
		}
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
