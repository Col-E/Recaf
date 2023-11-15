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
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;

import java.util.Collection;
import java.util.Collections;

/**
 * Common base for context-sensitive components for the assembler.
 *
 * @author Matt Coley
 */
public abstract class ContextualAssemblerComponent extends BorderPane implements Navigable, UpdatableNavigable {
	protected PathNode<?> path;

	protected abstract void onSelectClass(@Nonnull ClassInfo declared);

	protected abstract void onSelectMethod(@Nonnull ClassInfo declaring, @Nonnull MethodMember method);

	protected abstract void onSelectField(@Nonnull ClassInfo declaring, @Nonnull FieldMember field);

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
