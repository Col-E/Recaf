package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.*;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.cell.IconProviderService;
import software.coley.recaf.services.cell.TextProviderService;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.ui.control.popup.ItemSelectionPopup;
import software.coley.recaf.ui.control.popup.NamePopup;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.pane.editing.android.AndroidClassPane;
import software.coley.recaf.ui.pane.editing.binary.BinaryXmlFilePane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassEditorType;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassPane;
import software.coley.recaf.ui.pane.editing.text.TextFilePane;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.Unchecked;
import software.coley.recaf.util.visitors.ClassAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.MemberPredicate;
import software.coley.recaf.util.visitors.MemberRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static software.coley.recaf.util.Menus.*;

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
	private final TextProviderService textService;
	private final IconProviderService iconService;
	private final Instance<MappingApplier> applierProvider;
	private final Instance<JvmClassPane> jvmPaneProvider;
	private final Instance<AndroidClassPane> androidPaneProvider;
	private final Instance<BinaryXmlFilePane> binaryXmlPaneProvider;
	private final Instance<TextFilePane> textPaneProvider;
	private final ActionsConfig config;

	@Inject
	public Actions(@Nonnull ActionsConfig config,
				   @Nonnull NavigationManager navigationManager,
				   @Nonnull DockingManager dockingManager,
				   @Nonnull TextProviderService textService,
				   @Nonnull IconProviderService iconService,
				   @Nonnull Instance<MappingApplier> applierProvider,
				   @Nonnull Instance<JvmClassPane> jvmPaneProvider,
				   @Nonnull Instance<AndroidClassPane> androidPaneProvider,
				   @Nonnull Instance<BinaryXmlFilePane> binaryXmlPaneProvider,
				   @Nonnull Instance<TextFilePane> textPaneProvider) {
		this.config = config;
		this.navigationManager = navigationManager;
		this.dockingManager = dockingManager;
		this.textService = textService;
		this.iconService = iconService;
		this.applierProvider = applierProvider;
		this.jvmPaneProvider = jvmPaneProvider;
		this.androidPaneProvider = androidPaneProvider;
		this.binaryXmlPaneProvider = binaryXmlPaneProvider;
		this.textPaneProvider = textPaneProvider;
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
		throw new UnsupportedContent("Unsupported class type: " + info.getClass().getName());
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
		ClassPathNode path = buildPath(workspace, resource, bundle, info);
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
			items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> {
				ClipboardContent clipboard = new ClipboardContent();
				clipboard.putString(info.getName());
				Clipboard.getSystemClipboard().setContent(clipboard);
			}));
			items.add(separator());
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
		ClassPathNode path = buildPath(workspace, resource, bundle, info);
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
			ContextMenu menu = new ContextMenu();
			ObservableList<MenuItem> items = menu.getItems();
			items.add(action("menu.tab.copypath", () -> {
				ClipboardContent clipboard = new ClipboardContent();
				clipboard.putString(info.getName());
				Clipboard.getSystemClipboard().setContent(clipboard);
			}));
			items.add(separator());
			addCloseActions(menu, tab);
			tab.setContextMenu(menu);
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
		} else if (info instanceof BinaryXmlFileInfo binaryXml) {
			return gotoDeclaration(workspace, resource, bundle, binaryXml);
		}
		throw new UnsupportedContent("Unsupported file type: " + info.getClass().getName());
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
		FilePathNode path = buildPath(workspace, resource, bundle, info);
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
			ContextMenu menu = new ContextMenu();
			ObservableList<MenuItem> items = menu.getItems();
			items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> {
				ClipboardContent clipboard = new ClipboardContent();
				clipboard.putString(info.getName());
				Clipboard.getSystemClipboard().setContent(clipboard);
			}));
			items.add(separator());
			addCloseActions(menu, tab);
			tab.setContextMenu(menu);
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
		FilePathNode path = buildPath(workspace, resource, bundle, info);
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
			items.add(action("menu.tab.copypath", CarbonIcons.COPY_LINK, () -> {
				ClipboardContent clipboard = new ClipboardContent();
				clipboard.putString(info.getName());
				Clipboard.getSystemClipboard().setContent(clipboard);
			}));
			items.add(separator());
			addCloseActions(menu, tab);
			tab.setContextMenu(menu);
			return tab;
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
	 * 		Class to go move into a different package.
	 */
	public void moveClass(@Nonnull Workspace workspace,
						  @Nonnull WorkspaceResource resource,
						  @Nonnull JvmClassBundle bundle,
						  @Nonnull JvmClassInfo info) {
		// TODO: This should use a tree, not a list popup
		ItemSelectionPopup.forPackageNames(bundle, packages -> {
					// We only allow a single package, so the list should contain just one item.
					String oldPackage = info.getPackageName() + "/";
					String newPackage = packages.get(0) + "/";
					if (Objects.equals(oldPackage, newPackage)) return;

					// Create mapping for the class and any inner classes.
					String originalName = info.getName();
					String newName = newPackage + info.getName().substring(oldPackage.length());
					IntermediateMappings mappings = new IntermediateMappings();
					for (InnerClassInfo inner : info.getInnerClasses()) {
						if (inner.isExternalReference()) continue;
						String innerClassName = inner.getInnerClassName();
						mappings.addClass(innerClassName, newName + innerClassName.substring(originalName.length()));
					}

					// Apply the mappings.
					MappingApplier applier = applierProvider.get();
					MappingResults results = applier.applyToPrimaryResource(mappings);
					results.apply();
				})
				.withTitle(Lang.getBinding("dialog.title.move-class"))
				.withTextMapping(name -> textService.getPackageTextProvider(workspace, resource, bundle, name).makeText())
				.withGraphicMapping(name -> iconService.getPackageIconProvider(workspace, resource, bundle, name).makeIcon())
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
		// TODO: Handle other types (fields/methods)
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

		// Handle JVM vs Android
		if (info.isJvmClass()) {
			renameClass(workspace, resource, (JvmClassBundle) bundle, info.asJvmClass());
		} else if (info.isAndroidClass()) {
			// TODO: Android renaming
			logger.error("TODO: Android renaming");
		} else {
			throw new UnsupportedContent("Unsupported class type: " + info.getClass().getName());
		}
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
	 * 		Class to go rename.
	 */
	public void renameClass(@Nonnull Workspace workspace,
							@Nonnull WorkspaceResource resource,
							@Nonnull JvmClassBundle bundle,
							@Nonnull JvmClassInfo info) {
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
			MappingApplier applier = applierProvider.get();
			MappingResults results = applier.applyToPrimaryResource(mappings);
			results.apply();
		};
		new NamePopup(renameTask)
				.withInitialClassName(originalName)
				.forClassRename(bundle)
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
			MappingApplier applier = applierProvider.get();
			MappingResults results = applier.applyToClasses(mappings, resource, bundle, classesToCopy);
			for (ClassPathNode mappedClassPath : results.getPostMappingPaths().values()) {
				JvmClassInfo mappedClass = mappedClassPath.getValue().asJvmClass();
				bundle.put(mappedClass);
			}
		};
		new NamePopup(copyTask)
				.withInitialClassName(originalName)
				.forClassCopy(bundle)
				.show();
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
		ItemSelectionPopup.forFields(info, fields -> {
					ClassWriter writer = new ClassWriter(0);
					MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, new MemberPredicate() {
						@Override
						public boolean matchField(int access, String name, String desc, String sig, Object value) {
							for (FieldMember field : fields)
								if (field.getName().equals(name) && field.getDescriptor().equals(desc))
									return true;
							return false;
						}

						@Override
						public boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
							return false;
						}
					});
					info.getClassReader().accept(visitor, 0);
					bundle.put(info.toJvmClassBuilder()
							.adaptFrom(new ClassReader(writer.toByteArray()))
							.build());
				})
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.field"))
				.withTextMapping(field -> textService.getFieldMemberTextProvider(workspace, resource, bundle, info, field).makeText())
				.withGraphicMapping(field -> iconService.getClassMemberIconProvider(workspace, resource, bundle, info, field).makeIcon())
				.show();
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
		ItemSelectionPopup.forMethods(info, methods -> {
					ClassWriter writer = new ClassWriter(0);
					MemberRemovingVisitor visitor = new MemberRemovingVisitor(writer, new MemberPredicate() {
						@Override
						public boolean matchField(int access, String name, String desc, String sig, Object value) {
							return false;
						}

						@Override
						public boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions) {
							for (MethodMember method : methods)
								if (method.getName().equals(name) && method.getDescriptor().equals(desc))
									return true;
							return false;
						}
					});
					info.getClassReader().accept(visitor, 0);
					bundle.put(info.toJvmClassBuilder()
							.adaptFrom(new ClassReader(writer.toByteArray()))
							.build());
				})
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.method"))
				.withTextMapping(method -> textService.getMethodMemberTextProvider(workspace, resource, bundle, info, method).makeText())
				.withGraphicMapping(method -> iconService.getClassMemberIconProvider(workspace, resource, bundle, info, method).makeIcon())
				.show();
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
		ItemSelectionPopup.forAnnotationRemoval(info, annotations -> {
					List<String> names = annotations.stream()
							.map(AnnotationInfo::getDescriptor)
							.map(desc -> desc.substring(1, desc.length() - 1))
							.collect(Collectors.toList());
					ClassWriter writer = new ClassWriter(0);
					ClassAnnotationRemovingVisitor visitor = new ClassAnnotationRemovingVisitor(writer, names);
					info.getClassReader().accept(visitor, 0);
					bundle.put(info.toJvmClassBuilder()
							.adaptFrom(new ClassReader(writer.toByteArray()))
							.build());
				})
				.withMultipleSelection()
				.withTitle(Lang.getBinding("menu.edit.remove.annotation"))
				.withTextMapping(anno -> textService.getAnnotationTextProvider(workspace, resource, bundle, info, anno).makeText())
				.withGraphicMapping(anno -> iconService.getAnnotationIconProvider(workspace, resource, bundle, info, anno).makeIcon())
				.show();
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
			// Create the tab for the content, then display it.
			DockingTab tab = factory.get();
			tab.select();
			return (Navigable) tab.getContent();
		} else {
			// Content by path is already open.
			Navigable navigable = children.get(0);
			selectTab(navigable);
			navigable.requestFocus();
			return navigable;
		}
	}

	/**
	 * Selects the containing {@link DockingTab} that contains the content.
	 *
	 * @param navigable
	 * 		Navigable content to select in its containing {@link DockingRegion}.
	 */
	private static void selectTab(Navigable navigable) {
		if (navigable instanceof Node node) {
			while (node != null) {
				// Get the parent of the node, skip the intermediate 'content area' from tab-pane default skin.
				Parent parent = node.getParent();
				if (parent.getStyleClass().contains("tab-content-area"))
					parent = parent.getParent();

				// If the tab content is the node, select it and return.
				if (parent instanceof DockingRegion tabParent)
					for (DockingTab tab : tabParent.getDockTabs())
						if (tab.getContent() == node) {
							tab.select();
							return;
						}

				// Next parent.
				node = parent;
			}
		}
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
	private static DockingTab createTab(@Nonnull DockingRegion region,
										@Nonnull String title,
										@Nonnull Node graphic,
										@Nonnull Node content) {
		DockingTab tab = region.createTab(title, content);
		tab.setGraphic(graphic);
		return tab;
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
	 * @param currentTab
	 * 		Current tab reference.
	 */
	private static void addCloseActions(@Nonnull ContextMenu menu, @Nonnull DockingTab currentTab) {
		menu.getItems().addAll(
				action("menu.tab.close", CarbonIcons.CLOSE, currentTab::close),
				action("menu.tab.closeothers", CarbonIcons.CLOSE, () -> {
					for (DockingTab regionTab : currentTab.getRegion().getDockTabs()) {
						if (regionTab != currentTab)
							regionTab.close();
					}
				}),
				action("menu.tab.closeall", CarbonIcons.CLOSE, () -> {
					for (DockingTab regionTab : currentTab.getRegion().getDockTabs())
						regionTab.close();
				})
		);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class item to end path with.
	 *
	 * @return Class path node.
	 */
	@Nonnull
	private static ClassPathNode buildPath(@Nonnull Workspace workspace,
										   @Nonnull WorkspaceResource resource,
										   @Nonnull ClassBundle<?> bundle,
										   @Nonnull ClassInfo info) {
		return PathNodes.classPath(workspace, resource, bundle, info);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		File item to end path with.
	 *
	 * @return File path node.
	 */
	@Nonnull
	private static FilePathNode buildPath(@Nonnull Workspace workspace,
										  @Nonnull WorkspaceResource resource,
										  @Nonnull FileBundle bundle,
										  @Nonnull FileInfo info) {
		return PathNodes.filePath(workspace, resource, bundle, info);
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
