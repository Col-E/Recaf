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
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;

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
		split.setDividerPositions(0.5);
		setCenter(split);
		// TODO: Why does the divider position not get acknowledged with this? Its too small.
		//  - Can probably fix with setPreferredWidth on list
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
			DiffDecompilePane currentDecompile = new DiffDecompilePane(latch);
			DiffDecompilePane initialDecompile = new DiffDecompilePane(latch);
			currentDecompile.bindScrollTo(initialDecompile);
			currentDecompile.onUpdate(current);
			initialDecompile.onUpdate(initial);
			// Add to the UI
			split.getItems().addAll(initialDecompile, currentDecompile);
			// When the class versions both get decompiled, run a basic text diff and highlight modified lines.
			service.execute(() -> {
				try {
					if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
						// TODO: timed out, add logging or something to notify user
						return;
					}
					Patch<String> diff = DiffUtils.diff(
							Arrays.asList(initialDecompile.getCode().split("\n")),
							Arrays.asList(currentDecompile.getCode().split("\n"))
					);
					if (diff.getDeltas().isEmpty()) {
						return;
					}
					FxThreadUtil.delayedRun(100, () -> {
						int deleteOffset = 0;
						int insertOffset = 0;
						for (Delta<String> delta : diff.getDeltas()) {
							Chunk<String> original = delta.getOriginal();
							Chunk<String> revised = delta.getRevised();
							switch (delta.getType()) {
								case CHANGE:
									initialDecompile.markDiffChunk(original, "change", insertOffset);
									currentDecompile.markDiffChunk(revised, "change", deleteOffset);
									break;
								case DELETE:
									initialDecompile.markDiffChunk(original, "deletion", insertOffset);
									int deleteStartLine = original.getPosition() + insertOffset;
									int deletedCount = original.size();
									currentDecompile.getJavaArea().insertText(deleteStartLine, 0, "\n".repeat(deletedCount));
									currentDecompile.markDiffChunk(original, "deletion", insertOffset);
									deleteOffset += deletedCount;
									break;
								case INSERT:
									currentDecompile.markDiffChunk(revised, "insertion", deleteOffset);
									int insertStartLine = revised.getPosition() + deleteOffset;
									int insertedCount = revised.size();
									initialDecompile.getJavaArea().insertText(insertStartLine, 0, "\n".repeat(insertedCount));
									initialDecompile.markDiffChunk(revised, "insertion", deleteOffset);
									insertOffset -= insertedCount;
									break;
							}
						}
					});
				} catch (InterruptedException e) {
					// TODO: error handling
					e.printStackTrace();
				}
			});
			return split;
		} else if (item instanceof DexClassInfo) {
			// TODO: Android diff
			return new Label("TODO: Android diff");
		} else if (item instanceof FileInfo) {
			// TODO: Same as the java diff, but for plain text.
			//  - will have to do something custom for hex diffs
			return new Label("TODO: File diff");
		} else {
			throw new IllegalStateException("Unknown info type: " + ((item == null) ? "null" : item.getClass()));
		}
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
	 * An extension of the Java decompile pane for line difference highlighting.
	 */
	private static class DiffDecompilePane extends DecompilePane {
		private final CountDownLatch latch;
		private String code;

		public DiffDecompilePane(CountDownLatch latch) {
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

		public void bindScrollTo(DiffDecompilePane other) {
			getScroll().estimatedScrollYProperty().bindBidirectional(other.getScroll().estimatedScrollYProperty());
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

		public void markDiffChunk(Chunk<String> chunk, String style, int offset) {
			JavaArea area = getJavaArea();
			int pos = chunk.getPosition() + offset;
			int len = chunk.size();
			Collection<String> styles = Collections.singleton(style);
			for (int i = 0; i < len; i++)
				area.setParagraphStyle(pos + i, styles);
		}
	}
}
