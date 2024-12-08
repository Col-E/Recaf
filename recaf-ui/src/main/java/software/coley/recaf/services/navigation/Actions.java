package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.*;
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
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.graph.MethodCallGraphsPane;
import software.coley.recaf.ui.control.popup.AddMemberPopup;
import software.coley.recaf.ui.control.popup.ItemListSelectionPopup;
import software.coley.recaf.ui.control.popup.ItemTreeSelectionPopup;
import software.coley.recaf.ui.control.popup.NamePopup;
import software.coley.recaf.ui.control.popup.OverrideMethodPopup;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.pane.CommentEditPane;
import software.coley.recaf.ui.pane.DocumentationPane;
import software.coley.recaf.ui.pane.editing.android.AndroidClassPane;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.ui.pane.editing.binary.BinaryXmlFilePane;
import software.coley.recaf.ui.pane.editing.binary.HexFilePane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassEditorType;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassPane;
import software.coley.recaf.ui.pane.editing.jvm.TextEditorType;
import software.coley.recaf.ui.pane.editing.media.AudioFilePane;
import software.coley.recaf.ui.pane.editing.media.ImageFilePane;
import software.coley.recaf.ui.pane.editing.media.VideoFilePane;
import software.coley.recaf.ui.pane.editing.text.TextFilePane;
import software.coley.recaf.ui.pane.search.AbstractSearchPane;
import software.coley.recaf.ui.pane.search.ClassReferenceSearchPane;
import software.coley.recaf.ui.pane.search.MemberReferenceSearchPane;
import software.coley.recaf.ui.pane.search.NumberSearchPane;
import software.coley.recaf.ui.pane.search.StringSearchPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.SceneUtils;
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
	private final NavigationManager navigationManager;
	private final DockingManager dockingManager;
	private final WindowFactory windowFactory;
	private final TextProviderService textService;
	private final IconProviderService iconService;
	private final CellConfigurationService cellConfigurationService;
	private final PathExportingManager pathExportingManager;
	private final MappingApplierService mappingApplierService;
	private final Instance<InheritanceGraph> inheritanceGraphProvider;
	private final Instance<JvmClassPane> jvmPaneProvider;
	private final Instance<AndroidClassPane> androidPaneProvider;
	private final Instance<BinaryXmlFilePane> binaryXmlPaneProvider;
	private final Instance<TextFilePane> textPaneProvider;
	private final Instance<ImageFilePane> imagePaneProvider;
	private final Instance<AudioFilePane> audioPaneProvider;
	private final Instance<VideoFilePane> videoPaneProvider;
	private final Instance<HexFilePane> hexPaneProvider;
	private final Instance<AssemblerPane> assemblerPaneProvider;
	private final Instance<CommentEditPane> documentationPaneProvider;
	private final Instance<MethodCallGraphsPane> callGraphsPaneProvider;
	private final Instance<StringSearchPane> stringSearchPaneProvider;
	private final Instance<NumberSearchPane> numberSearchPaneProvider;
	private final Instance<ClassReferenceSearchPane> classReferenceSearchPaneProvider;
	private final Instance<MemberReferenceSearchPane> memberReferenceSearchPaneProvider;
	private final ActionsConfig config;

	@Inject
	public Actions(@Nonnull ActionsConfig config,
	               @Nonnull NavigationManager navigationManager,
	               @Nonnull DockingManager dockingManager,
	               @Nonnull WindowFactory windowFactory,
	               @Nonnull TextProviderService textService,
	               @Nonnull IconProviderService iconService,
	               @Nonnull CellConfigurationService cellConfigurationService,
	               @Nonnull PathExportingManager pathExportingManager,
	               @Nonnull MappingApplierService mappingApplierService,
	               @Nonnull Instance<InheritanceGraph> inheritanceGraphProvider,
	               @Nonnull Instance<MappingApplier> applierProvider,
	               @Nonnull Instance<JvmClassPane> jvmPaneProvider,
	               @Nonnull Instance<AndroidClassPane> androidPaneProvider,
	               @Nonnull Instance<BinaryXmlFilePane> binaryXmlPaneProvider,
	               @Nonnull Instance<TextFilePane> textPaneProvider,
	               @Nonnull Instance<ImageFilePane> imagePaneProvider,
	               @Nonnull Instance<AudioFilePane> audioPaneProvider,
	               @Nonnull Instance<VideoFilePane> videoPaneProvider,
	               @Nonnull Instance<HexFilePane> hexPaneProvider,
	               @Nonnull Instance<AssemblerPane> assemblerPaneProvider,
	               @Nonnull Instance<CommentEditPane> documentationPaneProvider,
	               @Nonnull Instance<StringSearchPane> stringSearchPaneProvider,
	               @Nonnull Instance<NumberSearchPane> numberSearchPaneProvider,
	               @Nonnull Instance<MethodCallGraphsPane> callGraphsPaneProvider,
	               @Nonnull Instance<ClassReferenceSearchPane> classReferenceSearchPaneProvider,
	               @Nonnull Instance<MemberReferenceSearchPane> memberReferenceSearchPaneProvider) {
		this.config = config;
		this.navigationManager = navigationManager;
		this.dockingManager = dockingManager;
		this.windowFactory = windowFactory;
		this.textService = textService;
		this.iconService = iconService;
		this.cellConfigurationService = cellConfigurationService;
		this.pathExportingManager = pathExportingManager;
		this.mappingApplierService = mappingApplierService;
		this.inheritanceGraphProvider = inheritanceGraphProvider;
		this.jvmPaneProvider = jvmPaneProvider;
		this.androidPaneProvider = androidPaneProvider;
		this.binaryXmlPaneProvider = binaryXmlPaneProvider;
		this.textPaneProvider = textPaneProvider;
		this.imagePaneProvider = imagePaneProvider;
		this.audioPaneProvider = audioPaneProvider;
		this.videoPaneProvider = videoPaneProvider;
		this.hexPaneProvider = hexPaneProvider;
		this.assemblerPaneProvider = assemblerPaneProvider;
		this.documentationPaneProvider = documentationPaneProvider;
		this.stringSearchPaneProvider = stringSearchPaneProvider;
		this.numberSearchPaneProvider = numberSearchPaneProvider;
		this.callGraphsPaneProvider = callGraphsPaneProvider;
		this.classReferenceSearchPaneProvider = classReferenceSearchPaneProvider;
		this.memberReferenceSearchPaneProvider = memberReferenceSearchPaneProvider;
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
			Node graphic = iconService.getJvmClassInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			JvmClassPane content = jvmPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			content.addPathUpdateListener(updatedPath -> {
				// Update tab graphic in case backing class details change.
				JvmClassInfo updatedInfo = updatedPath.getValue().asJvmClass();
				String updatedTitle = textService.getJvmClassInfoTextProvider(workspace, resource, bundle, updatedInfo).makeText();
				Node updatedGraphic = iconService.getJvmClassInfoIconProvider(workspace, resource, bundle, updatedInfo).makeIcon();
				FxThreadUtil.run(() -> {
					tab.setText(updatedTitle);
					tab.setGraphic(updatedGraphic);
				});
			});
			ContextMenu menu = new ContextMenu();
			ObservableList<MenuItem> items = menu.getItems();
			Menu mode = menu("menu.mode", CarbonIcons.VIEW);
			mode.getItems().addAll(
					action("menu.mode.class.decompile", CarbonIcons.CODE,
							() -> content.setEditorType(JvmClassEditorType.DECOMPILE)),
					action("menu.mode.file.hex", CarbonIcons.NUMBER_0,
							() -> content.setEditorType(JvmClassEditorType.HEX))
			);
			items.add(mode);
			addCopyPathAction(menu, info);
			addCloseActions(menu, tab);
			tab.setContextMenu(menu);
			return tab;
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
			Node graphic = iconService.getAndroidClassInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			AndroidClassPane content = androidPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			content.addPathUpdateListener(updatedPath -> {
				// Update tab graphic in case backing class details change.
				AndroidClassInfo updatedInfo = updatedPath.getValue().asAndroidClass();
				String updatedTitle = textService.getAndroidClassInfoTextProvider(workspace, resource, bundle, updatedInfo).makeText();
				Node updatedGraphic = iconService.getAndroidClassInfoIconProvider(workspace, resource, bundle, updatedInfo).makeIcon();
				FxThreadUtil.run(() -> {
					tab.setText(updatedTitle);
					tab.setGraphic(updatedGraphic);
				});
			});
			setupInfoTabContextMenu(info, tab);
			return tab;
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

		// Handle text vs binary
		if (info.isTextFile()) {
			return gotoDeclaration(workspace, resource, bundle, info.asTextFile());
		} else if (info.isImageFile()) {
			return gotoDeclaration(workspace, resource, bundle, info.asImageFile());
		} else if (info instanceof BinaryXmlFileInfo binaryXml) {
			return gotoDeclaration(workspace, resource, bundle, binaryXml);
		} else if (info.isAudioFile()) {
			return gotoDeclaration(workspace, resource, bundle, info.asAudioFile());
		} else if (info.isVideoFile()) {
			return gotoDeclaration(workspace, resource, bundle, info.asVideoFile());
		}
		return gotoDeclaration(workspace, resource, bundle, info.asFile());
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given binary XML file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Binary XML file to go to.
	 *
	 * @return Navigable content representing binary XML file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull BinaryXmlFileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			BinaryXmlFilePane content = binaryXmlPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			setupInfoTabContextMenu(info, tab);
			return tab;
		});
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given text file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Text file to go to.
	 *
	 * @return Navigable content representing text file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull TextFileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			TextFilePane content = textPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			ContextMenu menu = new ContextMenu();
			ObservableList<MenuItem> items = menu.getItems();
			Menu mode = menu("menu.mode", CarbonIcons.VIEW);
			mode.getItems().addAll(
					action("menu.mode.file.text", CarbonIcons.CODE,
							() -> content.setEditorType(TextEditorType.TEXT)),
					action("menu.mode.file.hex", CarbonIcons.NUMBER_0,
							() -> content.setEditorType(TextEditorType.HEX))
			);
			items.add(mode);
			addCopyPathAction(menu, info);
			addCloseActions(menu, tab);
			tab.setContextMenu(menu);

			return tab;
		});
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given image file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Image file to go to.
	 *
	 * @return Navigable content representing image file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull ImageFileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			ImageFilePane content = imagePaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			setupInfoTabContextMenu(info, tab);
			return tab;
		});
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given audio file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Audio file to go to.
	 *
	 * @return Navigable content representing audio file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull AudioFileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			AudioFilePane content = audioPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			setupInfoTabContextMenu(info, tab);
			return tab;
		});
	}

	/**
	 * Brings a {@link FileNavigable} component representing the given video file into focus.
	 * If no such component exists, one is created.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Video file to go to.
	 *
	 * @return Navigable content representing video file content of the path.
	 */
	@Nonnull
	public FileNavigable gotoDeclaration(@Nonnull Workspace workspace,
	                                     @Nonnull WorkspaceResource resource,
	                                     @Nonnull FileBundle bundle,
	                                     @Nonnull VideoFileInfo info) {
		FilePathNode path = PathNodes.filePath(workspace, resource, bundle, info);
		return (FileNavigable) getOrCreatePathContent(path, () -> {
			// Create text/graphic for the tab to create.
			String title = textService.getFileInfoTextProvider(workspace, resource, bundle, info).makeText();
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			VideoFilePane content = videoPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			setupInfoTabContextMenu(info, tab);
			return tab;
		});
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
			Node graphic = iconService.getFileInfoIconProvider(workspace, resource, bundle, info).makeIcon();
			if (title == null) throw new IllegalStateException("Missing title");
			if (graphic == null) throw new IllegalStateException("Missing graphic");

			// Create content for the tab.
			HexFilePane content = hexPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
			setupInfoTabContextMenu(info, tab);
			return tab;
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
			Node graphic = new FontIconView(CarbonIcons.BOOKMARK_FILLED);
			if (title == null) throw new IllegalStateException("Missing title");
			return createCommentEditTab(path, title, graphic, info);
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
			Node graphic = new FontIconView(CarbonIcons.BOOKMARK_FILLED);
			if (title == null) throw new IllegalStateException("Missing title");
			return createCommentEditTab(path, title, graphic, classInfo);
		});
	}

	@Nonnull
	private DockingTab createCommentEditTab(@Nonnull PathNode<?> path, @Nonnull String title,
	                                        @Nonnull Node graphic, @Nonnull ClassInfo classInfo) {
		// Create content for the tab.
		CommentEditPane content = documentationPaneProvider.get();
		content.onUpdatePath(path);

		// Place the tab in a region with other comments if possible.
		DockingRegion targetRegion = dockingManager.getDockTabs().stream()
				.filter(t -> t.getContent() instanceof DocumentationPane)
				.map(DockingTab::getRegion)
				.findFirst().orElse(dockingManager.getPrimaryRegion());

		// Build the tab.
		DockingTab tab = createTab(targetRegion, title, graphic, content);
		setupInfoTabContextMenu(classInfo, tab);
		return tab;
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
			ClassWriter cw = new ClassWriter(0);
			MemberCopyingVisitor cp = new MemberCopyingVisitor(cw, member, newName);
			declaringClass.getClassReader().accept(cp, 0);
			bundle.put(new JvmClassInfoBuilder(cw.toByteArray()).build());
		};
		new NamePopup(copyTask)
				.withInitialName(originalName)
				.forFieldCopy(declaringClass, member)
				.show();
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
			String title = "Assembler: " + name;
			Node graphic = new FontIconView(CarbonIcons.CODE);

			// Create content for the tab.
			AssemblerPane content = assemblerPaneProvider.get();
			content.onUpdatePath(path);

			// Build the tab.
			return createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
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
			Node graphic = new FontIconView(CarbonIcons.FLOW);

			// Create content for the tab.
			MethodCallGraphsPane content = callGraphsPaneProvider.get();
			content.onUpdatePath(PathNodes.memberPath(workspace, resource, bundle, declaringClass, method));

			// Build the tab.
			return createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
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
		ClassWriter writer = new ClassWriter(0);
		MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, FieldPredicate.of(fields));
		declaringClass.getClassReader().accept(visitor, 0);
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
		ClassWriter writer = new ClassWriter(0);
		MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, MethodPredicate.of(methods));
		declaringClass.getClassReader().accept(visitor, 0);
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
			ClassWriter writer = new ClassWriter(0);
			info.getClassReader().accept(new MemberStubAddingVisitor(writer, member), 0);
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
			ClassWriter writer = new ClassWriter(0);
			info.getClassReader().accept(new MemberStubAddingVisitor(writer, member), 0);
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
		InheritanceGraph inheritanceGraph = inheritanceGraphProvider.get();
		if (inheritanceGraph == null)
			return;
		new OverrideMethodPopup(this, cellConfigurationService, inheritanceGraph, workspace, info, (methodOwner, method) -> {
			ClassWriter writer = new ClassWriter(0);
			info.getClassReader().accept(new MemberStubAddingVisitor(writer, method), 0);
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
		ClassWriter writer = new ClassWriter(0);
		MethodNoopingVisitor visitor = new MethodNoopingVisitor(writer, MethodPredicate.of(methods));
		declaringClass.getClassReader().accept(visitor, 0);
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
				ClassWriter writer = new ClassWriter(0);
				target.getClassReader().accept(new ClassAnnotationRemovingVisitor(writer, annotationTypes), 0);
				JvmClassInfo updatedClass = new JvmClassInfoBuilder(writer.toByteArray()).build();
				bundle.put(cast(updatedClass));
			} else if (annotated instanceof ClassMember member && member.getDeclaringClass() instanceof JvmClassInfo target) {
				ClassWriter writer = new ClassWriter(0);
				if (member.isField()) {
					FieldMember field = (FieldMember) member;
					target.getClassReader().accept(FieldAnnotationRemovingVisitor.forClass(writer, annotationTypes, field), 0);
				} else {
					MethodMember method = (MethodMember) member;
					target.getClassReader().accept(MethodAnnotationRemovingVisitor.forClass(writer, annotationTypes, method), 0);
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

	@Nonnull
	private <T extends AbstractSearchPane> T openSearchPane(@Nonnull String titleId, @Nonnull Ikon icon, @Nonnull Instance<T> paneProvider) {
		DockingRegion region = dockingManager.getRegions().stream()
				.filter(r -> r.getDockTabs().stream().anyMatch(t -> t.getContent() instanceof AbstractSearchPane))
				.findFirst().orElse(null);
		T content = paneProvider.get();
		if (region != null) {
			DockingTab tab = region.createTab(getBinding(titleId), content);
			tab.setGraphic(new FontIconView(icon));
			tab.select();
			FxThreadUtil.run(() -> SceneUtils.focus(content));
		} else {
			region = dockingManager.newRegion();
			DockingTab tab = region.createTab(getBinding(titleId), content);
			tab.setGraphic(new FontIconView(icon));
			RecafScene scene = new RecafScene(region);
			Stage window = windowFactory.createAnonymousStage(scene, getBinding("menu.search"), 800, 400);
			window.show();
			window.requestFocus();
		}
		return content;
	}

	/**
	 * Looks for the {@link Navigable} component representing the path and returns it if found.
	 * If no such component exists, it should be generated by the passed supplier, which then gets returned.
	 * <br>
	 * The tab containing the {@link Navigable} component is selected when returned.
	 *
	 * @param path
	 * 		Path to navigate to.
	 * @param factory
	 * 		Factory to create a tab for displaying content located at the given path,
	 * 		should a tab for the content not already exist.
	 * 		<br>
	 * 		<b>NOTE:</b> It is required/assumed that the {@link Tab#getContent()} is a
	 * 		component implementing {@link Navigable}.
	 *
	 * @return Navigable content representing content of the path.
	 */
	@Nonnull
	public Navigable getOrCreatePathContent(@Nonnull PathNode<?> path, @Nonnull Supplier<DockingTab> factory) {
		List<Navigable> children = navigationManager.getNavigableChildrenByPath(path);
		if (children.isEmpty()) {
			return createContent(factory);
		} else {
			// Content by path is already open.
			Navigable navigable = children.getFirst();
			selectTab(navigable);
			navigable.requestFocus();
			return navigable;
		}
	}

	@Nonnull
	private static Navigable createContent(@Nonnull Supplier<DockingTab> factory) {
		// Create the tab for the content, then display it.
		DockingTab tab = factory.get();
		tab.select();
		SceneUtils.focus(tab.getRegion().getScene());
		return (Navigable) tab.getContent();
	}

	private static void setupInfoTabContextMenu(@Nonnull Info info, @Nonnull DockingTab tab) {
		ContextMenu menu = new ContextMenu();
		ObservableList<MenuItem> items = menu.getItems();
		addCopyPathAction(menu, info);
		addCloseActions(menu, tab);
		tab.setContextMenu(menu);
	}


	/**
	 * Selects the containing {@link DockingTab} that contains the content.
	 *
	 * @param navigable
	 * 		Navigable content to select in its containing {@link DockingRegion}.
	 */
	private static void selectTab(Navigable navigable) {
		if (navigable instanceof Node node)
			SceneUtils.focus(node);
	}

	/**
	 * Shorthand for tab-creation + graphic setting.
	 *
	 * @param region
	 * 		Parent region to spawn in.
	 * @param title
	 * 		Tab title.
	 * @param graphic
	 * 		Tab graphic.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	@Nonnull
	private static DockingTab createTab(@Nonnull DockingRegion region,
	                                    @Nonnull String title,
	                                    @Nonnull Node graphic,
	                                    @Nonnull Node content) {
		DockingTab tab = region.createTab(title, content);
		tab.setGraphic(graphic);
		return tab;
	}

	/**
	 * Shorthand for tab-creation + graphic setting.
	 *
	 * @param region
	 * 		Parent region to spawn in.
	 * @param title
	 * 		Tab title.
	 * @param graphic
	 * 		Tab graphic.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	@Nonnull
	private static DockingTab createTab(@Nonnull DockingRegion region,
	                                    @Nonnull ObservableValue<String> title,
	                                    @Nonnull Node graphic,
	                                    @Nonnull Node content) {
		DockingTab tab = region.createTab(title, content);
		tab.setGraphic(graphic);
		return tab;
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
	 * @param tab
	 * 		Tab reference.
	 */
	private static void addCloseActions(@Nonnull ContextMenu menu, @Nonnull DockingTab tab) {
		menu.getItems().addAll(
				action("menu.tab.close", CarbonIcons.CLOSE, tab::close),
				action("menu.tab.closeothers", CarbonIcons.CLOSE, () -> {
					Unchecked.checkedForEach(tab.getRegion().getDockTabs(), regionTab -> {
						if (regionTab != tab)
							regionTab.close();
					}, (regionTab, error) -> {
						logger.error("Failed to close tab '{}'", regionTab.getText(), error);
					});
				}),
				action("menu.tab.closeall", CarbonIcons.CLOSE, () -> {
					Unchecked.checkedForEach(tab.getRegion().getDockTabs(), DockingTab::close, (regionTab, error) -> {
						logger.error("Failed to close tab '{}'", regionTab.getText(), error);
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
