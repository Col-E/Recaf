package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.ui.pane.search.MemberReferenceSearchPane;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;

/**
 * Basic implementation for {@link FieldContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicFieldContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements FieldContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicFieldContextMenuProviderFactory.class);

	@Inject
	public BasicFieldContextMenuProviderFactory(@Nonnull TextProviderService textService,
	                                            @Nonnull IconProviderService iconService,
	                                            @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getFieldContextMenuProvider(@Nonnull ContextSource source,
	                                                       @Nonnull Workspace workspace,
	                                                       @Nonnull WorkspaceResource resource,
	                                                       @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                                       @Nonnull ClassInfo declaringClass,
	                                                       @Nonnull FieldMember field) {
		return () -> {
			TextProvider nameProvider = textService.getFieldMemberTextProvider(workspace, resource, bundle, declaringClass, field);
			IconProvider iconProvider = iconService.getClassMemberIconProvider(workspace, resource, bundle, declaringClass, field);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			var builder = new ContextMenuBuilder(menu, source).forMember(workspace, resource, bundle, declaringClass, field);

			if (source.isReference()) {
				builder.item("menu.goto.field", ARROW_RIGHT, () -> {
					ClassPathNode classPath = PathNodes.classPath(workspace, resource, bundle, declaringClass);
					try {
						actions.gotoDeclaration(classPath)
								.requestFocus(field);
					} catch (IncompletePathException ex) {
						logger.error("Cannot go to field due to incomplete path", ex);
					}
				});
			} else {
				// Edit menu
				var edit = builder.submenu("menu.edit", EDIT);
				edit.item("menu.edit.assemble.field", EDIT, () -> Unchecked.runnable(() -> actions.openAssembler(PathNodes.memberPath(workspace, resource, bundle, declaringClass, field))).run());
				if (declaringClass.isJvmClass()) {
					JvmClassBundle jvmBundle = (JvmClassBundle) bundle;
					JvmClassInfo declaringJvmClass = declaringClass.asJvmClass();

					edit.item("menu.edit.copy", COPY_FILE, () -> actions.copyMember(workspace, resource, jvmBundle, declaringJvmClass, field));
					edit.item("menu.edit.delete", TRASH_CAN, () -> actions.deleteClassFields(workspace, resource, jvmBundle, declaringJvmClass, List.of(field)));
					edit.item("menu.edit.remove.annotation", CLOSE, () -> actions.deleteMemberAnnotations(workspace, resource, jvmBundle, declaringJvmClass, field))
							.disableWhen(field.getAnnotations().isEmpty());
				}

				// TODO: implement operations
				//  - Edit
				//    - Add annotation
			}

			// Search actions
			builder.item("menu.search.field-references", CODE_REFERENCE, () -> {
				MemberReferenceSearchPane pane = actions.openNewMemberReferenceSearch();
				pane.ownerPredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
				pane.namePredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
				pane.descPredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
				pane.ownerValueProperty().setValue(declaringClass.getName());
				pane.nameValueProperty().setValue(field.getName());
				pane.descValueProperty().setValue(field.getDescriptor());
			});

			// Copy path
			builder.item("menu.tab.copypath", COPY_LINK, () -> ClipboardUtil.copyString(declaringClass, field));

			// Documentation actions
			builder.memberItem("menu.analysis.comment", ADD_COMMENT, actions::openCommentEditing);

			// Refactor actions
			builder.memberItem("menu.refactor.rename", TAG_EDIT, actions::renameField);

			return menu;
		};
	}
}
