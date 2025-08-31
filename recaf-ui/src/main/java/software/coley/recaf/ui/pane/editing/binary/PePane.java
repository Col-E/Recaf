package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import me.martinez.pe.ExportEntry;
import me.martinez.pe.ImportEntry;
import me.martinez.pe.LibraryImports;
import me.martinez.pe.PeImage;
import me.martinez.pe.headers.ImageDataDirectory;
import me.martinez.pe.headers.ImageDosHeader;
import me.martinez.pe.headers.ImageFileHeader;
import me.martinez.pe.headers.ImageNtHeaders;
import me.martinez.pe.headers.ImageOptionalHeader;
import me.martinez.pe.headers.ImageSectionHeader;
import me.martinez.pe.io.CadesBufferStream;
import me.martinez.pe.util.ParseError;
import me.martinez.pe.util.ParseResult;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A pane for displaying basic information about Windows Portable Executables.
 *
 * @author Matt Coley
 */
@Dependent
public class PePane extends BorderPane implements FileNavigable, UpdatableNavigable {
	private static final Logger logger = Logging.get(PePane.class);
	private FilePathNode path;

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		// no-op
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			refresh();
		}
	}

	private void refresh() {
		ParseResult<PeImage> result = PeImage.read(new CadesBufferStream(path.getValue().getRawContent()));
		if (result.isOk()) {
			PeImage pe = result.getOk();

			TreeView<Object> tree = new TreeView<>();
			tree.setShowRoot(false);
			tree.setCellFactory(v -> new PeCell());
			tree.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
				// TODO: Display content of selected model
				//  - Its generally going to be a Tableview that doesn't occupy all the screen real-estate
				//    - Maybe there's a better way to display this content then?
				//    - If not, we can largely migrate the work from https://github.com/Col-E/Recaf/pull/422/files
			});
			PeItem root = new PeItem();
			if (pe.dosHeader != null) {
				root.child(CarbonIcons.GRID, pe.dosHeader);
			}

			ImageNtHeaders ntHeaders = pe.ntHeaders;
			if (ntHeaders != null) {
				PeItem ntItem = root.child(CarbonIcons.GRID, ntHeaders);
				ImageFileHeader fileHeader = ntHeaders.fileHeader;
				if (fileHeader != null) {
					ntItem.child(CarbonIcons.BLOCKCHAIN, fileHeader);
				}

				ImageOptionalHeader optionalHeader = ntHeaders.optionalHeader;
				if (optionalHeader != null) {
					PeItem optionalItem = ntItem.child(CarbonIcons.BLOCKCHAIN, optionalHeader);
					ImageDataDirectory[] dataDirectory = optionalHeader.dataDirectory;
					if (dataDirectory != null) {
						for (ImageDataDirectory imageDataDirectory : dataDirectory) {
							optionalItem.child(CarbonIcons.CODE_REFERENCE, imageDataDirectory);
						}
					}
				}
			}

			PeItem sectionHeaders = root.child(CarbonIcons.GRID, "Section Headers");
			for (ParseResult<ImageSectionHeader> headerResult : pe.sectionHeaders) {
				if (headerResult.isOk()) {
					ImageSectionHeader header = headerResult.getOk();
					sectionHeaders.child(header);
				}
			}

			PeItem imports = root.child(CarbonIcons.DOCUMENT_IMPORT, "Imports");
			if (pe.imports.isOk()) {
				for (LibraryImports libraryImports : pe.imports.getOk()) {
					PeItem library = imports.child(CarbonIcons.BOOK, libraryImports);
					for (ImportEntry entry : libraryImports.entries) {
						library.child(CarbonIcons.NOTEBOOK_REFERENCE, entry);
					}
				}
			}

			PeItem exports = root.child(CarbonIcons.DOCUMENT_EXPORT, "Exports");
			if (pe.exports.isOk()) {
				for (ExportEntry entry : pe.exports.getOk().entries) {
					exports.child(CarbonIcons.NOTEBOOK_REFERENCE, entry);
				}
			}

			tree.setRoot(root);
			setCenter(tree);
		} else {
			ParseError err = result.getErr();
			logger.error("Failed parsing {} - {}", path.getValue().getName(), err.getReason(), err.getException());
		}
	}

	private static class PeItem extends TreeItem<Object> {
		@Nonnull
		private PeItem child(@Nonnull Ikon icon, @Nonnull Object value) {
			PeItem child = new PeItem();
			child.setValue(value);
			child.setGraphic(new FontIconView(icon));
			getChildren().add(child);
			return child;
		}

		@Nonnull
		private PeItem child(@Nonnull Object value) {
			PeItem child = new PeItem();
			child.setValue(value);
			getChildren().add(child);
			return child;
		}
	}

	private static class PeCell extends TreeCell<Object> {
		@Override
		protected void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);

			if (item == null || empty) {
				setText(null);
				setGraphic(null);
			} else if (item instanceof String itemString) {
				setText(itemString);
			} else if (item instanceof ImageDosHeader dosHeader) {
				setText("DOS Header");
			} else if (item instanceof ImageNtHeaders ntHeaders) {
				setText("NT Headers");
			} else if (item instanceof ImageFileHeader fileHeader) {
				setText("File Header");
			} else if (item instanceof ImageOptionalHeader optionalHeader) {
				setText("Optional Header");
			} else if (item instanceof ImageDataDirectory dataDirectory) {
				setText("Data Directory");
			} else if (item instanceof ImageSectionHeader sectionHeader) {
				setText("Section Header: " + sectionHeader.getName());
			} else if (item instanceof LibraryImports libraryImports) {
				setText("Library Imports: " + libraryImports.name);
			} else if (item instanceof ImportEntry importEntry) {
				setText(Objects.requireNonNullElse(importEntry.name, String.valueOf(importEntry.ordinal)));
			} else if (item instanceof ExportEntry exportEntry) {
				setText(Objects.requireNonNullElse(exportEntry.name, String.valueOf(exportEntry.ordinal)));
			}

			TreeItem<Object> treeItem = getTreeItem();
			if (treeItem != null)
				setGraphic(treeItem.getGraphic());
		}
	}
}
