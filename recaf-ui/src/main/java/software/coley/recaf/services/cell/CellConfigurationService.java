package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.cell.context.ContextMenuProviderService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UnsupportedContentException;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Common configuration handling for {@link Cell} types.
 *
 * @author Matt Coley
 * @see WorkspaceTreeCell Tree cell handling.
 */
@ApplicationScoped
public class CellConfigurationService implements Service {
	public static final String SERVICE_ID = "cell-configuration";
	private static final String UNKNOWN_TEXT = "[ERROR]";
	private static final String CLASS_EDITED = "modified-class-cell";
	private static final Node UNKNOWN_GRAPHIC = new FontIconView(CarbonIcons.MISUSE_ALT);
	private static final Logger logger = Logging.get(WorkspaceTreeCell.class);
	private final CellConfigurationServiceConfig config;
	private final TextProviderService textService;
	private final IconProviderService iconService;
	private final ContextMenuProviderService contextMenuService;
	private final Actions actions;

	/**
	 * @param config
	 * 		Service config.
	 * @param textService
	 * 		Service to provide text.
	 * @param iconService
	 * 		Service to provide icons.
	 * @param contextMenuService
	 * 		Service to provide context menus.
	 * @param actions
	 * 		Action handling.
	 */
	@Inject
	public CellConfigurationService(@Nonnull CellConfigurationServiceConfig config,
	                                @Nonnull TextProviderService textService,
	                                @Nonnull IconProviderService iconService,
	                                @Nonnull ContextMenuProviderService contextMenuService,
	                                @Nonnull Actions actions) {
		this.config = config;
		this.textService = textService;
		this.iconService = iconService;
		this.contextMenuService = contextMenuService;
		this.actions = actions;

		// TODO: Handle new path types
		//  (FILE)
		//   - LineNumberPathNode
		//  (METHOD)
		//   - CatchPathNode
		//   - InstructionPathNode
		//   - LocalVariablePathNode
		//   - ThrowsPathNode
	}

	/**
	 * @param cell
	 * 		Cell to reset.
	 */
	public void reset(@Nonnull Cell<?> cell) {
		FxThreadUtil.run(() -> {
			cell.getStyleClass().remove(CLASS_EDITED);
			cell.setText(null);
			cell.setGraphic(null);
			cell.setContextMenu(null);
			cell.setOnMouseClicked(null);
		});
	}

	/**
	 * @param cell
	 * 		Cell to configure.
	 * @param item
	 * 		Content within the cell.
	 * @param source
	 * 		Origin source of the cell, for context menu specialization.
	 */
	public void configure(@Nonnull Cell<?> cell, @Nonnull PathNode<?> item, @Nonnull ContextSource source) {
		FxThreadUtil.run(() -> {
			configureStyle(cell, item);
			cell.setText(textOf(item));
			cell.setGraphic(graphicOf(item));
			cell.setOnMouseClicked(contextMenuHandlerOf(cell, item, source));
		});
	}

	/**
	 * @param item
	 * 		Item to open.
	 *
	 * @return Opened navigable display, or {@code null} if the path could not be opened.
	 */
	@Nullable
	private Navigable openPath(@Nonnull PathNode<?> item) {
		try {
			if (item instanceof ClassPathNode classPathNode) {
				return actions.gotoDeclaration(classPathNode);
			} else if (item instanceof ClassMemberPathNode classMemberPathNode) {
				ClassPathNode parent = classMemberPathNode.getParent();
				if (parent != null) {
					ClassNavigable classNavigable = actions.gotoDeclaration(parent);
					classNavigable.requestFocus(classMemberPathNode.getValue());
					return classNavigable;
				}
			} else if (item instanceof FilePathNode filePathNode) {
				return actions.gotoDeclaration(filePathNode);
			}
		} catch (IncompletePathException ex) {
			logger.error("Cannot open incomplete path", ex);
		} catch (UnsupportedContentException ex) {
			logger.warn("Cannot open unsupported content type");
		}
		return null;
	}

	/**
	 * @param cell
	 * 		Cell node to configure style of.
	 * @param item
	 * 		Content within the cell.
	 */
	public void configureStyle(@Nonnull Node cell, @Nonnull PathNode<?> item) {
		// Add the edited class CSS style to classes & files with changes made to them
		cell.getStyleClass().remove(CLASS_EDITED);
		if (item instanceof ClassPathNode classPathNode) {
			var bundle = classPathNode.getValueOfType(ClassBundle.class);
			if (bundle != null && bundle.hasHistory(classPathNode.getValue().getName())) {
				cell.getStyleClass().add(CLASS_EDITED);
			}
		} else if (item instanceof FilePathNode filePathNode) {
			var bundle = filePathNode.getValueOfType(FileBundle.class);
			if (bundle != null && bundle.hasHistory(filePathNode.getValue().getName())) {
				cell.getStyleClass().add(CLASS_EDITED);
			}
		}
	}

	/**
	 * @param item
	 * 		Item to create text for.
	 *
	 * @return Text for the item represented by the path.
	 */
	@SuppressWarnings("unchecked")
	public String textOf(@Nonnull PathNode<?> item) {
		Workspace workspace = item.getValueOfType(Workspace.class);
		WorkspaceResource resource = item.getValueOfType(WorkspaceResource.class);

		if (workspace == null) {
			logger.error("Path node missing workspace section: {}", item);
			return UNKNOWN_TEXT;
		}
		if (resource == null) {
			logger.error("Path node missing resource section: {}", item);
			return UNKNOWN_TEXT;
		}

		if (item instanceof ClassPathNode classPath) {
			ClassBundle<?> bundle = classPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Class path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo info = classPath.getValue();
			if (info.isJvmClass()) {
				return textService.getJvmClassInfoTextProvider(workspace, resource,
						(JvmClassBundle) bundle, info.asJvmClass()).makeText();
			} else if (info.isAndroidClass()) {
				return textService.getAndroidClassInfoTextProvider(workspace, resource,
						(AndroidClassBundle) bundle, info.asAndroidClass()).makeText();
			}
		} else if (item instanceof FilePathNode filePath) {
			FileBundle bundle = filePath.getValueOfType(FileBundle.class);
			if (bundle == null) {
				logger.error("File path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			FileInfo info = filePath.getValue();
			return textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
		} else if (item instanceof ClassMemberPathNode memberNode) {
			ClassBundle<?> bundle = memberNode.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Member path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = memberNode.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Member path node missing class section: {}", item);
				return null;
			}

			ClassMember member = memberNode.getValue();
			if (member instanceof FieldMember fieldMember) {
				return textService.getFieldMemberTextProvider(workspace, resource, bundle, classInfo, fieldMember).makeText();
			} else if (member instanceof MethodMember methodMember) {
				return textService.getMethodMemberTextProvider(workspace, resource, bundle, classInfo, methodMember).makeText();
			}
		} else if (item instanceof DirectoryPathNode directoryPath) {
			Bundle<?> bundle = directoryPath.getValueOfType(Bundle.class);
			if (bundle == null) {
				logger.error("Directory/package path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			if (bundle instanceof FileBundle fileBundle) {
				return textService.getDirectoryTextProvider(workspace, resource, fileBundle, directoryPath.getValue()).makeText();
			} else if (bundle instanceof ClassBundle<?> classBundle) {
				return textService.getPackageTextProvider(workspace, resource, classBundle, directoryPath.getValue()).makeText();
			}
		} else if (item instanceof InnerClassPathNode innerClassPath) {
			ClassBundle<? extends ClassInfo> bundle = innerClassPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Inner class path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo outerClass = innerClassPath.getValueOfType(ClassInfo.class);
			if (outerClass == null) {
				logger.error("Inner class path node missing outer class section: {}", item);
				return UNKNOWN_TEXT;
			}

			InnerClassInfo innerClass = innerClassPath.getValue();
			return textService.getInnerClassInfoTextProvider(workspace, resource,
					bundle, outerClass.asJvmClass(), innerClass).makeText();
		} else if (item instanceof AnnotationPathNode annotationPath) {
			ClassBundle<? extends ClassInfo> bundle = annotationPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Annotation path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			Annotated annotated = annotationPath.getValueOfType(Annotated.class);
			if (annotated == null) {
				logger.error("Annotation path node missing annotated element section: {}", item);
				return UNKNOWN_TEXT;
			}

			AnnotationInfo annotation = annotationPath.getValue();
			return textService.getAnnotationTextProvider(workspace, resource, bundle, annotated, annotation).makeText();
		} else if (item instanceof BundlePathNode bundlePath) {
			return textService.getBundleTextProvider(workspace, resource, bundlePath.getValue()).makeText();
		} else if (item instanceof ResourcePathNode) {
			return textService.getResourceTextProvider(workspace, resource).makeText();
		} else if (item instanceof InstructionPathNode instructionPath) {
			ClassBundle<? extends ClassInfo> bundle = instructionPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Instruction path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo declaringClass = instructionPath.getValueOfType(ClassInfo.class);
			if (declaringClass == null) {
				logger.error("Instruction path node missing class section: {}", item);
				return UNKNOWN_TEXT;
			}

			MethodMember declaringMethod = instructionPath.getValueOfType(MethodMember.class);
			if (declaringMethod == null) {
				logger.error("Instruction path node missing method section: {}", item);
				return UNKNOWN_TEXT;
			}

			return textService.getInstructionTextProvider(workspace, resource, bundle,
					declaringClass, declaringMethod, instructionPath.getValue()).makeText();
		} else if (item instanceof LineNumberPathNode lineNumberPath) {
			FileBundle bundle = lineNumberPath.getValueOfType(FileBundle.class);
			if (bundle == null) {
				logger.error("Line number path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			FileInfo declaringTextFile = lineNumberPath.getValueOfType(FileInfo.class);
			if (declaringTextFile == null) {
				logger.error("Line number path node missing file section: {}", item);
				return UNKNOWN_TEXT;
			}

			int line = lineNumberPath.getValue();
			return textService.getLineNumberTextProvider(workspace, resource, bundle, declaringTextFile, line).makeText();
		} else if (item instanceof LocalVariablePathNode localVariablePath) {
			ClassBundle<? extends ClassInfo> bundle = localVariablePath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Local var path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo declaringClass = localVariablePath.getValueOfType(ClassInfo.class);
			if (declaringClass == null) {
				logger.error("Local var path node missing class section: {}", item);
				return UNKNOWN_TEXT;
			}

			MethodMember declaringMethod = localVariablePath.getValueOfType(MethodMember.class);
			if (declaringMethod == null) {
				logger.error("Local var path node missing method section: {}", item);
				return UNKNOWN_TEXT;
			}

			return textService.getVariableTextProvider(workspace, resource, bundle,
					declaringClass, declaringMethod, localVariablePath.getValue()).makeText();
		} else if (item instanceof ThrowsPathNode throwsPath) {
			ClassBundle<? extends ClassInfo> bundle = throwsPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Throws path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo declaringClass = throwsPath.getValueOfType(ClassInfo.class);
			if (declaringClass == null) {
				logger.error("Throws path node missing class section: {}", item);
				return UNKNOWN_TEXT;
			}

			MethodMember declaringMethod = throwsPath.getValueOfType(MethodMember.class);
			if (declaringMethod == null) {
				logger.error("Throws path node missing method section: {}", item);
				return UNKNOWN_TEXT;
			}

			return textService.getThrowsTextProvider(workspace, resource, bundle,
					declaringClass, declaringMethod, throwsPath.getValue()).makeText();
		} else if (item instanceof CatchPathNode catchPath) {
			ClassBundle<? extends ClassInfo> bundle = catchPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Catch path node missing bundle section: {}", item);
				return UNKNOWN_TEXT;
			}

			ClassInfo declaringClass = catchPath.getValueOfType(ClassInfo.class);
			if (declaringClass == null) {
				logger.error("Catch path node missing class section: {}", item);
				return UNKNOWN_TEXT;
			}

			MethodMember declaringMethod = catchPath.getValueOfType(MethodMember.class);
			if (declaringMethod == null) {
				logger.error("Catch path node missing method section: {}", item);
				return UNKNOWN_TEXT;
			}

			return textService.getCatchTextProvider(workspace, resource, bundle,
					declaringClass, declaringMethod, catchPath.getValue()).makeText();
		} else if (item instanceof EmbeddedResourceContainerPathNode) {
			return Lang.get("tree.embedded-resources");
		}

		// No text
		return null;
	}

	/**
	 * @param item
	 * 		Item to create graphic for.
	 *
	 * @return Icon for the item represented by the path.
	 */
	@SuppressWarnings("unchecked")
	public Node graphicOf(@Nonnull PathNode<?> item) {
		Workspace workspace = item.getValueOfType(Workspace.class);
		WorkspaceResource resource = item.getValueOfType(WorkspaceResource.class);

		if (workspace == null) {
			logger.error("Path node missing workspace section: {}", item);
			return UNKNOWN_GRAPHIC;
		}
		if (resource == null) {
			logger.error("Path node missing resource section: {}", item);
			return UNKNOWN_GRAPHIC;
		}

		if (item instanceof ClassPathNode classPath) {
			ClassBundle<?> bundle = classPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Class path node missing bundle section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			ClassInfo info = classPath.getValue();
			if (info.isJvmClass()) {
				return iconService.getJvmClassInfoIconProvider(workspace, resource,
						(JvmClassBundle) bundle, info.asJvmClass()).makeIcon();
			} else if (info.isAndroidClass()) {
				return iconService.getAndroidClassInfoIconProvider(workspace, resource,
						(AndroidClassBundle) bundle, info.asAndroidClass()).makeIcon();
			}
		} else if (item instanceof FilePathNode filePath) {
			FileBundle bundle = filePath.getValueOfType(FileBundle.class);
			if (bundle == null) {
				logger.error("File path node missing bundle section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			FileInfo info = filePath.getValue();
			return iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
		} else if (item instanceof ClassMemberPathNode memberNode) {
			ClassBundle<?> bundle = memberNode.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Member path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = memberNode.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Member path node missing class section: {}", item);
				return null;
			}

			ClassMember member = memberNode.getValue();
			return iconService.getClassMemberIconProvider(workspace, resource, bundle, classInfo, member).makeIcon();
		} else if (item instanceof DirectoryPathNode directoryPath) {
			Bundle<?> bundle = directoryPath.getValueOfType(Bundle.class);
			if (bundle == null) {
				logger.error("Directory/package path node missing bundle section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			if (bundle instanceof FileBundle fileBundle) {
				return iconService.getDirectoryIconProvider(workspace, resource, fileBundle, directoryPath.getValue()).makeIcon();
			} else if (bundle instanceof ClassBundle<?> classBundle) {
				return iconService.getPackageIconProvider(workspace, resource, classBundle, directoryPath.getValue()).makeIcon();
			}
		} else if (item instanceof InnerClassPathNode innerClassPath) {
			ClassBundle<? extends ClassInfo> bundle = innerClassPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Inner class path node missing bundle section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			ClassInfo outerClass = innerClassPath.getValueOfType(ClassInfo.class);
			if (outerClass == null) {
				logger.error("Inner class path node missing outer class section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			InnerClassInfo innerClass = innerClassPath.getValue();
			return iconService.getInnerClassInfoIconProvider(workspace, resource,
					bundle, outerClass.asJvmClass(), innerClass).makeIcon();
		} else if (item instanceof AnnotationPathNode annotationPath) {
			ClassBundle<? extends ClassInfo> bundle = annotationPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Annotation path node missing bundle section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			Annotated annotated = annotationPath.getValueOfType(Annotated.class);
			if (annotated == null) {
				logger.error("Annotation path node missing annotated element section: {}", item);
				return UNKNOWN_GRAPHIC;
			}

			AnnotationInfo annotation = annotationPath.getValue();
			return iconService.getAnnotationIconProvider(workspace, resource, bundle, annotated, annotation).makeIcon();
		} else if (item instanceof BundlePathNode bundlePath) {
			return iconService.getBundleIconProvider(workspace, resource, bundlePath.getValue()).makeIcon();
		} else if (item instanceof ResourcePathNode) {
			return iconService.getResourceIconProvider(workspace, resource).makeIcon();
		} else if (item instanceof InstructionPathNode insnPath) {
			ClassBundle<?> bundle = insnPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Instruction path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = insnPath.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Instruction path node missing class section: {}", item);
				return null;
			}

			MethodMember method = insnPath.getValueOfType(MethodMember.class);
			if (method == null) {
				logger.error("Instruction path node missing method section: {}", item);
				return null;
			}

			AbstractInsnNode insn = insnPath.getValue();
			return iconService.getInstructionIconProvider(workspace, resource, bundle, classInfo, method, insn).makeIcon();
		} else if (item instanceof LocalVariablePathNode varPath) {
			ClassBundle<?> bundle = varPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Local var path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = varPath.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Local var path node missing class section: {}", item);
				return null;
			}

			MethodMember method = varPath.getValueOfType(MethodMember.class);
			if (method == null) {
				logger.error("Local var path node missing method section: {}", item);
				return null;
			}

			LocalVariable local = varPath.getValue();
			return iconService.getVariableIconProvider(workspace, resource, bundle, classInfo, method, local).makeIcon();
		} else if (item instanceof ThrowsPathNode throwsPath) {
			ClassBundle<?> bundle = throwsPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Throws path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = throwsPath.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Throws path node missing class section: {}", item);
				return null;
			}

			MethodMember method = throwsPath.getValueOfType(MethodMember.class);
			if (method == null) {
				logger.error("Throws path node missing method section: {}", item);
				return null;
			}

			String thrown = throwsPath.getValue();
			return iconService.getThrowsIconProvider(workspace, resource, bundle, classInfo, method, thrown).makeIcon();
		} else if (item instanceof CatchPathNode catchPath) {
			ClassBundle<?> bundle = catchPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Catch path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = catchPath.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Catch path node missing class section: {}", item);
				return null;
			}

			MethodMember method = catchPath.getValueOfType(MethodMember.class);
			if (method == null) {
				logger.error("Catch path node missing method section: {}", item);
				return null;
			}

			String caught = catchPath.getValue();
			return iconService.getCatchIconProvider(workspace, resource, bundle, classInfo, method, caught).makeIcon();
		} else if (item instanceof EmbeddedResourceContainerPathNode) {
			return new FontIconView(CarbonIcons.CATEGORIES);
		}

		// No graphic
		return null;
	}

	/**
	 * @param cell
	 * 		Cell to apply created context menus to.
	 * @param item
	 * 		Content within the cell.
	 * @param source
	 * 		Origin source of the cell, for context menu specialization.
	 *
	 * @return An event handler for {@link Node#setOnMouseClicked(EventHandler)} that handles creating context menus.
	 */
	@Nonnull
	public EventHandler<? super MouseEvent> contextMenuHandlerOf(@Nonnull Cell<?> cell, @Nonnull PathNode<?> item, @Nonnull ContextSource source) {
		return e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				// Lazily populate context menus when secondary click is prompted.
				if (cell.getContextMenu() == null) cell.setContextMenu(contextMenuOf(source, item));
			} else {
				// Handle primary mouse actions.
				if (cell instanceof TreeCell<?> treeCell) {
					if (e.getButton() == MouseButton.PRIMARY) {
						// Double-clicking leafs should 'open' their content.
						// Branches should recursively open.
						TreeItem<?> treeItem = treeCell.getTreeItem();
						if (e.getClickCount() == 2 && treeItem != null)
							if (treeItem.isLeaf())
								openPath(item);
							else if (treeItem.isExpanded()) // Looks odd, but results in less rapid re-closures
								TreeItems.recurseOpen(treeItem);
							else
								TreeItems.recurseClose(treeCell.getTreeView(), treeItem);
					}
				}
			}
		};
	}

	/**
	 * @param source
	 * 		Origin source of the cell, for context menu specialization.
	 * @param item
	 * 		Item to create a context-menu for.
	 *
	 * @return Context-menu for the item represented by the path.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public ContextMenu contextMenuOf(@Nonnull ContextSource source, @Nonnull PathNode<?> item) {
		Workspace workspace = item.getValueOfType(Workspace.class);
		WorkspaceResource resource = item.getValueOfType(WorkspaceResource.class);

		if (workspace == null) {
			logger.error("Path node missing workspace section: {}", item);
			return null;
		}
		if (resource == null) {
			logger.error("Path node missing resource section: {}", item);
			return null;
		}

		if (item instanceof ClassPathNode classPath) {
			ClassBundle<?> bundle = classPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Class path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo info = classPath.getValue();
			if (info.isJvmClass()) {
				return contextMenuService.getJvmClassInfoContextMenuProvider(source, workspace, resource,
						(JvmClassBundle) bundle, info.asJvmClass()).makeMenu();
			} else if (info.isAndroidClass()) {
				return contextMenuService.getAndroidClassInfoContextMenuProvider(source, workspace, resource,
						(AndroidClassBundle) bundle, info.asAndroidClass()).makeMenu();
			}
		} else if (item instanceof FilePathNode filePath) {
			FileBundle bundle = filePath.getValueOfType(FileBundle.class);
			if (bundle == null) {
				logger.error("File path node missing bundle section: {}", item);
				return null;
			}

			FileInfo info = filePath.getValue();
			return contextMenuService.getFileInfoContextMenuProvider(source, workspace, resource, bundle, info).makeMenu();
		} else if (item instanceof ClassMemberPathNode memberNode) {
			ClassBundle<?> bundle = memberNode.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Member path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo classInfo = memberNode.getValueOfType(ClassInfo.class);
			if (classInfo == null) {
				logger.error("Member path node missing class section: {}", item);
				return null;
			}

			ClassMember member = memberNode.getValue();
			return contextMenuService.getClassMemberContextMenuProvider(source, workspace, resource, bundle, classInfo, member).makeMenu();
		} else if (item instanceof DirectoryPathNode directoryPath) {
			Bundle<?> bundle = directoryPath.getValueOfType(Bundle.class);
			if (bundle == null) {
				logger.error("Directory/package path node missing bundle section: {}", item);
				return null;
			}

			if (bundle instanceof FileBundle fileBundle) {
				return contextMenuService.getDirectoryContextMenuProvider(source, workspace, resource, fileBundle, directoryPath.getValue()).makeMenu();
			} else if (bundle instanceof ClassBundle<?> classBundle) {
				return contextMenuService.getPackageContextMenuProvider(source, workspace, resource, classBundle, directoryPath.getValue()).makeMenu();
			}
		} else if (item instanceof InnerClassPathNode innerClassPath) {
			ClassBundle<? extends ClassInfo> bundle = innerClassPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Inner class path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo outerClass = innerClassPath.getValueOfType(ClassInfo.class);
			if (outerClass == null) {
				logger.error("Inner class path node missing outer class section: {}", item);
				return null;
			}

			InnerClassInfo innerClass = innerClassPath.getValue();
			return contextMenuService.getInnerClassInfoContextMenuProvider(source, workspace, resource,
					bundle, outerClass.asJvmClass(), innerClass).makeMenu();
		} else if (item instanceof AnnotationPathNode annotationPath) {
			ClassBundle<? extends ClassInfo> bundle = annotationPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Annotation path node missing bundle section: {}", item);
				return null;
			}

			Annotated annotated = annotationPath.getValueOfType(Annotated.class);
			if (annotated == null) {
				logger.error("Annotation path node missing annotated element section: {}", item);
				return null;
			}

			AnnotationInfo annotation = annotationPath.getValue();
			return contextMenuService.getAnnotationContextMenuProvider(source, workspace, resource, bundle, annotated, annotation).makeMenu();
		} else if (item instanceof BundlePathNode bundlePath) {
			return contextMenuService.getBundleContextMenuProvider(source, workspace, resource, bundlePath.getValue()).makeMenu();
		} else if (item instanceof ResourcePathNode) {
			return contextMenuService.getResourceContextMenuProvider(source, workspace, resource).makeMenu();
		} else if (item instanceof AssemblerPathNode assemblerPath) {
			ClassBundle<?> bundle = assemblerPath.getValueOfType(ClassBundle.class);
			if (bundle == null) {
				logger.error("Assembler path node missing bundle section: {}", item);
				return null;
			}

			ClassInfo declaring = assemblerPath.getValueOfType(ClassInfo.class);
			if (declaring == null) {
				logger.error("Assembler path node missing declaring class section: {}", item);
				return null;
			}

			return contextMenuService.getAssemblerContextMenuProvider(source, workspace, resource, bundle, declaring, assemblerPath.getValue()).makeMenu();
		}

		// No menu
		return null;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CellConfigurationServiceConfig getServiceConfig() {
		return config;
	}
}
