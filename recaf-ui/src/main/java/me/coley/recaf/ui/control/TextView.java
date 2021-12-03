package me.coley.recaf.ui.control;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.nio.charset.StandardCharsets;

/**
 * Basic scrollable text display with syntax highlighting support.
 *
 * @author Matt Coley
 */
public class TextView extends BorderPane implements FileRepresentation, Cleanable {
	private final SyntaxArea area;
	private boolean ignoreNextDecompile;
	private FileInfo info;

	/**
	 * @param language
	 * 		Language to use for syntax highlighting.
	 * @param problemTracking
	 * 		Problem tracker.
	 */
	public TextView(Language language, ProblemTracking problemTracking) {
		this.area = new SyntaxArea(language, problemTracking);
		this.area.paragraphGraphicFactoryProperty();
		setCenter(new VirtualizedScrollPane<>(area));
		SearchBar.install(this, area);
	}

	@Override
	public void onUpdate(FileInfo info) {
		this.info = info;
		if (ignoreNextDecompile) {
			ignoreNextDecompile = false;
			return;
		}
		area.setText(new String(info.getValue()));
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return info;
	}

	@Override
	public SaveResult save() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource primary = workspace.getResources().getPrimary();
		// Update in primary resource
		ignoreNextDecompile = true;
		FileInfo newInfo = new FileInfo(info.getName(), area.getText().getBytes(StandardCharsets.UTF_8));
		primary.getFiles().put(newInfo);
		return SaveResult.SUCCESS;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void cleanup() {
		area.cleanup();
	}
}
