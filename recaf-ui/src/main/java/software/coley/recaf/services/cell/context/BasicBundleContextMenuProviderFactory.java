package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import javafx.stage.WindowEvent;
import software.coley.recaf.info.Info;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProvider;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.contextmenu.ContextMenuBuilder;
import software.coley.recaf.ui.control.popup.ChangeClassVersionForAllPopup;
import software.coley.recaf.ui.control.popup.DecompileAllPopup;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.*;

/**
 * Basic implementation for {@link BundleContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicBundleContextMenuProviderFactory extends AbstractContextMenuProviderFactory
		implements BundleContextMenuProviderFactory {
	private final Instance<DecompileAllPopup> decompileAllPaneProvider;
	private final Instance<ChangeClassVersionForAllPopup> changeClassVersionProvider;

	@Inject
	public BasicBundleContextMenuProviderFactory(@Nonnull TextProviderService textService,
	                                             @Nonnull IconProviderService iconService,
	                                             @Nonnull Instance<DecompileAllPopup> decompileAllPaneProvider,
	                                             @Nonnull Instance<ChangeClassVersionForAllPopup> changeClassVersionProvider,
	                                             @Nonnull Actions actions) {
		super(textService, iconService, actions);
		this.decompileAllPaneProvider = decompileAllPaneProvider;
		this.changeClassVersionProvider = changeClassVersionProvider;
	}

	@Nonnull
	@Override
	public ContextMenuProvider getBundleContextMenuProvider(@Nonnull ContextSource source,
	                                                        @Nonnull Workspace workspace,
	                                                        @Nonnull WorkspaceResource resource,
	                                                        @Nonnull Bundle<? extends Info> bundle) {
		return () -> {
			TextProvider nameProvider = textService.getBundleTextProvider(workspace, resource, bundle);
			IconProvider iconProvider = iconService.getBundleIconProvider(workspace, resource, bundle);
			ContextMenu menu = new ContextMenu();
			addHeader(menu, nameProvider.makeText(), iconProvider.makeIcon());
			var builder = new ContextMenuBuilder(menu, source).forBundle(workspace, resource, bundle);
			var edit = builder.submenu("menu.edit", EDIT);
			edit.item("misc.clear", TRASH_CAN, bundle::clear);

			if (bundle instanceof JvmClassBundle jvmBundle) {
				builder.item("menu.export.classes", DOCUMENT_EXPORT, () -> actions.exportClasses(workspace, resource, jvmBundle));
				builder.item("menu.file.decompileall", DOCUMENT_EXPORT, () -> {
					DecompileAllPopup popup = decompileAllPaneProvider.get();
					popup.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> decompileAllPaneProvider.destroy(popup));
					popup.setTargetBundle(jvmBundle);
					popup.show();
				});
				builder.item("menu.edit.changeversion", ARROWS_VERTICAL, () -> {
					ChangeClassVersionForAllPopup popup = changeClassVersionProvider.get();
					popup.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> changeClassVersionProvider.destroy(popup));
					popup.setTargetBundle(jvmBundle);
					popup.show();
				});
			} else if (bundle instanceof FileBundle fileBundle) {
				builder.item("menu.export.files", DOCUMENT_EXPORT, () -> actions.exportFiles(workspace, resource, fileBundle));
			}
			return menu;
		};
	}
}
