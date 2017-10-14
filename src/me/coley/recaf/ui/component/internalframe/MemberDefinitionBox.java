package me.coley.recaf.ui.component.internalframe;

import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.JList;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.LabeledComponent;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class MemberDefinitionBox extends BasicFrame {
	public MemberDefinitionBox(FieldNode fn, JList<?> list) {
		super("Definition: " + fn.name);
		setup(fn.name, n -> {
			fn.name = n;
			list.repaint();
		}, fn.desc, d -> {
			fn.desc = d;
			list.repaint();
		}, fn.signature, s -> {
			if (s.isEmpty()) {
				fn.signature = null;
			} else {
				fn.signature = s;
			}
		});
	}

	public MemberDefinitionBox(MethodNode mn, JList<?> list) {
		super("Definition: " + mn.name);
		setup(mn.name, n -> {
			mn.name = n;
			list.repaint();
		}, mn.desc, d -> {
			mn.desc = d;
			list.repaint();
		}, mn.signature, s -> {
			if (s.isEmpty()) {
				mn.signature = null;
			} else {
				mn.signature = s;
			}
		});
	}

	private void setup(String name, Consumer<String> nameConsumer, String desc, Consumer<String> descConsumer, String signature,
					   Consumer<String> signatureConsumer) {
		setLayout(new GridLayout(3, 1));
		add(new LabeledComponent("Name: ", new ActionTextField(name, nameConsumer)));
		add(new LabeledComponent("Descriptor: ", new ActionTextField(desc, descConsumer)));
		add(new LabeledComponent("Signature: ", new ActionTextField(signature == null ? "" : signature, signatureConsumer)));
		setVisible(true);
	}

}
