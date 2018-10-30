package me.coley.recaf.ui.component;

import javafx.scene.control.*;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.*;

/**
 * Pane displaying editable attributes of a ClassNode <i>(class info + table of fields + table of methods)</i>.
 * 
 * @author Matt
 */
public class ClassEditTabs extends TabPane {
	public ClassEditTabs(ClassNode node) {
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		getTabs().add(new Tab(Lang.get("ui.edit.tab.classinfo"), new ClassInfoSheet(node)));
		getTabs().add(new Tab(Lang.get("ui.edit.tab.fields"), new FieldTable(node, node.fields)));
		getTabs().add(new Tab(Lang.get("ui.edit.tab.methods"), new MethodTable(node, node.methods)));
	}
}