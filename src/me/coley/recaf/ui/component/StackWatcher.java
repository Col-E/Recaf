package me.coley.recaf.ui.component;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import me.coley.recaf.bytecode.analysis.*;
import me.coley.recaf.ui.component.InsnListEditor.OpcodeList;

public class StackWatcher extends Stage implements ListChangeListener<AbstractInsnNode>, ChangeListener<Number> {
	private final ClassNode owner;
	private final MethodNode method;
	private final OpcodeList list;

	public StackWatcher(ClassNode owner, MethodNode method, OpcodeList list) {
		this.owner = owner;
		this.method = method;
		this.list = list;
	}

	/**
	 * Listener for selection changes. Update UI.
	 */
	@Override
	public void changed(ObservableValue<? extends Number> ob, Number last, Number current) {
		int index = current.intValue();
	}

	/**
	 * Listener for content changes. Regenerate stack-frames.
	 */
	@Override
	public void onChanged(Change<? extends AbstractInsnNode> change) {
		try {
			BasicInterpreter basicInterpreter = new BasicPlusInterpreter();
			Analyzer<BasicValue> analyzer = new Analyzer<>(basicInterpreter);
			Frame<BasicValue>[] frames = analyzer.analyze(owner.name, method);
		} catch (Exception e) {

		}
	}
}
