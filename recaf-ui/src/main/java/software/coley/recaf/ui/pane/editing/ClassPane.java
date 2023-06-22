package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.control.IconView;
import software.coley.recaf.ui.pane.editing.android.AndroidClassPane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassPane;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.ui.pane.editing.tabs.InheritancePane;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;

/**
 * Common outline for displaying {@link ClassInfo} content.
 *
 * @author Matt Coley
 * @see JvmClassPane For {@link JvmClassInfo}.
 * @see AndroidClassPane For {@link AndroidClassInfo}.
 */
public abstract class ClassPane extends AbstractContentPane<ClassPathNode> implements ClassNavigable {
	/**
	 * Configures common side-tab content of child types.
	 *
	 * @param fieldsAndMethodsPane
	 * 		Tab content to show fields/methods of a class.
	 * @param inheritancePane
	 * 		Tab content to show the inheritance hierarchy of a class.
	 */
	protected void configureCommonSideTabs(@Nonnull FieldsAndMethodsPane fieldsAndMethodsPane,
										   @Nonnull InheritancePane inheritancePane) {
		// Setup so clicking on items in fields-and-methods pane will synchronize with content in our class pane.
		fieldsAndMethodsPane.setupSelectionNavigationListener(this);

		// Setup side-tabs
		addSideTab(new BoundTab(Lang.getBinding("fieldsandmethods.title"),
				new IconView(Icons.getImage(Icons.FIELD_N_METHOD)),
				fieldsAndMethodsPane
		));
		addSideTab(new BoundTab(Lang.getBinding("hierarchy.title"),
				CarbonIcons.FLOW,
				inheritancePane
		));
	}

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
			pathUpdateListeners.forEach(listener -> listener.accept(classPath));

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
}
