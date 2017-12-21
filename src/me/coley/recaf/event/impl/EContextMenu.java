package me.coley.recaf.event.impl;

import javax.swing.JPopupMenu;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.event.Event;
import me.coley.recaf.ui.component.panel.ClassDisplayPanel;

/**
 * Created when: User right clicks to create a context menu.
 * 
 * @author Matt
 */
public class EContextMenu extends Event {
	private final JPopupMenu menu;
	private final ClassDisplayPanel display;
	private final MethodNode method;
	private final FieldNode field;
	private final AbstractInsnNode ain;
	private final Context context;

	/**
	 * Constructor for fields classes.
	 * 
	 * @param menu
	 * @param display
	 * @param method
	 * @param ain
	 */
	public EContextMenu(JPopupMenu menu, ClassDisplayPanel display) {
		this(Context.CLASS, menu, display, null, null, null);
	}

	/**
	 * Constructor for fields.
	 * 
	 * @param menu
	 * @param display
	 * @param method
	 * @param ain
	 */
	public EContextMenu(JPopupMenu menu, ClassDisplayPanel display, FieldNode field) {
		this(Context.FIELD, menu, display, field, null, null);
	}

	/**
	 * Constructor for methods.
	 * 
	 * @param menu
	 * @param display
	 * @param method
	 * @param ain
	 */
	public EContextMenu(JPopupMenu menu, ClassDisplayPanel display, MethodNode method) {
		this(Context.METHOD, menu, display, null, method, null);
	}

	/**
	 * Constructor for opcodes.
	 * 
	 * @param menu
	 * @param display
	 * @param method
	 * @param ain
	 */
	public EContextMenu(JPopupMenu menu, ClassDisplayPanel display, MethodNode method, AbstractInsnNode ain) {
		this(Context.OPCODE, menu, display, null, method, ain);
	}

	private EContextMenu(Context context, JPopupMenu menu, ClassDisplayPanel display, FieldNode field, MethodNode method,
			AbstractInsnNode ain) {
		this.context = context;
		this.menu = menu;
		this.display = display;
		this.field = field;
		this.method = method;
		this.ain = ain;
	}

	/**
	 * @return The menu being created.
	 */
	public JPopupMenu getMenu() {
		return menu;
	}

	/**
	 * @return The panel containing the menu.
	 */
	public ClassDisplayPanel getDisplay() {
		return display;
	}

	/**
	 * May be null depending on {@link #getContext() context}.
	 * 
	 * @return The method node related to the menu being created.
	 */
	public MethodNode getMethodNode() {
		return method;
	}

	/**
	 * May be null depending on {@link #getContext() context}.
	 * 
	 * @return The method node related to the menu being created.
	 */
	public FieldNode getFieldNode() {
		return field;
	}

	/**
	 * May be null depending on {@link #getContext() context}.
	 * 
	 * @return The opcode related to the menu being created.
	 */
	public AbstractInsnNode getOpcodeNode() {
		return ain;
	}

	/**
	 * The type of menu being created.
	 * 
	 * @return
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Indicator class for determining where the menu has appeared / what data
	 * it contains.
	 * 
	 * @author Matt
	 */
	public static enum Context {
		CLASS, FIELD, OPCODE, METHOD;
	}
}
