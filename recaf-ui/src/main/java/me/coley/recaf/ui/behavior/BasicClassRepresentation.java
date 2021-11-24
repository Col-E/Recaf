package me.coley.recaf.ui.behavior;

import javafx.scene.Node;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;

import java.util.function.Consumer;

/**
 * A basic implementation of {@link ClassRepresentation} capable of covering most class representation cases
 * with just some constructor parameters. Any class being represented with this will be read-only and not editable.
 *
 * @author Matt Coley
 */
public class BasicClassRepresentation implements ClassRepresentation {
	private final Node view;
	private final Consumer<CommonClassInfo> onUpdate;
	private CommonClassInfo info;

	/**
	 * @param view
	 * 		Display node.
	 * @param onUpdate
	 * 		Class update consumer.
	 */
	public BasicClassRepresentation(Node view, Consumer<CommonClassInfo> onUpdate) {
		this.view = view;
		this.onUpdate = onUpdate;
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		info = newValue;
		onUpdate.accept(newValue);
	}

	@Override
	public SaveResult save() {
		throw new UnsupportedOperationException("Basic representations do not support saving!");
	}

	@Override
	public boolean supportsEditing() {
		// Basic representations do not support editing.
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return view;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return info;
	}

	@Override
	public boolean supportsMemberSelection() {
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op
	}
}
