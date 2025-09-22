package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.LineNumberPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.graph.MethodCallGraphsPane;
import software.coley.recaf.ui.control.popup.AddMemberPopup;
import software.coley.recaf.ui.control.popup.ItemListSelectionPopup;
import software.coley.recaf.ui.control.popup.ItemTreeSelectionPopup;
import software.coley.recaf.ui.control.popup.NamePopup;
import software.coley.recaf.ui.control.popup.OverrideMethodPopup;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.pane.CommentEditPane;
import software.coley.recaf.ui.pane.CommentListPane;
import software.coley.recaf.ui.pane.DocumentationPane;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.ui.pane.editing.FileDisplayMode;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.android.AndroidClassEditorType;
import software.coley.recaf.ui.pane.editing.android.AndroidClassPane;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassEditorType;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassPane;
import software.coley.recaf.ui.pane.search.AbstractSearchPane;
import software.coley.recaf.ui.pane.search.ClassReferenceSearchPane;
import software.coley.recaf.ui.pane.search.InstructionSearchPane;
import software.coley.recaf.ui.pane.search.MemberDeclarationSearchPane;
import software.coley.recaf.ui.pane.search.MemberReferenceSearchPane;
import software.coley.recaf.ui.pane.search.NumberSearchPane;
import software.coley.recaf.ui.pane.search.StringSearchPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.visitors.ClassAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.FieldAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.FieldPredicate;
import software.coley.recaf.util.visitors.MemberCopyingVisitor;
import software.coley.recaf.util.visitors.MemberRemovingVisitor;
import software.coley.recaf.util.visitors.MemberStubAddingVisitor;
import software.coley.recaf.util.visitors.MethodAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.MethodNoopingVisitor;
import software.coley.recaf.util.visitors.MethodPredicate;
import software.coley.recaf.workspace.PathExportingManager;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static software.coley.collections.Unchecked.cast;
import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.*;
import static software.coley.recaf.util.StringUtil.*;

/**
 * Common actions integration.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class Actions implements Service {
	public static final String ID = "actions";
	private static final Logger logger = Logging.get(Actions.class);
	private final WorkspaceManager workspaceManager;
	private final NavigationManager navigationManager;
	private final DockingManager dockingManager;
	private final WindowFactory windowFactory;
	private final TextProviderService textService;
	private final IconProviderService iconService;
	private final CellConfigurationService cellConfigurationService;
	private final PathExportingManager pathExportingManager;
	private final MappingApplierService mappingApplierService;
	private final InheritanceGraphService inheritanceGraphService;
	private final Instance<JvmClassPane> jvmPaneProvider;
	private final Instance<AndroidClassPane> androidPaneProvider;
	private final Instance<FilePane> filePaneProvider;
	private final Instance<AssemblerPane> assemblerPaneProvider;
	private final Instance<WorkspaceInformationPane> infoPaneProvider;
	private final Instance<CommentEditPane> commentPaneProvider;
	private final Instance<CommentListPane> commentListPaneProvider;
	private final Instance<MethodCallGraphsPane> callGraphsPaneProvider;
	private final Instance<StringSearchPane> stringSearchPaneProvider;
	private final Instance<NumberSearchPane> numberSearchPaneProvider;
	private final Instance<ClassReferenceSearchPane> classReferenceSearchPaneProvider;
	private final Instance<MemberReferenceSearchPane> memberReferenceSearchPaneProvider;
	private final Instance<MemberDeclarationSearchPane> memberDeclarationSearchPaneProvider;
	private final Instance<InstructionSearchPane> instructionSearchPaneProvider;
	private final KeybindingConfig keybindingConfig;
	private final ActionsConfig config;

	@Inject
	public Actions(@Nonnull ActionsConfig config,
	               @Nonnull KeybindingConfig keybindingConfig,
	               @Nonnull WorkspaceManager workspaceManager,
	               @Nonnull NavigationManager navigationManager,
	               @Nonnull DockingManager dockingManager,
	               @Nonnull WindowFactory windowFactory,
	               @Nonnull TextProviderService textService,
	               @Nonnull IconProviderService iconService,
	               @Nonnull CellConfigurationService cellConfigurationService,
	               @Nonnull PathExportingManager pathExportingManager,
	               @Nonnull MappingApplierService mappingApplierService,
	               @Nonnull InheritanceGraphService inheritanceGraphService,
	               @Nonnull Instance<MappingApplier> applierProvider,
	               @Nonnull Instance<JvmClassPane> jvmPaneProvider,
	               @Nonnull Instance<AndroidClassPane> androidPaneProvider,
	               @Nonnull Instance<FilePane> filePaneProvider,
	               @Nonnull Instance<AssemblerPane> assemblerPaneProvider,
	               @Nonnull Instance<WorkspaceInformationPane> infoPaneProvider,
	               @Nonnull Instance<CommentEditPane> commentPaneProvider,
	               @Nonnull Instance<CommentListPane> commentListPaneProvider,
	               @Nonnull Instance<StringSearchPane> stringSearchPaneProvider,
	               @Nonnull Instance<NumberSearchPane> numberSearchPaneProvider,
	               @Nonnull Instance<MethodCallGraphsPane> callGraphsPaneProvider,
	               @Nonnull Instance<ClassReferenceSearchPane> classReferenceSearchPaneProvider,
	               @Nonnull Instance<MemberReferenceSearchPane> memberReferenceSearchPaneProvider,
	               @Nonnull Instance<MemberDeclarationSearchPane> memberDeclarationSearchPaneProvider,
	               @Nonnull Instance<InstructionSearchPane> instructionSearchPaneProvider) {
		this.config = config;
		this.keybindingConfig = keybindingConfig;
		this.workspaceManager = workspaceManager;
		this.navigationManager = navigationManager;
		this.dockingManager = dockingManager;
		this.windowFactory = windowFactory;
		this.textService = textService;
		this.iconService = iconService;
		this.cellConfigurationService = cellConfigurationService;
		this.pathExportingManager = pathExportingManager;
		this.mappingApplierService = mappingApplierService;
		this.inheritanceGraphService = inheritanceGraphService;
		this.jvmPaneProvider = jvmPaneProvider;
		this.androidPaneProvider = androidPaneProvider;
		this.filePaneProvider = filePaneProvider;
		this.assemblerPaneProvider = assemblerPaneProvider;
		this.infoPaneProvider = infoPaneProvider;
		this.commentPaneProvider = commentPaneProvider;
		this.commentListPaneProvider = commentListPaneProvider;
		this.stringSearchPaneProvider = stringSearchPaneProvider;
		this.numberSearchPaneProvider = numberSearchPaneProvider;
		this.callGraphsPaneProvider = callGraphsPaneProvider;
		this.classReferenceSearchPaneProvider = classReferenceSearchPaneProvider;
		this.memberReferenceSearchPaneProvider = memberReferenceSearchPaneProvider;
		this.memberDeclarationSearchPaneProvider = memberDeclarationSearchPaneProvider;
		this.instructionSearchPaneProvider = instructionSearchPaneProvider;
	}

	/**
	 * Brings a {@link Navigable} component representing a class/file into focus.
	 * If no such component exists, one is created.
	 * <br>
	 * Automatically calls the type-specific goto-declaration handling.
	 *
	 * @param path
	 * 		Path to class or file to open.
	 *
	 * @return Navigable content representing content of the path.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	@Nonnull
	public Navigable gotoDeclaration(@Nonnull PathNode<?> path) throws IncompletePathException {
		if (path instanceof ClassPathNode classPath) return gotoDeclaration(classPath);
		else if (path instanceof FilePathNode filePath) return gotoDeclaration(filePath);
		else if (path instanceof ClassMemberPathNode classMemberPath) {
			ClassPathNode parent = classMemberPath.getParent();
			if (parent == null)
				throw new IncompletePathException(ClassInfo.class);
			ClassNavigable navigable = gotoDeclaration(parent);
			navigable.requestFocus(classMemberPath.getValue());
			return navigable;
		} else if (path instanceof LineNumberPathNode lineNumberPath) {
			FilePathNode parent = lineNumberPath.getParent();
			if (parent == null)
				throw new IncompletePathException(FileInfo.class);
			return gotoDeclaration(parent);
		}
		throw new IncompletePathException(path.getValueType());
	}

	/**
	 * Brings a {@link ClassNavigable} component representing the given class into focus.
	 * If no such component exists, one is created.
	 * <br>
	 * Automatically calls the type-specific goto-declaration handling.
	 *
	 * @param path
	 * 		Path containing a class to open.
	 *
	 * @return Navigable content representing class content of the path.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	@Nonnull
	public ClassNavigable gotoDeclaration(@Nonnull ClassPathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		ClassBundle<?> bundle = path.getValueOfType(ClassBundle.class);
		ClassInfo info = path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing workspace in path", info.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing resource in path", info.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing bundle in path", info.getName());
			throw new IncompletePathException(ClassBundle.class);
		}

		// Handle JVM vs Android
		if (info.isJvmClass()) {
			return gotoDeclaration(workspace, resource, (JvmClassBundle) bundle, info.asJvmClass());
		} else if (info.isAndroidClass()) {
			return gotoDeclaration(workspace, resource, (AndroidClassBundle) bundle, info.asAndroidClass());
		}
		throw new UnsupportedContentException("Unsupported class type: " + info.getClass().getName());
	}

	/**
	 * Brings a {@link ClassNavigable} component representing the given class into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to go to.
	 *
	 * @return Navigable content representing class content of the path.
	 */
	@Nonnull
	public ClassNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                      @Nonnull WorkspaceResource resource,
	                                      @Nonnull JvmClassBundle bundle,
	                                      @Nonnull JvmClassInfo info) {
		ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, info);
		return (ClassNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getJvmClassInfoTextProvider(workspace, resource, bundle, info).makeText();
			IconProvider iconProvider = iconService.getJvmClassInfoIconProvider(workspace, resource, bundle, info);
			DockableIconFactory graphicFactory = d -> Objects.requireNonNull(iconProvider.makeIcon(), "Missing graphic");
			if (title == null) throw new IllegalStateException("Missing title");

			// Create content for the tab.
			JvmClassPane content = jvmPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), title, graphicFactory, content);
			dockable.addCloseListener((_, _) -> jvmPaneProvider.destroy(content));
			content.addPathUpdateListener(updatedPath -> {
				// Update tab graphic in case backing class details change.
				JvmClassInfo updatedInfo = updatedPath.getValue().asJvmClass();
				String updatedTitle = textService.getJvmClassInfoTextProvider(workspace, resource, bundle, updatedInfo).makeText();
				IconProvider updatedIconProvider = iconService.getJvmClassInfoIconProvider(workspace, resource, bundle, updatedInfo);
				DockableIconFactory updatedGraphicFactory = d -> Objects.requireNonNull(updatedIconProvider.makeIcon(), "Missing graphic");
				FxThreadUtil.run(() -> {
					dockable.setTitle(updatedTitle);
					dockable.setIconFactory(updatedGraphicFactory);
				});
			});
			setupInfoContextMenu(info, content, dockable);
			return dockable;
		});
	}

	/**
	 * Brings a {@link ClassNavigable} component representing the given class into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to go to.
	 *
	 * @return Navigable content representing class content of the path.
	 */
	@Nonnull
	public ClassNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                      @Nonnull WorkspaceResource resource,
	                                      @Nonnull AndroidClassBundle bundle,
	                                      @Nonnull AndroidClassInfo info) {
		ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, info);
		return (ClassNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getAndroidClassInfoTextProvider(workspace, resource, bundle, info).makeText();
			IconProvider iconProvider = iconService.getAndroidClassInfoIconProvider(workspace, resource, bundle, info);
			DockableIconFactory graphicFactory = d -> Objects.requireNonNull(iconProvider.makeIcon(), "Missing graphic");
			if (title == null) throw new IllegalStateException("Missing title");

			// Create content for the tab.
			AndroidClassPane content = androidPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), title, graphicFactory, content);
			dockable.addCloseListener((_, _) -> androidPaneProvider.destroy(content));
			content.addPathUpdateListener(updatedPath -> {
				// Update tab graphic in case backing class details change.
				AndroidClassInfo updatedInfo = updatedPath.getValue().asAndroidClass();
				String updatedTitle = textService.getAndroidClassInfoTextProvider(workspace, resource, bundle, updatedInfo).makeText();
				IconProvider updatedIconProvider = iconService.getAndroidClassInfoIconProvider(workspace, resource, bundle, updatedInfo);
				DockableIconFactory updatedGraphicFactory = d -> Objects.requireNonNull(updatedIconProvider.makeIcon(), "Missing graphic");
				FxThreadUtil.run(() -> {
					dockable.setTitle(updatedTitle);
					dockable.setIconFactory(updatedGraphicFactory);
				});
			});
			setupInfoContextMenu(info, content, dockable);
			return dockable;
		});
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given file into focus.
	 * If no such component exists, one is created.
	 * <br>
	 * Automatically calls the type-specific goto-declaration handling.
	 *
	 * @param path
	 * 		Path containing a file to open.
	 *
	 * @return Navigable content representing file content of the path.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull FilePathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		FileBundle bundle = path.getValueOfType(FileBundle.class);
		FileInfo info = path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing workspace in path", info.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing resource in path", info.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing bundle in path", info.getName());
			throw new IncompletePathException(ClassBundle.class);
		}

		return gotoDeclaration(workspace, resource, bundle, info.asFile());
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to go to.
	 *
	 * @return Navigable content representing file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull FileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			IconProvider iconProvider = iconService.getFileInfoIconProvider(workspace, resource, bundle, info);
			DockableIconFactory graphicFactory = d -> Objects.requireNonNull(iconProvider.makeIcon(), "Missing graphic");
			if (title == null) throw new IllegalStateException("Missing title");

			// Create content for the tab.
			FilePane content = filePaneProvider.get();
			content.setupForFileType(info);
			content.onUpdatePath(path);

			// Build the tab.
			Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), title, graphicFactory, content);
			dockable.addCloseListener((_, _) -> filePaneProvider.destroy(content));
			setupInfoContextMenu(info, content, dockable, menu -> addFilePaneOptions(menu, content));
			return dockable;
		});
	}

	/**
	 * Prompts the user to document the given class into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to document.
	 */
	public void openCommentEditing(@Nonnull Workspace workspace,
	                               @Nonnull WorkspaceResource resource,
	                               @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                               @Nonnull ClassInfo info) {
		createContent(() -> {
			ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, info);

			// Create text/graphic for the tab to create.
			String title = textService.getClassInfoTextProvider(workspace, resource, bundle, info).makeText();
			DockableIconFactory graphicFactory = d -> new FontIconView(CarbonIcons.BOOKMARK_FILLED);
			if (title == null) throw new IllegalStateException("Missing title");
			return createCommentEditDockable(path, title, graphicFactory, info);
		});
	}

	/**
	 * Prompts the user to document the given class into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param member
	 * 		Member to document.
	 */
	public void openCommentEditing(@Nonnull Workspace workspace,
	                               @Nonnull WorkspaceResource resource,
	                               @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                               @Nonnull ClassInfo declaringClass,
	                               @Nonnull ClassMember member) {
		createContent(() -> {
			ClassMemberPathNode path = PathNodes.memberPath(workspace, resource, bundle, declaringClass, member);

			// Create text/graphic for the tab to create.
			ClassInfo classInfo = path.getParent().getValue();
			String title = textService.getMemberTextProvider(workspace, resource, bundle, classInfo, member).makeText();
			DockableIconFactory graphicFactory = d -> new FontIconView(CarbonIcons.BOOKMARK_FILLED);
			if (title == null) throw new IllegalStateException("Missing title");
			return createCommentEditDockable(path, title, graphicFactory, classInfo);
		});
	}

	@Nonnull
	private Dockable createCommentEditDockable(@Nonnull PathNode<?> path, @Nonnull String title,
	                                           @Nonnull DockableIconFactory graphicFactory, @Nonnull ClassInfo classInfo) {
		// Create content for the dockable.
		CommentEditPane content = commentPaneProvider.get();
		content.onUpdatePath(path);

		// Place the tab in a region with other comments if possible.
		DockablePath searchPath = dockingManager.getBento()
				.search().dockable(d -> d.getNode() instanceof DocumentationPane);
		DockContainerLeaf container = searchPath == null ? dockingManager.getPrimaryDockingContainer() : searchPath.leafContainer();

		// Build the dockable.
		Dockable dockable = createDockable(container, title, graphicFactory, content);
		dockable.addCloseListener((_, _) -> commentPaneProvider.destroy(content));
		container.addDockable(dockable);
		dockable.setContextMenuFactory(d -> {
			ContextMenu menu = new ContextMenu();
			addCloseActions(menu, dockable);
			return menu;
		});

		selectTab(content);
		content.requestFocus();

		return dockable;
	}

	public void openCommentList() {
		// Check for tabs with the panel already open.
		DockablePath docPanePath = null;
		for (DockablePath path : dockingManager.getBento().search().allDockables()) {
			Dockable dockable = path.dockable();
			Node node = dockable.nodeProperty().get();
			if (node instanceof CommentListPane) {
				path.leafContainer().selectDockable(dockable);
				FxThreadUtil.run(() -> {
					node.requestFocus();
					Animations.animateNotice(node, 1000);
				});
				return;
			} else if (node instanceof DocumentationPane) {
				docPanePath = path;
			}
		}

		// Not already open, gotta open a new one.
		DockContainerLeaf container = docPanePath != null ? docPanePath.leafContainer() : dockingManager.getPrimaryDockingContainer();
		CommentListPane content = commentListPaneProvider.get();
		Dockable dockable = dockingManager.newTranslatableDockable("menu.analysis.list-comments", CarbonIcons.CHAT, content);
		dockable.addCloseListener((_, _) -> commentListPaneProvider.destroy(content));
		container.addDockable(dockable);

		container.selectDockable(dockable);
		content.requestFocus();
	}

	/**
	 * Display the workspace summary / current information.
	 */
	public void openSummary() {
		WorkspaceInformationPane content = infoPaneProvider.get();
		Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), getBinding("workspace.info"),
				d -> new FontIconView(CarbonIcons.INFORMATION), content);
		dockable.addCloseListener((_, _) -> infoPaneProvider.destroy(content));
		dockable.setContextMenuFactory(d -> {
			ContextMenu menu = new ContextMenu();
			addCloseActions(menu, d);
			return menu;
		});
	}

	/**
	 * Prompts the user to select a package to move the given class into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to move into a different package.
	 */
	public void moveClass(@Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource,
	                      @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo info) {
		boolean isRootDirectory = isNullOrEmpty(info.getPackageName());
		ItemTreeSelectionPopup.forPackageNames(bundle, packages -> {
					// We only allow a single package, so the list should contain just one item.
					String oldPackage = isRootDirectory ? "" : info.getPackageName() + "/";
					String newPackage = packages.get(0);
					if (Objects.equals(oldPackage, newPackage)) return;
					if (!newPackage.isEmpty()) newPackage += "/";

					// Create mapping for the class and any inner classes.
					String originalName = info.getName();
					String newName = replacePrefix(originalName, oldPackage, newPackage);
					IntermediateMappings mappings = new IntermediateMappings();
					for (InnerClassInfo inner : info.getInnerClasses()) {
						if (inner.isExternalReference()) continue;
						String innerClassName = inner.getInnerClassName();
						mappings.addClass(innerClassName, newName + innerClassName.substring(originalName.length()));
					}

					// Apply the mappings.
					MappingApplier applier = mappingApplierService.inWorkspace(workspace);
					MappingResults results = applier.applyToPrimaryResource(mappings);
					results.apply();
				})
				.withTitle(Lang.getBinding("dialog.title.move-class"))
				.withTextMapping(name -> textService.getPackageTextProvider(workspace, resource, bundle, name).makeText())
				.withGraphicMapping(name -> iconService.getPackageIconProvider(workspace, resource, bundle, name).makeIcon())
				.show();
	}

	/**
	 * Prompts the user to select a directory to move the given file into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to move into a different directory.
	 */
	public void moveFile(@Nonnull Workspace workspace,
	                     @Nonnull WorkspaceResource resource,
	                     @Nonnull FileBundle bundle,
	                     @Nonnull FileInfo info) {
		boolean isRootDirectory = isNullOrEmpty(info.getDirectoryName());
		ItemTreeSelectionPopup.forDirectoryNames(bundle, chosenDirectories -> {
					// We only allow a single directory, so the list should contain just one item.
					if (chosenDirectories.isEmpty()) return;
					String oldDirectoryName = isRootDirectory ? "" : info.getDirectoryName() + "/";
					String newDirectoryName = chosenDirectories.get(0);
					if (Objects.equals(oldDirectoryName, newDirectoryName)) return;
					if (!newDirectoryName.isEmpty()) newDirectoryName += "/";

					String newName = replacePrefix(info.getName(), oldDirectoryName, newDirectoryName);

					bundle.remove(info.getName());
					bundle.put(info.toFileBuilder().withName(newName).build());
				}).withTitle(Lang.getBinding("dialog.title.move-file"))
				.withTextMapping(name -> textService.getDirectoryTextProvider(workspace, resource, bundle, name).makeText())
				.withGraphicMapping(name -> iconService.getDirectoryIconProvider(workspace, resource, bundle, name).makeIcon())
				.show();
	}

	/**
	 * Prompts the user to select a package to move the given package into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Package to go move into another package as a sub-package.
	 */
	public void movePackage(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle,
	                        @Nonnull String packageName) {
		boolean isRootDirectory = packageName.isEmpty();
		ItemTreeSelectionPopup.forPackageNames(bundle, chosenPackages -> {
					if (chosenPackages.isEmpty()) return;
					String newPackageName = chosenPackages.get(0);
					if (packageName.equals(newPackageName)) return;

					// Create mappings for classes in the given package.
					IntermediateMappings mappings = new IntermediateMappings();
					String newPrefix = (newPackageName.isEmpty() ? "" : newPackageName + "/") + shortenPath(packageName) + "/";
					if (isRootDirectory) {
						// Source is default package
						for (JvmClassInfo info : bundle.values()) {
							String name = info.getName();
							if (name.indexOf('/') != -1) {
								mappings.addClass(name, newPrefix + name);
							}
						}
					} else {
						// Source is another package
						String oldPrefix = packageName + "/";
						for (JvmClassInfo info : bundle.values()) {
							String name = info.getName();
							if (newPackageName.isEmpty() && name.indexOf('/') == -1) {
								// Target is default package
								mappings.addClass(name, shortenPath(name));
							} else if (name.startsWith(oldPrefix)) {
								// Target is some package, replace prefix
								mappings.addClass(name, replacePrefix(name, oldPrefix, newPrefix));
							}
						}
					}

					// Apply the mappings.
					MappingApplier applier = mappingApplierService.inWorkspace(workspace);
					MappingResults results = applier.applyToPrimaryResource(mappings);
					results.apply();
				}).withTitle(Lang.getBinding("dialog.title.move-package"))
				.withTextMapping(name -> textService.getPackageTextProvider(workspace, resource, bundle, name).makeText())
				.withGraphicMapping(name -> iconService.getPackageIconProvider(workspace, resource, bundle, name).makeIcon())
				.show();
	}

	/**
	 * Prompts the user to select a directory to move the given directory into.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Directory to go move into another directory as a sub-directory.
	 */
	public void moveDirectory(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull FileBundle bundle,
	                          @Nonnull String directoryName) {
		boolean isRootDirectory = directoryName.isEmpty();
		String localDirectoryName = shortenPath(directoryName);
		ItemTreeSelectionPopup.forDirectoryNames(bundle, chosenDirectories -> {
					if (chosenDirectories.isEmpty()) return;
					String newDirectoryName = chosenDirectories.get(0);
					if (directoryName.equals(newDirectoryName)) return;

					String prefix = directoryName + "/";
					for (FileInfo value : bundle.valuesAsCopy()) {
						String filePath = value.getName();
						String fileName = shortenPath(filePath);

						// Source is root directory, this file is also in the root directory
						if (isRootDirectory && filePath.indexOf('/') == -1) {
							String name = newDirectoryName + "/" + fileName;
							bundle.remove(filePath);
							bundle.put(value.toFileBuilder().withName(name).build());
						} else {
							// Source is another package, this file matches that package
							if (filePath.startsWith(prefix)) {
								String name;
								if (newDirectoryName.isEmpty()) {
									// Target is root directory
									name = localDirectoryName + "/" + fileName;
								} else if (filePath.startsWith(directoryName)) {
									// Target is another directory
									name = replacePrefix(filePath, directoryName, newDirectoryName + "/" + localDirectoryName);
								} else {
									continue;
								}
								bundle.remove(filePath);
								bundle.put(value.toFileBuilder().withName(name).build());
							}
						}
					}
				}).withTitle(Lang.getBinding("dialog.title.move-directory"))
				.withTextMapping(name -> textService.getDirectoryTextProvider(workspace, resource, bundle, name).makeText())
				.withGraphicMapping(name -> iconService.getDirectoryIconProvider(workspace, resource, bundle, name).makeIcon())
				.show();
	}

	/**
	 * Prompts the user to rename whatever sort of content is contained within the given path.
	 *
	 * @param path
	 * 		Item to rename. Can be a number of values.
	 */
	public void rename(@Nonnull PathNode<?> path) {
		// Handle renaming based on the different resolved content type.
		if (path instanceof ClassPathNode classPath)
			Unchecked.run(() -> renameClass(classPath));
		else if (path instanceof ClassMemberPathNode memberPathNode)
			if (memberPathNode.isField())
				Unchecked.run(() -> renameField(memberPathNode));
			else
				Unchecked.run(() -> renameMethod(memberPathNode));
		else if (path instanceof FilePathNode filePath)
			Unchecked.run(() -> renameFile(filePath));
		else if (path instanceof DirectoryPathNode directoryPath)
			Unchecked.run(() -> renamePackageOrDirectory(directoryPath));
	}

	/**
	 * Prompts the user to rename the given class.
	 *
	 * @param path
	 * 		Path to class.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	public void renameClass(@Nonnull ClassPathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		ClassBundle<?> bundle = path.getValueOfType(ClassBundle.class);
		ClassInfo info = path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing workspace in path", info.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing resource in path", info.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing bundle in path", info.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		renameClass(workspace, resource, bundle, info);
	}

	/**
	 * Prompts the user to rename the given class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to rename.
	 */
	public void renameClass(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                        @Nonnull ClassInfo info) {
		String originalName = info.getName();
		Consumer<String> renameTask = newName -> {
			// Create mapping for the class and any inner classes.
			IntermediateMappings mappings = new IntermediateMappings();
			mappings.addClass(originalName, newName);
			for (InnerClassInfo inner : info.getInnerClasses()) {
				if (inner.isExternalReference()) continue;
				String innerClassName = inner.getInnerClassName();
				mappings.addClass(innerClassName, newName + innerClassName.substring(originalName.length()));
			}

			// Apply the mappings.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToPrimaryResource(mappings);
			results.apply();
		};
		new NamePopup(renameTask)
				.withInitialPathName(originalName)
				.forClassRename(bundle)
				.show();
	}

	/**
	 * Prompts the user to rename the given field.
	 *
	 * @param path
	 * 		Path to field.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	public void renameField(@Nonnull ClassMemberPathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		ClassBundle<?> bundle = path.getValueOfType(ClassBundle.class);
		ClassInfo declaringClass = path.getValueOfType(ClassInfo.class);
		FieldMember fieldMember = (FieldMember) path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for field '{}', missing workspace in path", fieldMember.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for field '{}', missing resource in path", fieldMember.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for field '{}', missing bundle in path", fieldMember.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		if (declaringClass == null) {
			logger.error("Cannot resolve required path nodes for field '{}', missing class in path", fieldMember.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		renameField(workspace, resource, bundle, declaringClass, fieldMember);
	}

	/**
	 * Prompts the user to rename the given field.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class containing the field.
	 * @param field
	 * 		Field to rename.
	 */
	public void renameField(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                        @Nonnull ClassInfo declaringClass,
	                        @Nonnull FieldMember field) {
		String originalName = field.getName();
		Consumer<String> renameTask = newName -> {
			IntermediateMappings mappings = new IntermediateMappings();
			mappings.addField(declaringClass.getName(), field.getDescriptor(), originalName, newName);

			// Apply the mappings.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToPrimaryResource(mappings);
			results.apply();
		};
		new NamePopup(renameTask)
				.withInitialName(originalName)
				.forFieldRename(declaringClass, field)
				.show();
	}

	/**
	 * Prompts the user to rename the given method.
	 *
	 * @param path
	 * 		Path to method.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	public void renameMethod(@Nonnull ClassMemberPathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		ClassBundle<?> bundle = path.getValueOfType(ClassBundle.class);
		ClassInfo declaringClass = path.getValueOfType(ClassInfo.class);
		MethodMember methodMember = (MethodMember) path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for method '{}', missing workspace in path", methodMember.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for method '{}', missing resource in path", methodMember.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for method '{}', missing bundle in path", methodMember.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		if (declaringClass == null) {
			logger.error("Cannot resolve required path nodes for method '{}', missing class in path", methodMember.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		renameMethod(workspace, resource, bundle, declaringClass, methodMember);
	}

	/**
	 * Prompts the user to rename the given method.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class containing the method.
	 * @param method
	 * 		Method to rename.
	 */
	public void renameMethod(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                         @Nonnull ClassInfo declaringClass,
	                         @Nonnull MethodMember method) {
		String originalName = method.getName();
		Consumer<String> renameTask = newName -> {
			IntermediateMappings mappings = new IntermediateMappings();
			mappings.addMethod(declaringClass.getName(), method.getDescriptor(), originalName, newName);

			// Apply the mappings.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToPrimaryResource(mappings);
			results.apply();
		};
		new NamePopup(renameTask)
				.withInitialName(originalName)
				.forMethodRename(declaringClass, method)
				.show();
	}

	/**
	 * Prompts the user to rename the given field.
	 *
	 * @param path
	 * 		Path to file.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	public void renameFile(@Nonnull FilePathNode path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		FileBundle bundle = path.getValueOfType(FileBundle.class);
		FileInfo info = path.getValue();
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing workspace in path", info.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing resource in path", info.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for file '{}', missing bundle in path", info.getName());
			throw new IncompletePathException(ClassBundle.class);
		}
		renameFile(workspace, resource, bundle, info);
	}

	/**
	 * Prompts the user to rename the given file.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to rename.
	 */
	public void renameFile(@Nonnull Workspace workspace,
	                       @Nonnull WorkspaceResource resource,
	                       @Nonnull FileBundle bundle,
	                       @Nonnull FileInfo info) {
		String name = info.getName();
		new NamePopup(newFileName -> {
			if (name.equals(newFileName)) return;
			bundle.remove(name);
			bundle.put(info.toFileBuilder().withName(newFileName).build());
		}).withInitialPathName(name)
				.forFileRename(bundle)
				.show();
	}

	/**
	 * Prompts the user to rename the given directory.
	 *
	 * @param path
	 * 		Path to directory.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	public void renamePackageOrDirectory(@Nonnull DirectoryPathNode path) throws IncompletePathException {
		String directoryName = path.getValue();

		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		Bundle<?> bundle = path.getValueOfType(Bundle.class);
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for directory '{}', missing workspace in path", directoryName);
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for directory '{}', missing resource in path", directoryName);
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for directory '{}', missing bundle in path", directoryName);
			throw new IncompletePathException(ClassBundle.class);
		}

		if (bundle instanceof FileBundle fileBundle)
			renameDirectory(workspace, resource, fileBundle, directoryName);
		else if (bundle instanceof JvmClassBundle classBundle)
			renamePackage(workspace, resource, classBundle, directoryName);
	}

	/**
	 * Prompts the user to rename the given directory.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Name of directory to rename.
	 */
	public void renameDirectory(@Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull FileBundle bundle,
	                            @Nonnull String directoryName) {
		boolean isRootDirectory = directoryName.isEmpty();
		new NamePopup(newDirectoryName -> {
			if (directoryName.equals(newDirectoryName)) return;

			String prefix = directoryName + "/";
			for (FileInfo value : bundle.valuesAsCopy()) {
				String filePath = value.getName();
				String fileName = shortenPath(filePath);

				// Source is root directory, this file is also in the root directory
				if (isRootDirectory && filePath.indexOf('/') == -1) {
					String name = newDirectoryName + "/" + fileName;
					bundle.remove(filePath);
					bundle.put(value.toFileBuilder().withName(name).build());
				} else {
					// Source is another package, this file matches that package
					if (filePath.startsWith(prefix)) {
						String name;
						if (newDirectoryName.isEmpty()) {
							// Target is root directory
							name = fileName;
						} else if (filePath.startsWith(directoryName)) {
							// Target is another directory
							name = replacePrefix(filePath, directoryName, newDirectoryName);
						} else {
							continue;
						}
						bundle.remove(filePath);
						bundle.put(value.toFileBuilder().withName(name).build());
					}
				}
			}
		}).withInitialPathName(directoryName)
				.forDirectoryRename(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a new name for the copied package.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Name of directory to copy.
	 */
	public void renamePackage(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull JvmClassBundle bundle,
	                          @Nonnull String packageName) {
		boolean isRootDirectory = packageName.isEmpty();
		new NamePopup(newPackageName -> {
			// Create mappings.
			String oldPrefix = isRootDirectory ? "" : packageName + "/";
			String newPrefix = newPackageName + "/";
			IntermediateMappings mappings = new IntermediateMappings();
			for (JvmClassInfo info : bundle.valuesAsCopy()) {
				String className = info.getName();
				if (isRootDirectory) {
					// Source is the default package
					if (className.indexOf('/') == -1)
						// Class is in the default package
						mappings.addClass(className, newPackageName + '/' + className);
				} else if (className.startsWith(oldPrefix))
					// Class starts with the package prefix
					mappings.addClass(className, replacePrefix(className, oldPrefix, newPrefix));
			}

			// Apply mappings to create copies of the affected classes, using the provided name.
			// Then dump the mapped classes into bundle.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToPrimaryResource(mappings);
			results.apply();
		}).withInitialPathName(packageName)
				.forPackageRename(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a name for the new class.
	 * Creates the class in the workspace and then opens it.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Package to place the class in initially.
	 */
	public void newClass(@Nonnull Workspace workspace,
	                     @Nonnull WorkspaceResource resource,
	                     @Nonnull JvmClassBundle bundle,
	                     @Nonnull String packageName) {
		new NamePopup(name -> {
			// TODO: We probably also want to configure the version
			//  - There are ways the user can do this themselves atm, but it would be nice to offer providing it
			ClassWriter cw = new ClassWriter(0);
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
			cw.visitEnd();

			JvmClassInfo info = new JvmClassInfoBuilder(cw.toByteArray()).build();
			bundle.put(info);

			FxThreadUtil.run(() -> gotoDeclaration(workspace, resource, bundle, info));
		}).withInitialName(packageName.isBlank() ? "ClassName" : packageName + "/ClassName")
				.forClassCreation(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a new name for the copied class.
	 * Inner classes also get copied.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to copy.
	 */
	public void copyClass(@Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource,
	                      @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo info) {
		String originalName = info.getName();
		Consumer<String> copyTask = newName -> {
			// Create mappings.
			IntermediateMappings mappings = new IntermediateMappings();
			mappings.addClass(originalName, newName);

			// Collect inner classes, we need to copy these as well.
			List<JvmClassInfo> classesToCopy = new ArrayList<>();
			classesToCopy.add(info);
			for (InnerClassInfo inner : info.getInnerClasses()) {
				if (inner.isExternalReference()) continue;
				String innerClassName = inner.getInnerClassName();
				mappings.addClass(innerClassName, newName + innerClassName.substring(originalName.length()));
				JvmClassInfo innerClassInfo = bundle.get(innerClassName);
				if (innerClassInfo != null)
					classesToCopy.add(innerClassInfo);
				else
					logger.warn("Could not find inner class for copy-operation: {}", EscapeUtil.escapeStandard(innerClassName));
			}

			// Apply mappings to create copies of the affected classes, using the provided name.
			// Then dump the mapped classes into bundle.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToClasses(mappings, resource, bundle, classesToCopy);
			for (ClassPathNode mappedClassPath : results.getPostMappingPaths().values()) {
				JvmClassInfo mappedClass = mappedClassPath.getValue().asJvmClass();
				bundle.put(mappedClass);
			}
		};
		new NamePopup(copyTask)
				.withInitialPathName(originalName)
				.forClassCopy(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a new name for the copied member.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Containing class.
	 * @param member
	 * 		member to copy.
	 */
	public void copyMember(@Nonnull Workspace workspace,
	                       @Nonnull WorkspaceResource resource,
	                       @Nonnull JvmClassBundle bundle,
	                       @Nonnull JvmClassInfo declaringClass,
	                       @Nonnull ClassMember member) {
		String originalName = member.getName();
		Consumer<String> copyTask = newName -> {
			ClassReader reader = declaringClass.getClassReader();
			ClassWriter writer = new ClassWriter(reader, 0);
			MemberCopyingVisitor copier = new MemberCopyingVisitor(writer, member, newName);
			reader.accept(copier, declaringClass.getClassReaderFlags());
			bundle.put(new JvmClassInfoBuilder(writer.toByteArray()).build());
		};
		NamePopup popup = new NamePopup(copyTask).withInitialName(originalName);
		if (member.isField())
			popup.forFieldCopy(declaringClass, member);
		else if (member.isMethod())
			popup.forMethodCopy(declaringClass, member);
		popup.show();
	}

	/**
	 * Prompts the user to give a new name for the copied file.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to copy.
	 */
	public void copyFile(@Nonnull Workspace workspace,
	                     @Nonnull WorkspaceResource resource,
	                     @Nonnull FileBundle bundle,
	                     @Nonnull FileInfo info) {
		new NamePopup(newName -> {
			if (info.getName().equals(newName)) return;
			bundle.put(info.toFileBuilder().withName(newName).build());
		}).withInitialPathName(info.getName())
				.forDirectoryCopy(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a new name for the copied directory.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Name of directory to copy.
	 */
	public void copyDirectory(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull FileBundle bundle,
	                          @Nonnull String directoryName) {
		boolean isRootDirectory = directoryName.isEmpty();
		new NamePopup(newDirectoryName -> {
			if (directoryName.equals(newDirectoryName)) return;
			for (FileInfo value : bundle.valuesAsCopy()) {
				String path = value.getName();
				if (isRootDirectory) {
					if (path.indexOf('/') == -1) {
						String name = newDirectoryName + "/" + path;
						bundle.put(value.toFileBuilder().withName(name).build());
					}
				} else {
					if (path.startsWith(directoryName)) {
						String name = replacePrefix(path, directoryName, newDirectoryName);
						bundle.put(value.toFileBuilder().withName(name).build());
					}
				}
			}
		}).withInitialPathName(directoryName)
				.forDirectoryCopy(bundle)
				.show();
	}

	/**
	 * Prompts the user to give a new name for the copied package.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Name of directory to copy.
	 */
	public void copyPackage(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle,
	                        @Nonnull String packageName) {
		boolean isRootDirectory = packageName.isEmpty();
		new NamePopup(newPackageName -> {
			// Create mappings.
			String oldPrefix = isRootDirectory ? "" : packageName + "/";
			String newPrefix = newPackageName + "/";
			IntermediateMappings mappings = new IntermediateMappings();
			List<JvmClassInfo> classesToCopy = new ArrayList<>();
			for (JvmClassInfo info : bundle.valuesAsCopy()) {
				String className = info.getName();
				if (isRootDirectory) {
					if (className.indexOf('/') == -1) {
						mappings.addClass(className, newPrefix + className);
						classesToCopy.add(info);
					}
				} else if (className.startsWith(oldPrefix)) {
					mappings.addClass(className, replacePrefix(className, oldPrefix, newPrefix));
					classesToCopy.add(info);
				}
			}

			// Apply mappings to create copies of the affected classes, using the provided name.
			// Then dump the mapped classes into bundle.
			MappingApplier applier = mappingApplierService.inWorkspace(workspace);
			MappingResults results = applier.applyToClasses(mappings, resource, bundle, classesToCopy);
			for (ClassPathNode mappedClassPath : results.getPostMappingPaths().values()) {
				JvmClassInfo mappedClass = mappedClassPath.getValue().asJvmClass();
				bundle.put(mappedClass);
			}
		}).withInitialPathName(packageName)
				.forPackageCopy(bundle)
				.show();
	}

	/**
	 * Opens an {@link AssemblerPane} for a class, field, or method at the given path.
	 *
	 * @param path
	 * 		Path containing a class, field, or method to open.
	 *
	 * @return Navigable content with an assembler for the given class, field, or method.
	 *
	 * @throws IncompletePathException
	 * 		When the path is missing parent elements.
	 */
	@Nonnull
	public Navigable openAssembler(@Nonnull PathNode<?> path) throws IncompletePathException {
		Workspace workspace = path.getValueOfType(Workspace.class);
		WorkspaceResource resource = path.getValueOfType(WorkspaceResource.class);
		ClassBundle<?> bundle = path.getValueOfType(ClassBundle.class);
		ClassInfo info = path.getValueOfType(ClassInfo.class);
		if (info == null) {
			logger.error("Cannot resolve required path nodes, missing class in path");
			throw new IncompletePathException(ClassInfo.class);
		}
		if (workspace == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing workspace in path", info.getName());
			throw new IncompletePathException(Workspace.class);
		}
		if (resource == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing resource in path", info.getName());
			throw new IncompletePathException(WorkspaceResource.class);
		}
		if (bundle == null) {
			logger.error("Cannot resolve required path nodes for class '{}', missing bundle in path", info.getName());
			throw new IncompletePathException(ClassBundle.class);
		}

		return createContent(() -> {
			// Create text/graphic for the tab to create.
			String name = "?";
			if (path instanceof ClassPathNode classPathNode)
				name = StringUtil.shortenPath(classPathNode.getValue().getName());
			else if (path instanceof ClassMemberPathNode classMemberPathNode)
				name = classMemberPathNode.getValue().getName();
			String title = "Assembler: " + EscapeUtil.escapeStandard(StringUtil.cutOff(name, 60));
			DockableIconFactory graphicFactory = d -> new FontIconView(CarbonIcons.CODE);

			// Create content for the tab.
			AssemblerPane content = assemblerPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), title, graphicFactory, content);
			dockable.addCloseListener((_, _) -> assemblerPaneProvider.destroy(content));

			// Class assemblers should have full context menus of a class.
			// Member assemblers should the basic close actions.
			if (path instanceof ClassPathNode)
				setupInfoContextMenu(info, content, dockable);
			else {
				dockable.setContextMenuFactory(d -> {
					ContextMenu menu = new ContextMenu();
					addCloseActions(menu, d);
					return menu;
				});
			}

			return dockable;
		});
	}


	/**
	 * Exports a class, prompting the user to select a location to save the class to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class declaring the method
	 * @param method
	 * 		Method to show the incoming/outgoing calls of.
	 *
	 * @return Navigable reference to the call graph pane.
	 */
	@Nonnull
	public Navigable openMethodCallGraph(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull JvmClassBundle bundle,
	                                     @Nonnull JvmClassInfo declaringClass,
	                                     @Nonnull MethodMember method) {
		return createContent(() -> {
			// Create text/graphic for the tab to create.
			String title = Lang.get("menu.view.methodcallgraph") + ": " + method.getName();
			DockableIconFactory graphicFactory = d -> new FontIconView(CarbonIcons.FLOW);

			// Create content for the tab.
			MethodCallGraphsPane content = callGraphsPaneProvider.get();
			content.onUpdatePath(PathNodes.memberPath(workspace, resource, bundle, declaringClass, method));

			// Build the tab.
			Dockable dockable = createDockable(dockingManager.getPrimaryDockingContainer(), title, graphicFactory, content);
			dockable.addCloseListener((_, _) -> callGraphsPaneProvider.destroy(content));
			return dockable;
		});
	}

	/**
	 * Exports a class, prompting the user to select a location to save the class to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to export.
	 */
	public void exportClass(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle,
	                        @Nonnull JvmClassInfo info) {
		pathExportingManager.export(info);
	}

	/**
	 * Exports a file, prompting the user to select a location to save the file to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to export.
	 */
	public void exportClass(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull FileBundle bundle,
	                        @Nonnull FileInfo info) {
		pathExportingManager.export(info);
	}

	/**
	 * Exports a package, prompting the user to select a location to save the file to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Name of package to export.
	 */
	public void exportPackage(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull JvmClassBundle bundle,
	                          @Nullable String packageName) {
		JvmClassBundle bundleCopy = new BasicJvmClassBundle();
		bundle.valuesAsCopy().forEach(cls -> {
			if (packageName == null && cls.getPackageName() == null)
				bundleCopy.put(cls);
			else if (cls.getName().startsWith(packageName + "/"))
				bundleCopy.put(cls);
		});
		WorkspaceResource resourceCopy = new WorkspaceResourceBuilder().withJvmClassBundle(bundleCopy).build();
		Workspace workspaceCopy = new BasicWorkspace(resourceCopy);
		pathExportingManager.export(workspaceCopy, "package", false);
	}

	/**
	 * Exports all classes in a bundle, prompting the user to select a location to save the file to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Bundle with contents to export.
	 */
	public void exportClasses(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull JvmClassBundle bundle) {
		BasicJvmClassBundle bundleCopy = new BasicJvmClassBundle();
		bundle.valuesAsCopy().forEach(bundleCopy::initialPut);
		WorkspaceResource resourceCopy = new WorkspaceResourceBuilder().withJvmClassBundle(bundleCopy).build();
		Workspace workspaceCopy = new BasicWorkspace(resourceCopy);
		pathExportingManager.export(workspaceCopy, "bundle", false);
	}

	/**
	 * Exports a directory, prompting the user to select a location to save the file to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Name of directory to export.
	 */
	public void exportDirectory(@Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull FileBundle bundle,
	                            @Nullable String directoryName) {
		FileBundle bundleCopy = new BasicFileBundle();
		bundle.valuesAsCopy().forEach(cls -> {
			if (directoryName == null && cls.getDirectoryName() == null)
				bundleCopy.put(cls);
			else if (cls.getName().startsWith(directoryName + "/"))
				bundleCopy.put(cls);
		});
		WorkspaceResource resourceCopy = new WorkspaceResourceBuilder().withFileBundle(bundleCopy).build();
		Workspace workspaceCopy = new BasicWorkspace(resourceCopy);
		pathExportingManager.export(workspaceCopy, "directory", false);
	}

	/**
	 * Exports all files in a bundle, prompting the user to select a location to save the file to.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Bundle with contents to export.
	 */
	public void exportFiles(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull FileBundle bundle) {
		BasicFileBundle bundleCopy = new BasicFileBundle();
		bundle.valuesAsCopy().forEach(bundleCopy::initialPut);
		WorkspaceResource resourceCopy = new WorkspaceResourceBuilder().withFileBundle(bundleCopy).build();
		Workspace workspaceCopy = new BasicWorkspace(resourceCopy);
		pathExportingManager.export(workspaceCopy, "bundle", false);
	}

	/**
	 * Prompts the user <i>(if configured, otherwise prompt is skipped)</i> to delete the class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to delete.
	 */
	public void deleteClass(@Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull JvmClassBundle bundle,
	                        @Nonnull JvmClassInfo info) {
		// TODO: Ask user if they are sure
		//  - Use config to check if "are you sure" prompts should be bypassed
		bundle.remove(info.getName());
	}

	/**
	 * Prompts the user <i>(if configured, otherwise prompt is skipped)</i> to delete the file.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File to delete.
	 */
	public void deleteFile(@Nonnull Workspace workspace,
	                       @Nonnull WorkspaceResource resource,
	                       @Nonnull FileBundle bundle,
	                       @Nonnull FileInfo info) {
		// TODO: Ask user if they are sure
		//  - Use config to check if "are you sure" prompts should be bypassed
		bundle.remove(info.getName());
	}

	/**
	 * Prompts the user <i>(if configured, otherwise prompt is skipped)</i> to delete the package.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		Name of package to delete.
	 */
	public void deletePackage(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull ClassBundle<?> bundle,
	                          @Nonnull String packageName) {
		// TODO: Ask user if they are sure
		//  - Use config to check if "are you sure" prompts should be bypassed
		boolean isRootDirectory = packageName.isEmpty();
		String packageNamePrefix = packageName + "/";
		for (ClassInfo value : bundle.valuesAsCopy()) {
			String path = value.getName();
			if (isRootDirectory) {
				// Source is in the default package, and the current class is also in the default package.
				if (path.indexOf('/') == -1) {
					bundle.remove(path);
				}
			} else {
				// Source is in a package, and the current class is in the same package.
				if (path.startsWith(packageNamePrefix)) {
					bundle.remove(path);
				}
			}
		}
	}

	/**
	 * Prompts the user <i>(if configured, otherwise prompt is skipped)</i> to delete the directory.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Name of directory to delete.
	 */
	public void deleteDirectory(@Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull FileBundle bundle,
	                            @Nonnull String directoryName) {
		// TODO: Ask user if they are sure
		//  - Use config to check if "are you sure" prompts should be bypassed
		boolean isRootDirectory = directoryName.isEmpty();
		String directoryNamePrefix = directoryName + "/";
		for (FileInfo value : bundle.valuesAsCopy()) {
			String path = value.getName();
			if (isRootDirectory) {
				// Source is in the root directory, and the current file is also in the root directory.
				if (path.indexOf('/') == -1) {
					bundle.remove(path);
				}
			} else {
				// Source is in a directory, and the current file is in the same directory.
				if (path.startsWith(directoryNamePrefix)) {
					bundle.remove(path);
				}
			}
		}
	}

	/**
	 * Prompts the user to select fields within the class to remove.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void deleteClassFields(@Nonnull Workspace workspace,
	                              @Nonnull WorkspaceResource resource,
	                              @Nonnull JvmClassBundle bundle,
	                              @Nonnull JvmClassInfo info) {
		ItemListSelectionPopup.forFields(info, fields -> deleteClassFields(workspace, resource, bundle, info, fields))
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.field"))
				.withTextMapping(field -> textService.getFieldMemberTextProvider(workspace, resource, bundle, info, field).makeText())
				.withGraphicMapping(field -> iconService.getClassMemberIconProvider(workspace, resource, bundle, info, field).makeIcon())
				.show();
	}

	/**
	 * Removes the given fields from the given class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class declaring the methods.
	 * @param fields
	 * 		Fields to delete.
	 */
	public void deleteClassFields(@Nonnull Workspace workspace,
	                              @Nonnull WorkspaceResource resource,
	                              @Nonnull JvmClassBundle bundle,
	                              @Nonnull JvmClassInfo declaringClass,
	                              @Nonnull Collection<FieldMember> fields) {
		ClassReader reader = declaringClass.getClassReader();
		ClassWriter writer = new ClassWriter(reader, 0);
		MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, FieldPredicate.of(fields));
		reader.accept(visitor, declaringClass.getClassReaderFlags());
		bundle.put(declaringClass.toJvmClassBuilder()
				.adaptFrom(writer.toByteArray())
				.build());
	}

	/**
	 * Prompts the user to select methods within the class to remove.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void deleteClassMethods(@Nonnull Workspace workspace,
	                               @Nonnull WorkspaceResource resource,
	                               @Nonnull JvmClassBundle bundle,
	                               @Nonnull JvmClassInfo info) {
		ItemListSelectionPopup.forMethods(info, methods -> deleteClassMethods(workspace, resource, bundle, info, methods))
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.method"))
				.withTextMapping(method -> textService.getMethodMemberTextProvider(workspace, resource, bundle, info, method).makeText())
				.withGraphicMapping(method -> iconService.getClassMemberIconProvider(workspace, resource, bundle, info, method).makeIcon())
				.show();
	}

	/**
	 * Removes the given methods from the given class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class declaring the methods.
	 * @param methods
	 * 		Methods to delete.
	 */
	public void deleteClassMethods(@Nonnull Workspace workspace,
	                               @Nonnull WorkspaceResource resource,
	                               @Nonnull JvmClassBundle bundle,
	                               @Nonnull JvmClassInfo declaringClass,
	                               @Nonnull Collection<MethodMember> methods) {
		ClassReader reader = declaringClass.getClassReader();
		ClassWriter writer = new ClassWriter(reader, 0);
		MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, MethodPredicate.of(methods));
		reader.accept(visitor, declaringClass.getClassReaderFlags());
		bundle.put(declaringClass.toJvmClassBuilder()
				.adaptFrom(writer.toByteArray())
				.build());
	}

	/**
	 * Prompts the user to select annotations on the class to remove.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void deleteClassAnnotations(@Nonnull Workspace workspace,
	                                   @Nonnull WorkspaceResource resource,
	                                   @Nonnull JvmClassBundle bundle,
	                                   @Nonnull JvmClassInfo info) {
		ItemListSelectionPopup.forAnnotationRemoval(info, annotations -> {
					List<String> names = annotations.stream()
							.map(AnnotationInfo::getDescriptor)
							.map(desc -> desc.substring(1, desc.length() - 1))
							.collect(Collectors.toList());
					immediateDeleteAnnotations(bundle, info, names);
				})
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.annotation"))
				.withTextMapping(anno -> textService.getAnnotationTextProvider(workspace, resource, bundle, info, anno).makeText())
				.withGraphicMapping(anno -> iconService.getAnnotationIconProvider(workspace, resource, bundle, info, anno).makeIcon())
				.show();
	}

	/**
	 * Prompts the user to select annotations on the field or method to remove.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 * @param member
	 * 		Field or method to remove annotations from.
	 */
	public void deleteMemberAnnotations(@Nonnull Workspace workspace,
	                                    @Nonnull WorkspaceResource resource,
	                                    @Nonnull JvmClassBundle bundle,
	                                    @Nonnull JvmClassInfo info,
	                                    @Nonnull ClassMember member) {
		ItemListSelectionPopup.forAnnotationRemoval(member, annotations -> {
					List<String> names = annotations.stream()
							.map(AnnotationInfo::getDescriptor)
							.map(desc -> desc.substring(1, desc.length() - 1))
							.collect(Collectors.toList());
					immediateDeleteAnnotations(bundle, member, names);
				})
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.annotation"))
				.withTextMapping(anno -> textService.getAnnotationTextProvider(workspace, resource, bundle, info, anno).makeText())
				.withGraphicMapping(anno -> iconService.getAnnotationIconProvider(workspace, resource, bundle, info, anno).makeIcon())
				.show();
	}

	/**
	 * Prompts the user for field declaration info, to add it to the given class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void addClassField(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull JvmClassBundle bundle,
	                          @Nonnull JvmClassInfo info) {
		new AddMemberPopup(member -> {
			ClassReader reader = info.getClassReader();
			ClassWriter writer = new ClassWriter(reader, 0);
			reader.accept(new MemberStubAddingVisitor(writer, member), info.getClassReaderFlags());
			JvmClassInfo updatedInfo = info.toJvmClassBuilder()
					.adaptFrom(writer.toByteArray())
					.build();
			bundle.put(updatedInfo);

			// Open the assembler with the new field
			try {
				openAssembler(PathNodes.memberPath(workspace, resource, bundle, updatedInfo, member));
			} catch (IncompletePathException e) {
				logger.error("Failed to open assembler for new field", e);
			}
		}).forField(info).show();
	}

	/**
	 * Prompts the user for method declaration info, to add it to the given class.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void addClassMethod(@Nonnull Workspace workspace,
	                           @Nonnull WorkspaceResource resource,
	                           @Nonnull JvmClassBundle bundle,
	                           @Nonnull JvmClassInfo info) {
		new AddMemberPopup(member -> {
			ClassReader reader = info.getClassReader();
			ClassWriter writer = new ClassWriter(reader, 0);
			reader.accept(new MemberStubAddingVisitor(writer, member), info.getClassReaderFlags());
			JvmClassInfo updatedInfo = info.toJvmClassBuilder()
					.adaptFrom(writer.toByteArray())
					.build();
			bundle.put(updatedInfo);

			// Open the assembler with the new method
			try {
				openAssembler(PathNodes.memberPath(workspace, resource, bundle, updatedInfo, member));
			} catch (IncompletePathException e) {
				logger.error("Failed to open assembler for new method", e);
			}
		}).forMethod(info).show();
	}

	/**
	 * Prompts the user to select a method to override.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to update.
	 */
	public void overrideClassMethod(@Nonnull Workspace workspace,
	                                @Nonnull WorkspaceResource resource,
	                                @Nonnull JvmClassBundle bundle,
	                                @Nonnull JvmClassInfo info) {
		InheritanceGraph inheritanceGraph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		new OverrideMethodPopup(this, cellConfigurationService, inheritanceGraph, workspace, info, (methodOwner, method) -> {
			ClassReader reader = info.getClassReader();
			ClassWriter writer = new ClassWriter(reader, 0);
			reader.accept(new MemberStubAddingVisitor(writer, method), info.getClassReaderFlags());
			JvmClassInfo updatedInfo = info.toJvmClassBuilder()
					.adaptFrom(writer.toByteArray())
					.build();
			bundle.put(updatedInfo);

			// Open the assembler with the new method
			try {
				openAssembler(PathNodes.memberPath(workspace, resource, bundle, updatedInfo, method));
			} catch (IncompletePathException e) {
				logger.error("Failed to open assembler for new method", e);
			}
		}).show();
	}

	/**
	 * Makes the given methods no-op.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param declaringClass
	 * 		Class declaring the methods.
	 * @param methods
	 * 		Methods to noop.
	 */
	public void makeMethodsNoop(@Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull JvmClassBundle bundle,
	                            @Nonnull JvmClassInfo declaringClass,
	                            @Nonnull Collection<MethodMember> methods) {
		ClassReader reader = declaringClass.getClassReader();
		ClassWriter writer = new ClassWriter(reader, 0);
		MethodNoopingVisitor visitor = new MethodNoopingVisitor(writer, MethodPredicate.of(methods));
		reader.accept(visitor, declaringClass.getClassReaderFlags());
		bundle.put(declaringClass.toJvmClassBuilder()
				.adaptFrom(writer.toByteArray())
				.build());
	}

	/**
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		Annotated class or member.
	 * @param annotationType
	 * 		Annotation type to remove.
	 */
	public void immediateDeleteAnnotations(@Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                       @Nonnull Annotated annotated,
	                                       @Nonnull String annotationType) {
		immediateDeleteAnnotations(bundle, annotated, Collections.singleton(annotationType));
	}

	/**
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		Annotated class or member.
	 * @param annotationTypes
	 * 		Annotation types to remove.
	 */
	public void immediateDeleteAnnotations(@Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                       @Nonnull Annotated annotated,
	                                       @Nonnull Collection<String> annotationTypes) {
		try {
			if (annotated instanceof JvmClassInfo target) {
				ClassReader reader = target.getClassReader();
				ClassWriter writer = new ClassWriter(reader, 0);
				reader.accept(new ClassAnnotationRemovingVisitor(writer, annotationTypes), target.getClassReaderFlags());
				JvmClassInfo updatedClass = new JvmClassInfoBuilder(writer.toByteArray()).build();
				bundle.put(cast(updatedClass));
			} else if (annotated instanceof ClassMember member && member.getDeclaringClass() instanceof JvmClassInfo target) {
				ClassReader reader = target.getClassReader();
				ClassWriter writer = new ClassWriter(reader, 0);
				if (member.isField()) {
					FieldMember field = (FieldMember) member;
					reader.accept(FieldAnnotationRemovingVisitor.forClass(writer, annotationTypes, field), target.getClassReaderFlags());
				} else {
					MethodMember method = (MethodMember) member;
					reader.accept(MethodAnnotationRemovingVisitor.forClass(writer, annotationTypes, method), target.getClassReaderFlags());
				}
				JvmClassInfo updatedClass = new JvmClassInfoBuilder(writer.toByteArray()).build();
				bundle.put(cast(updatedClass));
			} else {
				logger.warn("Cannot remove annotations on unsupported annotated type: {}", annotated.getClass().getSimpleName());
			}
		} catch (Throwable t) {
			logger.error("Failed removing annotation", t);
		}
	}

	/**
	 * @return New string search pane, opened in a new docking tab.
	 */
	@Nonnull
	public StringSearchPane openNewStringSearch() {
		return openSearchPane("menu.search.string", CarbonIcons.QUOTES, stringSearchPaneProvider);
	}

	/**
	 * @return New number search pane, opened in a new docking tab.
	 */
	@Nonnull
	public NumberSearchPane openNewNumberSearch() {
		return openSearchPane("menu.search.number", CarbonIcons.NUMBER_0, numberSearchPaneProvider);
	}

	/**
	 * @return New class-reference search pane, opened in a new docking tab.
	 */
	@Nonnull
	public ClassReferenceSearchPane openNewClassReferenceSearch() {
		return openSearchPane("menu.search.class.type-references", CarbonIcons.CODE_REFERENCE, classReferenceSearchPaneProvider);
	}

	/**
	 * @return New member-reference search pane, opened in a new docking tab.
	 */
	@Nonnull
	public MemberReferenceSearchPane openNewMemberReferenceSearch() {
		return openSearchPane("menu.search.class.member-references", CarbonIcons.CODE_REFERENCE, memberReferenceSearchPaneProvider);
	}

	/**
	 * @return New member-declaration search pane, opened in a new docking tab.
	 */
	@Nonnull
	public MemberDeclarationSearchPane openNewMemberDeclarationSearch() {
		return openSearchPane("menu.search.class.member-declarations", CarbonIcons.CODE, memberDeclarationSearchPaneProvider);
	}

	/**
	 * @return New instruction search pane, opened in a new docking tab.
	 */
	@Nonnull
	public InstructionSearchPane openNewInstructionSearch() {
		return openSearchPane("menu.search.class.instruction", CarbonIcons.CODE, instructionSearchPaneProvider);
	}

	@Nonnull
	private <T extends AbstractSearchPane> T openSearchPane(@Nonnull String titleId, @Nonnull Ikon icon, @Nonnull Instance<T> paneProvider) {
		// Place the tab in a region with other comments if possible.
		DockablePath searchPath = dockingManager.getBento()
				.search().dockable(d -> d.getNode() instanceof AbstractSearchPane);
		DockContainerLeaf container = searchPath == null ? null : searchPath.leafContainer();

		T content = paneProvider.get();
		Dockable dockable;
		if (container != null) {
			dockable = createDockable(container, getBinding(titleId), d -> new FontIconView(icon), content);
		} else {
			dockable = createDockable(null, getBinding(titleId), d -> new FontIconView(icon), content);
			Scene originScene = dockingManager.getPrimaryDockingContainer().asRegion().getScene();
			Stage stage = dockingManager.getBento().stageBuilding().newStageForDockable(originScene, dockable, 800, 400);
			stage.show();
			stage.requestFocus();
		}
		dockable.addCloseListener((_, _) -> paneProvider.destroy(content));
		dockable.setContextMenuFactory(d -> {
			ContextMenu menu = new ContextMenu();
			addCloseActions(menu, d);
			return menu;
		});
		return content;
	}

	/**
	 * Looks for the {@link Navigable} component representing the path and returns it if found.
	 * If no such component exists, it should be generated by the passed supplier, which then gets returned.
	 * <br>
	 * The dockable containing the {@link Navigable} component is selected when returned.
	 *
	 * @param path
	 * 		Path to navigate to.
	 * @param factory
	 * 		Factory to create a dockable for displaying content located at the given path,
	 * 		should a dockable for the content not already exist.
	 * 		<br>
	 * 		<b>NOTE:</b> It is required/assumed that the {@link Tab#getContent()} is a
	 * 		component implementing {@link Navigable}.
	 *
	 * @return Navigable content representing content of the path.
	 */
	@Nonnull
	public Navigable getOrCreatePathContent(@Nonnull PathNode<?> path, @Nonnull Supplier<Dockable> factory) {
		List<Navigable> children = navigationManager.getNavigableChildrenByPath(path);
		Navigable navigable = children.isEmpty() ? createContent(factory) : children.getFirst();
		selectTab(navigable);
		navigable.requestFocus();
		return navigable;
	}

	@Nonnull
	private Navigable createContent(@Nonnull Supplier<Dockable> factory) {
		// Create the dockable for the content, then display it.
		Dockable dockable = factory.get();
		Navigable navigable = (Navigable) Objects.requireNonNull(dockable.getNode());
		selectTab(navigable);
		navigable.requestFocus();
		return navigable;
	}

	private void setupInfoContextMenu(@Nonnull Info info,
	                                  @Nonnull AbstractContentPane<?> contentPane,
	                                  @Nonnull Dockable dockable) {
		setupInfoContextMenu(info, contentPane, dockable, null);
	}

	private void setupInfoContextMenu(@Nonnull Info info,
	                                  @Nonnull AbstractContentPane<?> contentPane,
	                                  @Nonnull Dockable dockable,
	                                  @Nullable Consumer<ContextMenu> menuAdapter) {
		dockable.setContextMenuFactory(d -> {
			ContextMenu menu = new ContextMenu();

			if (menuAdapter != null)
				menuAdapter.accept(menu);

			ObservableList<MenuItem> items = menu.getItems();
			if (info instanceof JvmClassInfo classInfo && contentPane instanceof JvmClassPane content) {
				Menu mode = menu("menu.mode", CarbonIcons.VIEW);
				mode.getItems().addAll(
						action("menu.mode.class.decompile", CarbonIcons.CODE,
								() -> content.setEditorType(JvmClassEditorType.DECOMPILE)),
						action("menu.mode.class.low-level", CarbonIcons.MICROSCOPE,
								() -> content.setEditorType(JvmClassEditorType.LOW_LEVEL)),
						action("menu.mode.file.hex", CarbonIcons.NUMBER_0,
								() -> content.setEditorType(JvmClassEditorType.HEX))
				);
				items.add(mode);
			} else if (info instanceof AndroidClassInfo classInfo && contentPane instanceof AndroidClassPane content) {
				Menu mode = menu("menu.mode", CarbonIcons.VIEW);
				mode.getItems().addAll(
						action("menu.mode.class.decompile", CarbonIcons.CODE,
								() -> content.setEditorType(AndroidClassEditorType.DECOMPILE)),
						action("menu.mode.file.smali", CarbonIcons.NUMBER_0,
								() -> content.setEditorType(AndroidClassEditorType.SMALI))
				);
				items.add(mode);
			}

			addCopyPathAction(menu, info);
			addCloseActions(menu, d);

			return menu;
		});
	}


	/**
	 * Selects the containing {@link Dockable} that contains the navigable content.
	 *
	 * @param navigable
	 * 		Navigable content to select in its containing {@link DockContainerLeaf}.
	 */
	private void selectTab(@Nullable Navigable navigable) {
		if (navigable == null)
			return;
		Dockable dockable = navigationManager.lookupDockable(navigable);
		if (dockable != null)
			dockable.inContainer(DockContainerLeaf::selectDockable);
	}

	/**
	 * Shorthand for dockable-creation + graphic setting.
	 *
	 * @param container
	 * 		Parent container to spawn in.
	 * @param title
	 * 		Dockable title.
	 * @param graphicFactory
	 * 		Dockable graphic factory.
	 * @param node
	 * 		Dockable content.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	private Dockable createDockable(@Nullable DockContainerLeaf container,
	                                @Nonnull String title,
	                                @Nonnull DockableIconFactory graphicFactory,
	                                @Nonnull Node node) {
		Dockable dockable = dockingManager.newDockable(title, graphicFactory, node);
		node.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (keybindingConfig.getCloseTab().match(e))
				dockable.inContainer(DockContainerLeaf::closeDockable);
		});
		if (container != null) {
			container.addDockable(dockable);
			container.selectDockable(dockable);
		}
		return dockable;
	}

	/**
	 * Shorthand for dockable-creation + graphic setting.
	 *
	 * @param container
	 * 		Parent container to spawn in.
	 * @param title
	 * 		Dockable title.
	 * @param graphicFactory
	 * 		Dockable graphic factory.
	 * @param node
	 * 		Dockable content.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	private Dockable createDockable(@Nullable DockContainerLeaf container,
	                                @Nonnull ObservableValue<String> title,
	                                @Nonnull DockableIconFactory graphicFactory,
	                                @Nonnull Node node) {
		Dockable dockable = dockingManager.newTranslatableDockable(title, graphicFactory, node);
		node.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (keybindingConfig.getCloseTab().match(e))
				dockable.inContainer(DockContainerLeaf::closeDockable);
		});
		if (container != null) {
			container.addDockable(dockable);
			container.selectDockable(dockable);
		}
		return dockable;
	}

	/**
	 * Adds 'mode' switches for supported file display modes in the given file pane.
	 *
	 * @param menu
	 * 		Menu to add items to.
	 * @param content
	 * 		File pane to pull supported display modes from.
	 */
	private static void addFilePaneOptions(@Nonnull ContextMenu menu, @Nonnull FilePane content) {
		List<FileDisplayMode> fileDisplayModes = content.getFileDisplayModes();
		if (fileDisplayModes.size() > 1) {
			Menu modeMenu = menu("menu.mode", CarbonIcons.VIEW);
			for (FileDisplayMode mode : fileDisplayModes)
				modeMenu.getItems().add(action(mode.getKey(), mode.newIcon(), () -> content.setFileDisplayMode(mode)));
			menu.getItems().add(modeMenu);
		}
	}

	/**
	 * Adds 'copy path' action to the given menu, with a following separator assuming other items will be added next.
	 *
	 * @param menu
	 * 		Menu to add to.
	 * @param info
	 * 		Info with path to copy.
	 */
	private static void addCopyPathAction(@Nonnull ContextMenu menu, @Nonnull Info info) {
		ObservableList<MenuItem> items = menu.getItems();
		items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> ClipboardUtil.copyString(info)));
		items.add(separator());
	}

	/**
	 * Adds close actions to the given menu.
	 * <ul>
	 *     <li>Close</li>
	 *     <li>Close others</li>
	 *     <li>Close all</li>
	 * </ul>
	 *
	 * @param menu
	 * 		Menu to add to.
	 * @param dockable
	 * 		Dockable reference.
	 */
	private static void addCloseActions(@Nonnull ContextMenu menu, @Nonnull Dockable dockable) {
		menu.getItems().addAll(
				action("menu.tab.close", CarbonIcons.CLOSE, () -> dockable.inContainer(DockContainerLeaf::closeDockable)),
				action("menu.tab.closeothers", CarbonIcons.CLOSE, () -> {
					dockable.inContainer(container -> {
						Unchecked.checkedForEach(container.getDockables(), d -> {
							if (d != dockable)
								container.closeDockable(d);
						}, (d, error) -> {
							logger.error("Failed to close tab '{}'", d.getTitle(), error);
						});
					});
				}),
				action("menu.tab.closeall", CarbonIcons.CLOSE, () -> {
					dockable.inContainer(container -> {
						Unchecked.checkedForEach(container.getDockables(), container::closeDockable, (d, error) -> {
							logger.error("Failed to close tab '{}'", d.getTitle(), error);
						});
					});
				})
		);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public ActionsConfig getServiceConfig() {
		return config;
	}
}
