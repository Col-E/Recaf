package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Objects;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.ARROW_RIGHT;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.TAG_EDIT;
import static software.coley.recaf.util.Menus.action;

/**
 * Basic implementation for {@link LocalVariableContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicLocalVariableContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements LocalVariableContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicLocalVariableContextMenuProviderFactory.class);

	@Inject
	public BasicLocalVariableContextMenuProviderFactory(@Nonnull TextProviderService textService,
	                                                   @Nonnull IconProviderService iconService,
	                                                   @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getLocalVariableContextMenuProvider(@Nonnull ContextSource source,
	                                                               @Nonnull Workspace workspace,
	                                                               @Nonnull WorkspaceResource resource,
	                                                               @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                                               @Nonnull ClassInfo declaringClass,
	                                                               @Nonnull MethodMember declaringMethod,
	                                                               @Nonnull LocalVariable variable) {
		return () -> {
			TextProvider nameProvider = textService.getVariableTextProvider(workspace, resource, bundle, declaringClass, declaringMethod, variable);
			IconProvider iconProvider = iconService.getVariableIconProvider(workspace, resource, bundle, declaringClass, declaringMethod, variable);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());

			String typeName = objectTypeName(variable);
			if (typeName != null) {
				ClassPathNode typePath = workspace.findClass(typeName);
				ActionMenuItem gotoTypeAction = action("menu.goto.type.class", ARROW_RIGHT, () -> {
					try {
						actions.gotoDeclaration(Objects.requireNonNull(typePath));
					} catch (IncompletePathException ex) {
						logger.error("Cannot go to variable type due to incomplete path", ex);
					}
				});
				if (typePath == null)
					gotoTypeAction.setDisable(true);
				menu.getItems().add(gotoTypeAction);
			}

			if (!resource.isInternal() && variable.getIndex() >= 0) {
				menu.getItems().add(action("menu.refactor.rename", TAG_EDIT, () -> actions.rename(
						PathNodes.variablePath(workspace, resource, bundle, declaringClass, declaringMethod, variable))));
			}

			return menu;
		};
	}

	/**
	 * @param variable
	 * 		Variable to get the object type name from.
	 *
	 * @return Internal name of the variable object type, or {@code null} for primitive values.
	 */
	@Nullable
	private static String objectTypeName(@Nonnull LocalVariable variable) {
		Type type = Type.getType(variable.getDescriptor());
		while (type.getSort() == Type.ARRAY)
			type = type.getElementType();
		return type.getSort() == Type.OBJECT ? type.getInternalName() : null;
	}
}
