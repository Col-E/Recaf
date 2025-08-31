package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSectionHeader;
import net.fornwall.jelf.ElfSegment;
import net.fornwall.jelf.ElfStringTable;
import net.fornwall.jelf.ElfSymbol;
import net.fornwall.jelf.ElfSymbolTableSection;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.StringUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * A pane for displaying basic information about Executable and Linkable Format files.
 *
 * @author Matt Coley
 */
@Dependent
public class ElfPane extends BorderPane implements FileNavigable, UpdatableNavigable {
	private static final Logger logger = Logging.get(ElfPane.class);
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
		try {
			ElfFile elf = ElfFile.from(path.getValue().getRawContent());

			TreeView<Object> tree = new TreeView<>();
			tree.setShowRoot(false);
			tree.setCellFactory(v -> new ElfCell());
			tree.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
				// TODO: Display content of selected model
				//  - Its generally going to be a Tableview that doesn't occupy all the screen real-estate
				//    - Maybe there's a better way to display this content then?
				//    - If not, we can largely migrate the work from https://github.com/Col-E/Recaf/pull/423/files
			});
			ElfItem root = new ElfItem();

			ElfItem elfHeader = root.child(CarbonIcons.GRID, "ELF Header");
			ElfItem programHeaders = root.child(CarbonIcons.GRID, "Program Headers");
			for (int i = 0; i < elf.e_phnum; i++) {
				// TODO: Better display name derivation based on header type (see segment toString())
				ElfSegment programHeader = elf.getProgramHeader(i);
				if (programHeader != null)
					programHeaders.child("Header %d".formatted(i));
			}

			ElfItem sectionHeaders = root.child(CarbonIcons.GRID, "Section Headers");
			for (int i = 0; i < elf.e_shnum; i++) {
				int sectionIdx = i;
				ElfSectionHeader sectionHeader = Unchecked.getOr(() -> elf.getSection(sectionIdx).header, null);
				if (sectionHeader != null) {
					String name = sectionHeader.getName();
					if (!StringUtil.isNullOrEmpty(name))
						sectionHeaders.child(name);
				}
			}

			ElfItem stringTable = root.child(CarbonIcons.GRID, "String Table");
			ElfStringTable table = Unchecked.getOr(elf::getStringTable, null);
			if (table != null) {
				for (int i = 0; i < table.numStrings; i++) {
					// TODO: Instead of being part of the tree, the table contents will be shown
					//  in the "current selection" display (see above)
					String str = table.get(i);
					if (!StringUtil.isNullOrEmpty(str))
						stringTable.child(str);
				}
			}

			ElfItem dynamicStringTable = root.child(CarbonIcons.GRID, "Dynamic String Table");
			table = Unchecked.getOr(elf::getDynamicStringTable, null);
			if (table != null) {
				for (int i = 0; i < table.numStrings; i++) {
					// TODO: Instead of being part of the tree, the table contents will be shown
					//  in the "current selection" display (see above)
					String str = table.get(i);
					if (!StringUtil.isNullOrEmpty(str))
						dynamicStringTable.child(str);
				}
			}

			ElfItem symbolTable = root.child(CarbonIcons.GRID, "Symbol Table");
			ElfSymbolTableSection symbolTableSection = Unchecked.getOr(elf::getSymbolTableSection, null);
			if (symbolTableSection != null) {
				for (ElfSymbol symbol : symbolTableSection.symbols) {
					// TODO: Instead of being part of the tree, the table contents will be shown
					//  in the "current selection" display (see above)
					String name = symbol.getName();
					if (!StringUtil.isNullOrEmpty(name))
						symbolTable.child(name);
				}
			}

			ElfItem dynamicSymbolTable = root.child(CarbonIcons.GRID, "Dynamic Symbol Table");
			ElfSymbolTableSection dynamicSymbolTableSection = Unchecked.getOr(elf::getDynamicSymbolTableSection, null);
			if (dynamicSymbolTableSection != null) {
				for (ElfSymbol symbol : dynamicSymbolTableSection.symbols) {
					// TODO: Instead of being part of the tree, the table contents will be shown
					//  in the "current selection" display (see above)
					String name = symbol.getName();
					if (!StringUtil.isNullOrEmpty(name))
						dynamicSymbolTable.child(name);
				}
			}

			tree.setRoot(root);

			setCenter(tree);
		} catch (Throwable t) {
			logger.error("Failed parsing {}", path.getValue().getName(), t);
		}
	}

	private static class ElfItem extends TreeItem<Object> {
		@Nonnull
		private ElfItem child(@Nonnull Ikon icon, @Nonnull Object value) {
			ElfItem child = new ElfItem();
			child.setValue(value);
			child.setGraphic(new FontIconView(icon));
			getChildren().add(child);
			return child;
		}

		@Nonnull
		private ElfItem child(@Nonnull Object value) {
			ElfItem child = new ElfItem();
			child.setValue(value);
			getChildren().add(child);
			return child;
		}
	}

	private static class ElfCell extends TreeCell<Object> {
		@Override
		protected void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);

			if (item == null || empty) {
				setText(null);
				setGraphic(null);
			} else if (item instanceof String itemString) {
				setText(itemString);
			}

			TreeItem<Object> treeItem = getTreeItem();
			if (treeItem != null)
				setGraphic(treeItem.getGraphic());
		}
	}
}
