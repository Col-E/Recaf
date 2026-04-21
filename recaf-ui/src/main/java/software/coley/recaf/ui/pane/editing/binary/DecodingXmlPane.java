package software.coley.recaf.ui.pane.editing.binary;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import software.coley.android.xml.AndroidResourceProvider;
import software.coley.android.xml.XmlDecoder;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ArscFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.info.properties.builtin.BinaryXmlDecodedProperty;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
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
	private static final Logger logger = Logging.get(DecodingXmlPane.class);
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

	/**
	 * @param text
	 * 		Text to set in the editor.
	 */
	public void setText(@Nonnull String text) {
		FxThreadUtil.run(() -> editor.setText("<!-----------------------\n\n" + text + "\n\n------------------------>"));
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
					// Check if we cached the decoded XML already. If so, use it.
					String cachedXml = BinaryXmlDecodedProperty.get(binaryXml);
					if (cachedXml != null) {
						FxThreadUtil.run(() -> editor.setText(cachedXml));
						return;
					}

					// If not cached, decode the XML and cache it for next time.
					Workspace workspace = path.getValueOfType(Workspace.class);
					String decodedXml = decode(workspace, binaryXml.getChunkModel());
					BinaryXmlDecodedProperty.set(binaryXml, decodedXml);
				} catch (Exception ex) {
					FxThreadUtil.run(() -> editor.setText("<!-- Failed to decode XML:\n" +
							"Message: " + ex.getMessage() + "\n" +
							StringUtil.traceToString(ex) + "\n--->"));
				}
			}
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to lookup ARSC resources from, if available. This is used to improve the quality of the decoded XML.
	 * @param chunkModel
	 * 		Binary XML chunk model from the {@code resources.arsc} file.
	 *
	 * @return Decoded XML string.
	 */
	@Nonnull
	private String decode(@Nullable Workspace workspace, @Nonnull BinaryResourceFile chunkModel) {
		// Attempt to lookup ARSC resource for better decoding.
		ArscFileInfo arscFile = null;
		if (workspace != null) {
			// This assumes the ARSC file is always 'resources.arsc' in the root.
			// This should generally always be the case, unless a developer is doing something funky.
			FilePathNode arscPath = workspace.findFile(ArscFileInfo.NAME);
			if (arscPath != null) arscFile = (ArscFileInfo) arscPath.getValue();
			else {
				logger.warn("Failed to find 'resources.arsc' in workspace, falling back to Android base model");
			}
		}

		// Decode XML and update the editor text.
		AndroidResourceProvider arscResources = arscFile == null ? null : arscFile.getResourceInfo();
		String decodedXml = XmlDecoder.decode(chunkModel, AndroidRes.getAndroidBase(), arscResources);
		FxThreadUtil.run(() -> editor.setText(decodedXml));
		return decodedXml;
	}
}
