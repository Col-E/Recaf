package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.BasicBlacklistingContextSource;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;

import java.util.Collection;
import java.util.Collections;

/**
 * Common base for context-sensitive components for the assembler.
 *
 * @author Matt Coley
 */
public abstract class ContextualAssemblerComponent extends BorderPane implements Navigable, UpdatableNavigable, EditorComponent {
	/** Source with 'refactor' entries hidden */
	protected static final ContextSource CONTEXT_SOURCE = new BasicBlacklistingContextSource(false, s -> s.contains("refactor"));
	protected PathNode<?> path;
	protected Editor editor;

	/**
	 * Called when {@link #path} is set to a {@link ClassPathNode}.
	 *
	 * @param declared
	 * 		Target class.
	 */
	protected abstract void onSelectClass(@Nonnull ClassInfo declared);

	/**
	 * Called when {@link #path} is set to a {@link ClassMemberPathNode} of a method.
	 *
	 * @param declaring
	 * 		Declaring class.
	 * @param method
	 * 		Target method.
	 */
	protected abstract void onSelectMethod(@Nonnull ClassInfo declaring, @Nonnull MethodMember method);

	/**
	 * Called when {@link #path} is set to a {@link ClassMemberPathNode} of a field.
	 *
	 * @param declaring
	 * 		Declaring class.
	 * @param field
	 * 		Target field.
	 */
	protected abstract void onSelectField(@Nonnull ClassInfo declaring, @Nonnull FieldMember field);

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
		setDisable(true);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		this.path = path;
		if (path instanceof ClassPathNode classPathNode) {
			onSelectClass(classPathNode.getValue());
		} else if (path instanceof ClassMemberPathNode classMemberPathNode) {
			ClassInfo declaring = classMemberPathNode.getParent().getValue();
			ClassMember member = classMemberPathNode.getValue();
			if (member.isField()) {
				onSelectField(declaring, (FieldMember) member);
			} else {
				onSelectMethod(declaring, (MethodMember) member);
			}
		}
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// no-op
	}
}
