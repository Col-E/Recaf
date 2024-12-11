package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.android.xml.AndroidResourceProvider;
import software.coley.android.xml.XmlDecoder;
import software.coley.recaf.info.ArscFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.android.AndroidRes;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Displays a {@link TextFileInfo} via a configured {@link Editor}.
 *
 * @author Matt Coley
 */
@Dependent
public class DecodingXmlPane extends BorderPane implements FileNavigable, UpdatableNavigable {
	protected final AtomicBoolean updateLock = new AtomicBoolean();
	protected final Editor editor;
	protected FilePathNode path;

	@Inject
	public DecodingXmlPane(@Nonnull SearchBar searchBar, @Nonnull FileTypeSyntaxAssociationService languageAssociation) {
		// Configure the editor
		editor = new Editor();
		editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.getRootLineGraphicFactory().addLineGraphicFactory(new BracketMatchGraphicFactory());
		languageAssociation.configureEditorSyntax("xml", editor);
		editor.getCodeArea().setEditable(false);
		searchBar.install(editor);

		// Layout
		setCenter(editor);
	}

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
		setDisable(true);
		setOnKeyPressed(null);
		editor.close();
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// Pass to children
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof UpdatableNavigable updatableNavigable)
				updatableNavigable.onUpdatePath(path);

		// Handle updates to the text.
		if (!updateLock.get() && path instanceof FilePathNode filePath) {
			FileInfo info = filePath.getValue();
			if (info instanceof BinaryXmlFileInfo binaryXml) {
				this.path = filePath;
				try {
					// Attempt to lookup ARSC resource for better decoding.
					ArscFileInfo arscFile = null;
					Workspace workspace = path.getValueOfType(Workspace.class);
					if (workspace != null) {
						FilePathNode arscPath = workspace.findFile(ArscFileInfo.NAME);
						if (arscPath != null) arscFile = (ArscFileInfo) arscPath.getValue();
					}

					// Decode XML and update the editor text.
					AndroidResourceProvider arscResources = arscFile == null ? null : arscFile.getResourceInfo();
					String decodedXml = XmlDecoder.decode(binaryXml.getChunkModel(), AndroidRes.getAndroidBase(), arscResources);
					FxThreadUtil.run(() -> editor.setText(decodedXml));
				} catch (Exception ex) {
					FxThreadUtil.run(() -> editor.setText("<!-- Failed to decode XML:\n" +
							"Message: " + ex.getMessage() + "\n" +
							StringUtil.traceToString(ex) + "\n--->"));
				}
			}
		}
	}
}
