package me.coley.recaf.ui;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.*;
import me.coley.recaf.ui.control.PannableImageView;
import me.coley.recaf.ui.control.TextView;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.hex.HexFileView;
import me.coley.recaf.ui.pane.elf.ElfExplorerPane;
import me.coley.recaf.ui.pane.pe.PEExplorerPane;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Display for a {@link FileView}.
 *
 * @author Matt Coley
 */
public class FileView extends BorderPane implements FileRepresentation, Cleanable, Undoable {
	private FileRepresentation mainView;
	private FileViewMode mode = Configs.editor().defaultFileView;
	private FileInfo info;

	/**
	 * @param info
	 * 		Initial state of the file to display.
	 */
	public FileView(FileInfo info) {
		this.info = info;
		mainView = createViewForFile(info);
		setCenter(mainView.getNodeRepresentation());
		onUpdate(info);
		Configs.keybinds().installEditorKeys(this);
	}

	@Override
	public void cleanup() {
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		this.info = newValue;
		if (mainView != null) {
			mainView.onUpdate(newValue);
		}
	}

	@Override
	public void undo() {
		if (supportsEditing()) {
			Resource primary = getPrimary();
			String name = info.getName();
			if (primary != null && primary.getFiles().hasHistory(name))
				primary.getFiles().decrementHistory(name);
		}
	}

	@Override
	public SaveResult save() {
		if (supportsEditing()) {
			return mainView.save();
		}
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		// Only allow editing if the wrapped info belongs to the primary resource
		Resource primary = getPrimary();
		if (primary == null || !primary.getFiles().containsKey(info.getName()))
			return false;
		// Then delegate to main view
		if (mainView != null)
			return mainView.supportsEditing();
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return info;
	}

	/**
	 * Set the view mode and trigger a refresh.
	 *
	 * @param mode
	 * 		New view mode.
	 */
	public void setMode(FileViewMode mode) {
		// Skip if the same
		if (this.mode == mode)
			return;
		this.mode = mode;
		// Cleanup old view if present
		if (mainView instanceof Cleanable) {
			((Cleanable) mainView).cleanup();
		}
		// Trigger refresh
		mainView = createViewForFile(info);
		setCenter(mainView.getNodeRepresentation());
		onUpdate(info);
	}

	private FileRepresentation createViewForFile(FileInfo info) {
		byte[] content = info.getValue();
		// Create representation based on file header
		if (mode == FileViewMode.AUTO) {
			if (ByteHeaderUtil.matchAny(content, ByteHeaderUtil.IMAGE_HEADERS)) {
				PannableImageView imageView = new PannableImageView();
				return new BasicFileRepresentation(imageView, newInfo -> imageView.setImage(newInfo.getValue()));
			}
			else if (ByteHeaderUtil.match(content, ByteHeaderUtil.PE)) {
				PEExplorerPane peExplorerPane = new PEExplorerPane();
				return new BasicFileRepresentation(peExplorerPane, peExplorerPane::onUpdate);
			}
			else if (ByteHeaderUtil.match(content, ByteHeaderUtil.ELF)) {
				ElfExplorerPane elfExplorerPane = new ElfExplorerPane();
				return new BasicFileRepresentation(elfExplorerPane, elfExplorerPane::onUpdate);
			}
			else if (StringUtil.isText(info.getValue())) {
				return new TextView(Languages.get(info.getExtension()), null);
			} else {
				return new HexFileView();
			}
		} else if (mode == FileViewMode.TEXT) {
			return new TextView(Languages.get(info.getExtension()), null);
		} else {
			return new HexFileView();
		}
	}

	private static Resource getPrimary() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace != null)
			return workspace.getResources().getPrimary();
		return null;
	}
}
