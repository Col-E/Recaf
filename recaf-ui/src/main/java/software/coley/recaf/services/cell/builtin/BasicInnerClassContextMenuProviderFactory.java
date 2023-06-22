package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.services.cell.ClassContextMenuProviderFactory;
import software.coley.recaf.services.cell.ContextMenuProvider;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.services.cell.InnerClassContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link InnerClassContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicInnerClassContextMenuProviderFactory implements InnerClassContextMenuProviderFactory {
	private final ClassContextMenuProviderFactory classContextFactory;

	@Inject
	public BasicInnerClassContextMenuProviderFactory(@Nonnull ClassContextMenuProviderFactory classContextFactory) {
		this.classContextFactory = classContextFactory;
	}

	@Nonnull
	@Override
	public ContextMenuProvider getInnerClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	@Nonnull Workspace workspace,
																	@Nonnull WorkspaceResource resource,
																	@Nonnull ClassBundle<? extends ClassInfo> bundle,
																	@Nonnull ClassInfo outerClass,
																	@Nonnull InnerClassInfo inner) {
		return () -> {
			// While the inner class attribute gives us access flags, we want to grab the REAL
			// class-info instance for the inner class. This allows us to check for properties and such.
			ClassInfo innerClass = bundle.get(inner.getInnerClassName());
			if (innerClass == null)
				return null;

			// Delegate initial menu creation to standard class menu.
			ContextMenu menu;
			if (innerClass.isJvmClass()) {
				menu = classContextFactory.getJvmClassInfoContextMenuProvider(source, workspace, resource,
						(JvmClassBundle) bundle, innerClass.asJvmClass()).makeMenu();
			} else if (innerClass.isAndroidClass()) {
				menu = classContextFactory.getAndroidClassInfoContextMenuProvider(source, workspace, resource,
						(AndroidClassBundle) bundle, innerClass.asAndroidClass()).makeMenu();
			} else {
				return null;
			}

			return menu;
		};
	}
}
