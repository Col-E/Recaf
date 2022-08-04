package me.coley.recaf.ui.pane;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.BinaryXMLParser;
import jadx.core.xmlgen.ResTableParser;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Displays content of binary XML files found in APK files.
 *
 * @author Matt Coley
 */
public class BinaryXmlPane extends BorderPane implements FileRepresentation, FontSizeChangeable {
	private static final Logger logger = Logging.get(BinaryXmlPane.class);
	/**
	 * We're keeping a static instance since when we load new resource data into it, the old data is wiped.
	 * Reduces some pointless allocations.
	 */
	private static final RootNode JADX_ROOT = new RootNode(new JadxArgs());
	private final SyntaxArea xmlArea = new SyntaxArea(Languages.XML, null);
	private FileInfo fileInfo;

	/**
	 * Create and set up the panel.
	 */
	public BinaryXmlPane() {
		setCenter(new VirtualizedScrollPane<>(xmlArea));
		// Search support
		SearchBar.install(this, xmlArea);
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return fileInfo;
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		// TODO: Add ability to recompile XML back to binary format
		//        - Assumes the 'resources.arsc' content is available for linking references
		//        - Or assumes there are no references, just literals
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		fileInfo = newValue;
		try {
			Workspace workspace = RecafUI.getController().getWorkspace();
			if (workspace == null)
				return;
			StringBuilder prefix = new StringBuilder("<!--\n" +
					"  Converted from binary XML format.\n" +
					"  This file is READ-ONLY\n"
					);
			// The XML parser needs to pull data stored in sub-files contained within the 'arsc' file.
			FileInfo resourcesFile = workspace.getResources().getFile("resources.arsc");
			if (resourcesFile != null) {
				// TODO: We only need to read this file once per workspace
				ResTableParser tableParser = new ResTableParser(JADX_ROOT);
				tableParser.decode(new ByteArrayInputStream(resourcesFile.getValue()));
				JADX_ROOT.processResources(tableParser.getResStorage());
			} else {
				prefix.append("  This file is missing references to contents stored in 'resources'arsc'.\n");
			}
			prefix.append("-->\n");
			// Parse and set text
			BinaryXMLParser parser = new BinaryXMLParser(JADX_ROOT);
			ICodeInfo result = parser.parse(new ByteArrayInputStream(fileInfo.getValue()));
			FxThreadUtil.run(() -> xmlArea.setText(prefix + result.getCodeStr()));
		} catch (IOException ex) {
			logger.error("Failed to decode XML resource: {}", fileInfo.getName(), ex);
		}
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		xmlArea.bindFontSize(property);
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		xmlArea.applyEventsForFontSizeChange(consumer);
	}
}
