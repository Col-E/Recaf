package me.coley.recaf.ui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import me.coley.recaf.agent.Attach;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.action.ActionMenuItem;

/**
 * Menu which displays currently running VM's as sub-items. Clicking spawns
 * Recaf in their process.
 * 
 * @author Matt
 */
@SuppressWarnings("serial")
public class JVMMenu extends JMenu {
	public JVMMenu() {
		super(Lang.get("navbar.agent.list"));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				repopulate();
			}
		});
	}

	/**
	 * Update JVM sub-list.
	 */
	private final void repopulate() {
		removeAll();
		for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
			add(new ActionMenuItem(vm.displayName(), () -> Attach.attach(vm)));
		}
		revalidate();
	}
}
