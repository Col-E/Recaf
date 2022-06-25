package me.coley.recaf.ui.pane;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.Controller;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.ScrollSnapshot;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.PannableImageView;
import me.coley.recaf.ui.control.TextView;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.hex.HexView;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A simple diff viewer to show a comparison of the changes made to classes/files.
 *
 * @author Matt Coley
 */
public class DiffViewPane extends BorderPane implements ControllerListener,
		ResourceClassListener, ResourceDexClassListener, ResourceFileListener {
	private static final Logger logger = Logging.get(DiffViewPane.class);
	private static final long TIMEOUT_MS = 10_000;
	private final ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("DiffView");
	private final ObservableList<ItemInfo> items = FXCollections.observableArrayList();
	private Workspace workspace;

	/**
	 * Create the diff viewer, and add listeners to the controller so that we can live update which
	 * classes and files have history to display.
	 *
	 * @param controller
	 * 		Controller to add listener to.
	 */
	public DiffViewPane(Controller controller) {
		controller.addListener(this);
		// Primary display of selected item
		BorderPane content = new BorderPane();
		// List of modified items (classes/files/etc)
		ListView<ItemInfo> listView = new ListView<>();
		listView.setMinWidth(220);
		listView.setItems(items);
		listView.setCellFactory(c -> new ListCell<>() {
			@Override
			protected void updateItem(ItemInfo item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					setOnMousePressed(null);
				} else {
					CellFactory.update(CellOriginType.SEARCH_RESULTS, this, workspace.getResources().getPrimary(), item);
					// Override with full name
					setText(EscapeUtil.escape(item.getName()));
					setOnMousePressed(e -> content.setCenter(createDiffDisplay(item)));
				}
			}
		});
		// Layout
		SplitPane split = new SplitPane();
		split.getItems().addAll(listView, content);
		setCenter(split);
		SplitPane.setResizableWithParent(listView, false);
	}

	private Node createDiffDisplay(ItemInfo item) {
		Resource primary = workspace.getResources().getPrimary();
		SplitPane split = new SplitPane();
		if (item instanceof ClassInfo) {
			Stack<ClassInfo> history = primary.getClasses().getHistory(item.getName());
			if (history == null)
				return new BoundLabel(Lang.getBinding("modifications.none"));
			ClassInfo current = (ClassInfo) item;
			ClassInfo initial = history.firstElement();
			// Create a countdown for the two classes to decompile
			CountDownLatch latch = new CountDownLatch(2);
			DiffableDecompilePane currentDecompile = new DiffableDecompilePane(latch);
			DiffableDecompilePane initialDecompile = new DiffableDecompilePane(latch);
			currentDecompile.bindScrollTo(initialDecompile);
			currentDecompile.onUpdate(current);
			initialDecompile.onUpdate(initial);
			// Add to the UI
			split.getItems().addAll(initialDecompile, currentDecompile);
			// When the class versions both get decompiled, run a basic text diff and highlight modified lines.
			service.execute(() -> {
				try {
					if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
						logger.error("Timed out waiting for decompilation");
						return;
					}
					highlightDiff(initialDecompile, currentDecompile);
				} catch (InterruptedException e) {
					logger.error("Decompilation task interrupted");
				}
			});
			return split;
		} else if (item instanceof DexClassInfo) {
			// TODO: Android diff
			//  - Probably just go with 'SmaliAssemblerPane' similar to how the java logic above is done
			//    and do the same standard text-diff highlighting.
			return new Label("TODO: Android diff");
		} else if (item instanceof FileInfo) {
			Stack<FileInfo> history = primary.getFiles().getHistory(item.getName());
			FileInfo current = (FileInfo) item;
			FileInfo initial = history.firstElement();
			byte[] currentRaw = current.getValue();
			byte[] initialRaw = initial.getValue();
			if (ByteHeaderUtil.matchAny(currentRaw, ByteHeaderUtil.IMAGE_HEADERS)) {
				PannableImageView initialImage = new PannableImageView();
				PannableImageView currentImage = new PannableImageView();
				initialImage.setImage(initialRaw);
				currentImage.setImage(currentRaw);
				split.getItems().addAll(initialImage, currentImage);
			} else if (StringUtil.isText(currentRaw)) {
				Language language = Languages.get(current.getExtension());
				DiffableTextView initialText = new DiffableTextView(language);
				DiffableTextView currentText = new DiffableTextView(language);
				initialText.onUpdate(initial);
				currentText.onUpdate(current);
				initialText.getTextArea().setEditable(false);
				currentText.getTextArea().setEditable(false);
				split.getItems().addAll(initialText, currentText);
				highlightDiff(initialText, currentText);
			} else {
				// TODO: How do we want to go around highlighting hex diffs?
				//  - Representing 'inserted' and 'removed' can't use padding like text-view can
				//  - Can just do simple direct highlights without padding probably
				//     - Need access to each cell/offset to do this
				HexView initialHex = new HexView();
				HexView currentHex = new HexView();
				initialHex.onUpdate(initialRaw);
				currentHex.onUpdate(currentRaw);
				// TODO: Should update HexView to support 'setEditable(boolean)' which disables HexRow editable cells.
				split.getItems().addAll(initialHex, currentHex);
			}
			return split;
		} else {
			throw new IllegalStateException("Unknown info type: " + ((item == null) ? "null" : item.getClass()));
		}
	}

	private void highlightDiff(Diffable initial, Diffable current) {
		Patch<String> diff = DiffUtils.diff(
				Arrays.asList(initial.getText().split("\n")),
				Arrays.asList(current.getText().split("\n"))
		);
		if (diff.getDeltas().isEmpty()) {
			return;
		}
		FxThreadUtil.delayedRun(100, () -> {
			int deletions = 0;
			int insertions = 0;
			int currentOffset = 0;
			int initialOffset = 0;
			for (Delta<String> delta : diff.getDeltas()) {
				Chunk<String> original = delta.getOriginal();
				Chunk<String> revised = delta.getRevised();
				switch (delta.getType()) {
					case CHANGE:
						initial.markDiffChunk(original, "change", insertions);
						current.markDiffChunk(revised, "change", deletions);
						break;
					case DELETE:
						initial.markDiffChunk(original, "deletion", insertions);
						int deleteStartLine = revised.getPosition() + currentOffset;
						int deletedCount = original.size();
						int chunkDiffRO = revised.getPosition() - original.getPosition();
						current.getCodeArea().insertText(deleteStartLine, 0, "\n".repeat(deletedCount));
						current.markDiffChunk(original, "deletion", chunkDiffRO + currentOffset);
						deletions += deletedCount;
						currentOffset += deletedCount;
						break;
					case INSERT:
						current.markDiffChunk(revised, "insertion", deletions);
						int insertStartLine = original.getPosition() + initialOffset;
						int insertedCount = revised.size();
						int chunkDiffOR = original.getPosition() - revised.getPosition();
						initial.getCodeArea().insertText(insertStartLine, 0, "\n".repeat(insertedCount));
						initial.markDiffChunk(revised, "insertion", chunkDiffOR + initialOffset);
						insertions += insertedCount;
						initialOffset += insertedCount;
						break;
				}
			}
		});
		initial.getCodeArea().showParagraphAtCenter(0);
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		workspace = newWorkspace;
		// Clear old items
		items.clear();
		// Add listeners to receive new items
		if (newWorkspace != null) {
			Resource resource = newWorkspace.getResources().getPrimary();
			resource.addClassListener(this);
			resource.addDexListener(this);
			resource.addFileListener(this);
		}
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		items.add(newValue);
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		items.add(newValue);
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		items.add(newValue);
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
	}

	/**
	 * Simple interface to make text-diff logic backed by {@link CodeArea} easy to implement.
	 */
	private interface Diffable {
		default void markDiffChunk(Chunk<String> chunk, String style, int offset) {
			CodeArea area = getCodeArea();
			int pos = chunk.getPosition() + offset;
			int len = chunk.size();
			Collection<String> styles = Collections.singleton(style);
			for (int i = 0; i < len; i++)
				area.setParagraphStyle(pos + i, styles);
		}

		default String getText() {
			return getCodeArea().getText();
		}

		CodeArea getCodeArea();
	}

	/**
	 * An extension of the text-view for line difference highlighting.
	 */
	private static class DiffableTextView extends TextView implements Diffable {
		/**
		 * @param language
		 * 		Language to use for syntax highlighting.
		 */
		public DiffableTextView(Language language) {
			super(language, null);
		}

		@Override
		public CodeArea getCodeArea() {
			return getTextArea();
		}

		@Override
		public SaveResult save() {
			return SaveResult.IGNORED;
		}

		@Override
		public boolean supportsEditing() {
			return false;
		}
	}

	/**
	 * An extension of the Java decompile pane for line difference highlighting.
	 */
	private static class DiffableDecompilePane extends DecompilePane implements Diffable {
		private final CountDownLatch latch;
		private String code;

		public DiffableDecompilePane(CountDownLatch latch) {
			this.latch = latch;
			getJavaArea().setEditable(false);
			// This is a lazy fix.
			// Prevent users from switching decompiler and thus invalidating the text.
			setBottom(null);
		}

		/**
		 * @return Decompiled code.
		 */
		public String getCode() {
			return code;
		}

		public void bindScrollTo(DiffableDecompilePane other) {
			getScroll().estimatedScrollYProperty().bindBidirectional(other.getScroll().estimatedScrollYProperty());
		}

		@Override
		public CodeArea getCodeArea() {
			return getJavaArea();
		}

		@Override
		public SaveResult save() {
			return SaveResult.IGNORED;
		}

		@Override
		public boolean supportsEditing() {
			return false;
		}

		@Override
		public ScrollSnapshot makeScrollSnapshot() {
			return () -> {
				// no-op
			};
		}

		@Override
		protected void onDecompileCompletion(String code) {
			this.code = code;
			latch.countDown();
		}
	}
}
