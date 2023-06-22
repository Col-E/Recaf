package software.coley.recaf.ui.pane.editing.text;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.problem.ProblemGraphicFactory;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.bundle.FileBundle;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Displays a {@link TextFileInfo} via a configured {@link Editor}.
 *
 * @author Matt Coley
 */
@Dependent
public class TextPane extends BorderPane implements FileNavigable, UpdatableNavigable {
	protected final AtomicBoolean updateLock = new AtomicBoolean();
	protected final Editor editor;
	protected FilePathNode path;

	@Inject
	public TextPane(@Nonnull KeybindingConfig keys,
					@Nonnull SearchBar searchBar) {
		// Configure the editor
		editor = new Editor();
		editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory(),
				new ProblemGraphicFactory()
		);
		searchBar.install(editor);

		// Setup keybindings
		setOnKeyPressed(e -> {
			if (keys.getSave().match(e))
				ThreadUtil.run(this::save);
		});

		// Layout
		setCenter(editor);
	}

	/**
	 * Called when {@link KeybindingConfig#getSave()} is pressed.
	 * <br>
	 * Updates the {@link FileInfo} in the containing {@link FileBundle}.
	 */
	private void save() {
		// Pull data from path.
		FileInfo info = path.getValue();
		FileBundle bundle = path.getValueOfType(FileBundle.class);
		if (bundle == null)
			throw new IllegalStateException("Bundle missing from file path node");

		// Create updated info model.
		FileInfo newInfo = info.toFileBuilder()
				.withRawContent(editor.getText().getBytes(StandardCharsets.UTF_8))
				.build();

		// Update the file in the bundle.
		updateLock.set(true);
		bundle.put(newInfo);
		updateLock.set(false);
		FxThreadUtil.run(() -> Animations.animateSuccess(this, 1000));
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
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// Pass to children
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof UpdatableNavigable updatableNavigable)
				updatableNavigable.onUpdatePath(path);

		// Handle updates to the text.
		if (!updateLock.get() && path instanceof FilePathNode filePath) {
			this.path = filePath;
			FileInfo info = filePath.getValue();
			if (info.isTextFile()) {
				TextFileInfo textInfo = info.asTextFile();

				// TODO: Select correct stylesheet & syntax highlighter impl
				//    editor.getStylesheets().add("/syntax/java.css");
				//    editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJavaLanguage()));
				//     - Remember to allow users to customize mapping of file extensions to different types

				FxThreadUtil.run(() -> {
					editor.setText(textInfo.getText());
				});
			}
		}
	}
}
