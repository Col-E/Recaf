package me.coley.recaf.ui.control;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;

/**
 * Delegating {@link ClassRepresentation} calls to a child component.
 * The delegate is expected to be set as a child via one of the
 * {@code BorderPane.setTop/Bottom/Left/Right/Center(...)}
 *
 * @author Matt Coley
 */
public class ClassBorderPane extends BorderPane implements ClassRepresentation {
	private final ClassRepresentation delegate;

	/**
	 * @param delegate
	 * 		Representation to pass to.
	 */
	public ClassBorderPane(ClassRepresentation delegate) {
		this.delegate = delegate;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return delegate.getCurrentClassInfo();
	}

	@Override
	public boolean supportsMemberSelection() {
		return delegate.supportsMemberSelection();
	}

	@Override
	public boolean isMemberSelectionReady() {
		return delegate.isMemberSelectionReady();
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		delegate.selectMember(memberInfo);
	}

	@Override
	public SaveResult save() {
		return delegate.save();
	}

	@Override
	public boolean supportsEditing() {
		return delegate.supportsEditing();
	}

	@Override
	public Node getNodeRepresentation() {
		return delegate.getNodeRepresentation();
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		delegate.onUpdate(newValue);
	}
}
