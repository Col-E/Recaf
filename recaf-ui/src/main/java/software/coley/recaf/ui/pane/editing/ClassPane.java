package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.pane.editing.android.AndroidClassPane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassPane;

/**
 * Common outline for displaying {@link ClassInfo} content.
 *
 * @author Matt Coley
 * @see JvmClassPane For {@link JvmClassInfo}.
 * @see AndroidClassPane For {@link AndroidClassInfo}.
 */
public abstract class ClassPane extends AbstractContentPane<ClassPathNode> implements ClassNavigable {
	private static final DebuggingLogger logger = Logging.get(ClassPane.class);

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		// Delegate to child components
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof ClassNavigable navigableClass)
				navigableClass.requestFocus(member);
			else {
				// The side-tabs are not class-navigable, but some the side-tab's contents are.
				// We will thus check the children of non class-navigable components to address this.
				for (Navigable childOfChild : navigableChild.getNavigableChildren())
					if (childOfChild instanceof ClassNavigable upnavigableClassatable)
						upnavigableClassatable.requestFocus(member);
			}
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// Update if class has changed.
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;
			Unchecked.checkedForEach(pathUpdateListeners, listener -> listener.accept(classPath),
					(listener, t) -> logger.error("Exception thrown when handling class-pane path update callback", t));

			// Initialize UI if it has not been done yet.
			if (getCenter() == null)
				generateDisplay();

			// Notify children of change.
			getNavigableChildren().forEach(child -> {
				if (child instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(path);
			});
		}
	}

	@Nonnull
	@Override
	public ClassPathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}
}
