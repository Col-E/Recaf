package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.*;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link MethodContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicMethodContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements MethodContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicMethodContextMenuProviderFactory.class);

	@Inject
	public BasicMethodContextMenuProviderFactory(@Nonnull TextProviderService textService,
												 @Nonnull IconProviderService iconService,
												 @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getMethodContextMenuProvider(@Nonnull ContextSource source,
															@Nonnull Workspace workspace,
															@Nonnull WorkspaceResource resource,
															@Nonnull ClassBundle<? extends ClassInfo> bundle,
															@Nonnull ClassInfo declaringClass,
															@Nonnull MethodMember method) {
		return () -> {
			TextProvider nameProvider = textService.getMethodMemberTextProvider(workspace, resource, bundle, declaringClass, method);
			IconProvider iconProvider = iconService.getClassMemberIconProvider(workspace, resource, bundle, declaringClass, method);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());

			ObservableList<MenuItem> items = menu.getItems();
			items.add(action("menu.goto.method", CarbonIcons.ARROW_RIGHT,
					() -> {
						ClassPathNode classPath = PathNodes.classPath(workspace, resource, bundle, declaringClass);
						try {
							actions.gotoDeclaration(classPath)
									.requestFocus(method);
						} catch (IncompletePathException ex) {
							logger.error("Cannot go to method due to incomplete path", ex);
						}
					}));
			// TODO: implement additional operations
			//  - Go to
			//  - Edit
			//    - (field / method assembler)
			//    - Add annotation
			//    - Remove annotations
			//    - Make no-op
			//  - Copy
			//  - Delete
			//  - Refactor
			//    - Rename
			//  - Search references
			//  - View
			//    - Control flow graph
			//    - Application flow graph
			//  - Deobfuscate
			//    - Regenerate variable names
			//    - Optimize with pattern matchers
			//    - Optimize with SSVM
			//  - Simulate with SSVM (Virtualize > Run)
			return menu;
		};
	}
}
